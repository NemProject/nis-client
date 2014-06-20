package org.nem.nis;

import javax.annotation.PostConstruct;

import java.util.logging.Logger;

import org.nem.core.crypto.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.DeserializationContext;
import org.nem.nis.dao.*;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.BlockMapper;
import org.nem.core.model.*;
import org.nem.core.time.*;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.springframework.beans.factory.annotation.Autowired;

public class NisMain {
	private static final Logger LOGGER = Logger.getLogger(NisMain.class.getName());

	public static final TimeProvider TIME_PROVIDER = new SystemTimeProvider();

	private Block nemesisBlock;
	private Hash nemesisBlockHash;

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private AccountAnalyzer accountAnalyzer;

	@Autowired
	private BlockChain blockChain;

	@Autowired
	private NisPeerNetworkHost networkHost;

	@Autowired
	private BlockChainLastBlockLayer blockChainLastBlockLayer;

	private void analyzeBlocks() {
		Long curBlockId;
		LOGGER.info("starting analysis...");

		org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHash(this.nemesisBlockHash);
		LOGGER.info(String.format("hex: %s", dbBlock.getGenerationHash()));
		if (!dbBlock.getGenerationHash().equals(Hash.fromHexString("c5d54f3ed495daec32b4cbba7a44555f9ba83ea068e5f1923e9edb774d207cd8"))) {
			LOGGER.severe("couldn't find nemesis block, you're probably using developer's build, drop the db and rerun");
			System.exit(-1);
		}

		Block parentBlock = null;

		// This is tricky:
		// we pass AA to observer and AutoCachedAA to toModel
		// it creates accounts for us inside AA but without height, so inside observer we'll set height
		final AccountsHeightObserver observer = new AccountsHeightObserver(this.accountAnalyzer);
		do {
			final Block block = BlockMapper.toModel(dbBlock, this.accountAnalyzer.asAutoCache());

			if (null != parentBlock) {
				this.blockChain.updateScore(parentBlock, block);
			}

			block.subscribe(observer);
			block.execute();
			block.unsubscribe(observer);

			// fully vest all transactions coming out of the nemesis block
			if (null == parentBlock) {
				for (final Account account : this.accountAnalyzer) {
					if (NemesisBlock.ADDRESS.equals(account.getAddress())) {
						continue;
					}

					account.getWeightedBalances().convertToFullyVested();
				}
			}

			parentBlock = block;

			curBlockId = dbBlock.getNextBlockId();
			if (null == curBlockId) {
				this.blockChainLastBlockLayer.analyzeLastBlock(dbBlock);
				break;
			}

			dbBlock = this.blockDao.findById(curBlockId);
			if (dbBlock == null && this.blockChainLastBlockLayer.getLastDbBlock() == null) {
				LOGGER.severe("inconsistent db state, you're probably using developer's build, drop the db and rerun");
				System.exit(-1);
			}
		} while (dbBlock != null);

		LOGGER.info("Known accounts: " + this.accountAnalyzer.size());
	}

	@PostConstruct
	private void init() {
		LOGGER.warning("context ================== current: " + TIME_PROVIDER.getCurrentTime());

		// load the nemesis block information
		this.nemesisBlock = this.loadNemesisBlock();
		this.nemesisBlockHash = HashUtils.calculateHash(this.nemesisBlock);
		this.logNemesisInformation();

		this.populateDb();

		this.analyzeBlocks();
	}

	private NemesisBlock loadNemesisBlock() {
		// set up the nemesis block amounts
		final Account nemesisAccount = this.accountAnalyzer.addAccountToCache(NemesisBlock.ADDRESS);
		nemesisAccount.incrementBalance(NemesisBlock.AMOUNT);
		nemesisAccount.getWeightedBalances().addReceive(BlockHeight.ONE, NemesisBlock.AMOUNT);
		nemesisAccount.setHeight(BlockHeight.ONE);

		// load the nemesis block
		return NemesisBlock.fromResource(new DeserializationContext(this.accountAnalyzer.asAutoCache()));
	}

	private void logNemesisInformation() {
		LOGGER.info("nemesis block hash:" + this.nemesisBlockHash);

		final KeyPair nemesisKeyPair = this.nemesisBlock.getSigner().getKeyPair();
		final Address nemesisAddress = this.nemesisBlock.getSigner().getAddress();
		LOGGER.info("nemesis account private key          : " + nemesisKeyPair.getPrivateKey());
		LOGGER.info("nemesis account            public key: " + nemesisKeyPair.getPublicKey());
		LOGGER.info("nemesis account compressed public key: " + nemesisAddress.getEncoded());
		LOGGER.info("nemesis account generation hash      : " + this.nemesisBlock.getGenerationHash());
	}

	private void populateDb() {
		if (0 != this.blockDao.count())
			return;

		this.saveBlock(this.nemesisBlock);
	}

	private org.nem.nis.dbmodel.Block saveBlock(final Block block) {
		org.nem.nis.dbmodel.Block dbBlock;

		dbBlock = this.blockDao.findByHash(this.nemesisBlockHash);
		if (null != dbBlock) {
			return dbBlock;
		}

		dbBlock = BlockMapper.toDbModel(block, new AccountDaoLookupAdapter(this.accountDao));
		this.blockDao.save(dbBlock);
		return dbBlock;
	}
}
