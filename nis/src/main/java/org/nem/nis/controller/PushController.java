package org.nem.nis.controller;

import org.nem.core.model.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.transactions.TransactionFactory;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.BlockChain;
import org.nem.nis.Foraging;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.controller.annotations.P2PApi;
import org.nem.peer.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.InvalidParameterException;
import java.util.logging.Logger;

/**
 * This controller will handle data propagation:
 * * /push/transaction - for what is now model.Transaction
 * * /push/block - for model.Block
 * <p/>
 * It would probably fit better in TransferController, but this is
 * part of p2p API, so I think it should be kept separated.
 * (I think it might pay off in future, if we'd like to add restrictions to client APIs)
 */

@RestController
public class PushController {
	private static final Logger LOGGER = Logger.getLogger(PushController.class.getName());

	private final AccountAnalyzer accountAnalyzer;
	private final Foraging foraging;
	private final BlockChain blockChain;
	private final NisPeerNetworkHost host;

	@Autowired(required = true)
	PushController(
			final AccountAnalyzer accountAnalyzer,
			final Foraging foraging,
			final BlockChain blockChain,
			final NisPeerNetworkHost host) {
		this.accountAnalyzer = accountAnalyzer;
		this.foraging = foraging;
		this.blockChain = blockChain;
		this.host = host;
	}

	@RequestMapping(value = "/push/transaction", method = RequestMethod.POST)
	@P2PApi
	public void pushTransaction(final Deserializer deserializer) {
		final Transaction transaction = TransactionFactory.VERIFIABLE.deserialize(deserializer);

		LOGGER.info("   signer: " + transaction.getSigner().getKeyPair().getPublicKey());
		LOGGER.info("   verify: " + Boolean.toString(transaction.verify()));

		// transaction timestamp is checked inside processTransaction
		if (!transaction.isValid() || !transaction.verify())
			throw new InvalidParameterException("transfer must be valid and verifiable");

		final PeerNetwork network = this.host.getNetwork();

		// add to unconfirmed transactions
		if (this.foraging.processTransaction(transaction))
			network.broadcast(NodeApiId.REST_PUSH_TRANSACTION, transaction);
	}

	@RequestMapping(value = "/push/block", method = RequestMethod.POST)
	@P2PApi
	public void pushBlock(final Deserializer deserializer) {
		final Block block = BlockFactory.VERIFIABLE.deserialize(deserializer);

		// TODO: refactor logging
		LOGGER.info("   signer: " + block.getSigner().getKeyPair().getPublicKey());
		LOGGER.info("   verify: " + Boolean.toString(block.verify()));

		if (!block.verify())
			throw new InvalidParameterException("block must be verifiable");

		// PeerNetworkHost peerNetworkHost = PeerNetworkHost.getDefaultHost();

		// validate block, add to chain
		this.blockChain.processBlock(block);

		// TODO: propagate block
		//peerNetworkHost.getNetwork().broadcast(NodeApiId.REST_PUSH_BLOCK, block);
	}
}
