package org.nem.nis.dao;

import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Transfer;
import org.nem.nis.mappers.*;
import org.nem.nis.test.MockAccountDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TransferDaoTest {
	@Autowired
	TransferDao transferDao;

	@Autowired
	BlockDao blockDao;

	@Test
	public void savingTransferSavesAccounts() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = prepareMapping(sender, recipient);
		final TransferTransaction transferTransaction = prepareTransferTransaction(sender, recipient, 10, 123);
		final Transfer entity = TransferMapper.toDbModel(transferTransaction, 0, accountDaoLookup);

		// Act
		transferDao.save(entity);

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getSender().getId(), notNullValue());
		Assert.assertThat(entity.getRecipient().getId(), notNullValue());
	}

	@Test
	public void canReadSavedData() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = prepareMapping(sender, recipient);
		final TransferTransaction transferTransaction = prepareTransferTransaction(sender, recipient, 10, 0);
		final Transfer dbTransfer = TransferMapper.toDbModel(transferTransaction, 12345, accountDaoLookup);

		// Act
		transferDao.save(dbTransfer);
		final Transfer entity = transferDao.findByHash(HashUtils.calculateHash(transferTransaction).getRaw());

		// Assert:
		Assert.assertThat(entity, notNullValue());
		Assert.assertThat(entity.getId(), equalTo(dbTransfer.getId()));
		Assert.assertThat(entity.getSender().getPublicKey(), equalTo(sender.getKeyPair().getPublicKey()));
		Assert.assertThat(entity.getRecipient().getPublicKey(), equalTo(recipient.getKeyPair().getPublicKey()));
		Assert.assertThat(entity.getRecipient().getPublicKey(), equalTo(recipient.getKeyPair().getPublicKey()));
		Assert.assertThat(entity.getAmount(), equalTo(transferTransaction.getAmount().getNumMicroNem()));
		Assert.assertThat(entity.getBlkIndex(), equalTo(12345));
		Assert.assertThat(entity.getSenderProof(), equalTo(transferTransaction.getSignature().getBytes()));
	}

	@Test
	public void countReturnsProperValue() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = prepareMapping(sender, recipient);
		final TransferTransaction transferTransaction = prepareTransferTransaction(sender, recipient, 10, 123);
		final Transfer dbTransfer1 = TransferMapper.toDbModel(transferTransaction, 12345, accountDaoLookup);
		final Transfer dbTransfer2 = TransferMapper.toDbModel(transferTransaction, 12345, accountDaoLookup);
		final Transfer dbTransfer3 = TransferMapper.toDbModel(transferTransaction, 12345, accountDaoLookup);
		final Long initialCount = transferDao.count();

		// Act
		transferDao.save(dbTransfer1);
		final Long count1 = transferDao.count();
		transferDao.save(dbTransfer2);
		final Long count2 = transferDao.count();
		transferDao.save(dbTransfer3);
		final Long count3 = transferDao.count();

		// Assert:
		Assert.assertThat(count1, equalTo(initialCount + 1));
		Assert.assertThat(count2, equalTo(initialCount + 2));
		Assert.assertThat(count3, equalTo(initialCount + 3));
	}

	@Test
	public void getTransactionsForAccountUsingHashRespectsHash() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		addMapping(mockAccountDao, sender);
		final Block dummyBlock = new Block(sender, Hash.ZERO, Hash.ZERO, new TimeInstant(123), BlockHeight.ONE);

		for (int i = 0; i<30; i++) {
			final Account recipient = Utils.generateRandomAccount();
			addMapping(mockAccountDao, recipient);
			final TransferTransaction transferTransaction = prepareTransferTransaction(sender, recipient, 10, 123);

			// need to wrap it in block, cause getTransactionsForAccount returns also "owning" block's height
			dummyBlock.addTransaction(transferTransaction);
		}
		dummyBlock.sign();
		final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);

		// Act
		this.blockDao.save(dbBlock);

		// Act
		final Collection<Object[]> entities1 = this.transferDao.getTransactionsForAccountUsingHash(sender, null, ReadOnlyTransferDao.TransferType.ALL, 25);
		final Collection<Object[]> entities2 = this.transferDao.getTransactionsForAccountUsingHash(sender, dbBlock.getBlockTransfers().get(24).getTransferHash(), ReadOnlyTransferDao.TransferType.ALL, 25);
		final Collection<Object[]> entities3 = this.transferDao.getTransactionsForAccountUsingHash(sender, dbBlock.getBlockTransfers().get(29).getTransferHash(), ReadOnlyTransferDao.TransferType.ALL, 25);

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(25));
		Assert.assertThat(entities2.size(), equalTo(5));
		Assert.assertThat(entities3.size(), equalTo(0));
	}

	@Test(expected = MissingResourceException.class)
	public void getTransactionsForAccountUsingHashThrowsWhenHashNotFound() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		addMapping(mockAccountDao, sender);
		final Block dummyBlock = new Block(sender, Hash.ZERO, Hash.ZERO, new TimeInstant(123), BlockHeight.ONE);
		dummyBlock.sign();
		final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);

		// Act
		this.blockDao.save(dbBlock);

		// Act
		this.transferDao.getTransactionsForAccountUsingHash(sender, new Hash(new byte[]{6,66}), ReadOnlyTransferDao.TransferType.ALL, 25);
	}

	private Account prepareIncomingOutgoingData() {
		final Account testedAccount = Utils.generateRandomAccount();
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		addMapping(mockAccountDao, testedAccount);
		final Block dummyBlock = new Block(testedAccount, Hash.ZERO, Hash.ZERO, new TimeInstant(123), BlockHeight.ONE);


		for (int i = 0; i<100; i++) {
			final Account otherAccount = Utils.generateRandomAccount();
			addMapping(mockAccountDao, otherAccount);
			if (i % 2 == 0) {
				final TransferTransaction transferTransaction = prepareTransferTransaction(testedAccount, otherAccount, 10, 500+i);
				dummyBlock.addTransaction(transferTransaction);
			} else {
				final TransferTransaction transferTransaction = prepareTransferTransaction(otherAccount, testedAccount, 10, 500+i);
				dummyBlock.addTransaction(transferTransaction);
			}
		}
		dummyBlock.sign();
		final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);

		// Act
		this.blockDao.save(dbBlock);
		return testedAccount;
	}

	@Test
	public void getTransactionsForAccountUsingHashStrategyIncoming() {
		// Arrange:
		final List<Integer> incomingTimestamps = IntStream.range(0,50)
				.map(o -> 500 + 2*(49-o) + 1)
				.boxed()
				.collect(Collectors.toList());

		final Account testedAccount = prepareIncomingOutgoingData();

		// Act
		final List<Transfer> incomingEntities1 = this.transferDao.getTransactionsForAccountUsingHash(testedAccount, null, ReadOnlyTransferDao.TransferType.INCOMING, 25).stream()
				.map(obj -> (Transfer)obj[0])
				.collect(Collectors.toList());

		final Collection<Integer> incomingTimeStamps1 = incomingEntities1.stream()
				.map(obj -> obj.getTimestamp())
				.collect(Collectors.toList());

		final List<Transfer> incomingEntities2 = this.transferDao.getTransactionsForAccountUsingHash(testedAccount, incomingEntities1.get(24).getTransferHash(), ReadOnlyTransferDao.TransferType.INCOMING, 25).stream()
				.map(obj -> (Transfer)obj[0])
				.collect(Collectors.toList());

		final Collection<Integer> incomingTimeStamps2 = incomingEntities2.stream()
				.map(obj -> obj.getTimestamp())
				.collect(Collectors.toList());

		final List<Transfer> incomingEntities3 = this.transferDao.getTransactionsForAccountUsingHash(testedAccount, incomingEntities2.get(24).getTransferHash(), ReadOnlyTransferDao.TransferType.INCOMING, 25).stream()
				.map(obj -> (Transfer)obj[0])
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(incomingTimeStamps1.size(), equalTo(25));
		Assert.assertThat(incomingTimeStamps1, equalTo(incomingTimestamps.subList(0, 25)));
		Assert.assertThat(incomingTimeStamps2.size(), equalTo(25));
		Assert.assertThat(incomingTimeStamps2, equalTo(incomingTimestamps.subList(25, 50)));
		Assert.assertThat(incomingEntities3.size(), equalTo(0));
	}

	@Test
	public void getTransactionsForAccountUsingHashStrategyOutgoing() {
		// Arrange:
		final List<Integer> outgoingTimestamps = IntStream.range(0,50)
				.map(o -> 500 + 2*(49-o))
				.boxed()
				.collect(Collectors.toList());

		final Account testedAccount = prepareIncomingOutgoingData();

		// Act
		final List<Transfer> outgoingEntities1 = this.transferDao.getTransactionsForAccountUsingHash(testedAccount, null, ReadOnlyTransferDao.TransferType.OUTGOING, 25).stream()
				.map(obj -> (Transfer)obj[0])
				.collect(Collectors.toList());

		final Collection<Integer> outgoingTimeStamps1 = outgoingEntities1.stream()
				.map(obj -> obj.getTimestamp())
				.collect(Collectors.toList());

		final List<Transfer> outgoingEntities2 = this.transferDao.getTransactionsForAccountUsingHash(testedAccount, outgoingEntities1.get(24).getTransferHash(), ReadOnlyTransferDao.TransferType.OUTGOING, 25).stream()
				.map(obj -> (Transfer)obj[0])
				.collect(Collectors.toList());

		final Collection<Integer> outgoingTimeStamps2 = outgoingEntities2.stream()
				.map(obj -> obj.getTimestamp())
				.collect(Collectors.toList());

		final List<Transfer> outgoingEntities3 = this.transferDao.getTransactionsForAccountUsingHash(testedAccount, outgoingEntities2.get(24).getTransferHash(), ReadOnlyTransferDao.TransferType.OUTGOING, 25).stream()
				.map(obj -> (Transfer)obj[0])
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(outgoingTimeStamps1.size(), equalTo(25));
		Assert.assertThat(outgoingTimeStamps1, equalTo(outgoingTimestamps.subList(0, 25)));
		Assert.assertThat(outgoingTimeStamps2.size(), equalTo(25));
		Assert.assertThat(outgoingTimeStamps2, equalTo(outgoingTimestamps.subList(25, 50)));
		Assert.assertThat(outgoingEntities3.size(), equalTo(0));
	}


	@Test
	public void getTransactionsForAccountUsingHashStrategyAll() {
		// Arrange:
		final List<Integer> outgoingTimestamps = IntStream.range(0,100)
				.map(o -> 500 + (99-o))
				.boxed()
				.collect(Collectors.toList());

		final Account testedAccount = prepareIncomingOutgoingData();

		// Act
		final List<Transfer> allEntities1 = this.transferDao.getTransactionsForAccountUsingHash(testedAccount, null, ReadOnlyTransferDao.TransferType.ALL, 25).stream()
				.map(obj -> (Transfer)obj[0])
				.collect(Collectors.toList());

		final Collection<Integer> allTimeStamps1 = allEntities1.stream()
				.map(obj -> obj.getTimestamp())
				.collect(Collectors.toList());

		final List<Integer> allTimeStamps2 = this.transferDao.getTransactionsForAccountUsingHash(testedAccount, allEntities1.get(24).getTransferHash(), ReadOnlyTransferDao.TransferType.ALL, 25).stream()
				.map(obj -> ((Transfer)obj[0]).getTimestamp())
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(allTimeStamps1.size(), equalTo(25));
		Assert.assertThat(allTimeStamps1, equalTo(outgoingTimestamps.subList(0, 25)));
		Assert.assertThat(allTimeStamps2.size(), equalTo(25));
		Assert.assertThat(allTimeStamps2, equalTo(outgoingTimestamps.subList(25, 50)));
	}

	private void createTestBlocks(long[] heights, int[] blockTimestamp, int[][] txTimestamps, Account sender) {
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);

		addMapping(mockAccountDao, sender);

		for (int i = 0; i < heights.length; ++i) {
			long height = heights[i];
			int blockTs = blockTimestamp[i];

			final Block dummyBlock = new Block(sender, Hash.ZERO, Hash.ZERO, new TimeInstant(blockTs), new BlockHeight(height));

			for (int txTimestamp : txTimestamps[i]) {
				final Account recipient = Utils.generateRandomAccount();
				addMapping(mockAccountDao, recipient);
				final TransferTransaction transferTransaction = prepareTransferTransaction(sender, recipient, 10, txTimestamp);

				// need to wrap it in block, cause getTransactionsForAccount returns also "owning" block's height
				dummyBlock.addTransaction(transferTransaction);
			}
			dummyBlock.sign();
			final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);

			// Act
			this.blockDao.save(dbBlock);
		}
	}

	@Test
	public void getTransactionsForAccountUsingHashReturnsSortedResults() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();

		final List<Long> originalHeights = Arrays.asList(4l,4l,4l,3l,3l,2l,2l,1l,1l);
		final List<Integer> originalTimestamps = Arrays.asList(1900, 1700, 1700, 1800, 1600, 1500, 1300, 1400, 1200);
		long heights[] = { 3, 4, 1, 2 };
		int blockTimestamp[] = {1801, 1901, 1401, 1501};
		int txTimestamps[][] = { {1800, 1600}, {1900, 1700, 1700}, {1200, 1400}, {1300, 1500} };
		createTestBlocks(heights, blockTimestamp, txTimestamps, sender);

		// Act
		final Collection<Object[]> entities1 = this.transferDao.getTransactionsForAccountUsingHash(sender, null, ReadOnlyTransferDao.TransferType.ALL, 25);

		final List<Long> resultHeights = entities1.stream().map(obj -> (Long)obj[1]).collect(Collectors.toList());
		final List<Integer> resultTimestamps = entities1.stream().map(obj -> ((Transfer)obj[0]).getTimestamp()).collect(Collectors.toList());

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(9));
		Assert.assertThat(resultHeights, equalTo(originalHeights));
		Assert.assertThat(resultTimestamps, equalTo(originalTimestamps));
	}

	@Test
	public void getTransactionsForAccountRespectsTimestamp() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		addMapping(mockAccountDao, sender);
		final Block dummyBlock = new Block(sender, Hash.ZERO, Hash.ZERO, new TimeInstant(123), BlockHeight.ONE);

		for (int i = 0; i<30; i++) {
			final Account recipient = Utils.generateRandomAccount();
			addMapping(mockAccountDao, recipient);
			final TransferTransaction transferTransaction = prepareTransferTransaction(sender, recipient, 10, 123);

			// need to wrap it in block, cause getTransactionsForAccount returns also "owning" block's height
			dummyBlock.addTransaction(transferTransaction);
		}
		dummyBlock.sign();
		final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);

		// Act
		this.blockDao.save(dbBlock);

		// Act
		final Collection<Object[]> entities1 = this.transferDao.getTransactionsForAccount(sender, 123, 25);
		final Collection<Object[]> entities2 = this.transferDao.getTransactionsForAccount(sender, 122, 25);

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(25));
		Assert.assertThat(entities2.size(), equalTo(0));
	}


	@Test
	public void getTransactionsForAccountReturnsTransactionsSortedByTime() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		addMapping(mockAccountDao, sender);

		final Block dummyBlock = new Block(sender, Hash.ZERO, Hash.ZERO, new TimeInstant(123 + 30), BlockHeight.ONE);
		for (int i = 0; i<30; i++) {
			final Account recipient = Utils.generateRandomAccount();
			addMapping(mockAccountDao, recipient);
			// pseudorandom times
			final TransferTransaction transferTransaction = prepareTransferTransaction(sender, recipient, 10, 100 + (i*23 + 3)%30);

			// need to wrap it in block, cause getTransactionsForAccount returns also "owning" block's height
			dummyBlock.addTransaction(transferTransaction);
		}
		dummyBlock.sign();
		final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);

		// Act
		this.blockDao.save(dbBlock);

		// Act
		final Collection<Object[]> entities1 = this.transferDao.getTransactionsForAccount(sender, 100+30, 25);
		final Collection<Object[]> entities2 = this.transferDao.getTransactionsForAccount(sender, 99, 25);

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(25));
		Assert.assertThat(entities2.size(), equalTo(0));
		int lastTimestamp = 100+29;
		for (final Object[] entity : entities1) {
			Assert.assertThat(((Transfer)entity[0]).getTimestamp(), equalTo(lastTimestamp));
			lastTimestamp = lastTimestamp - 1;
		}
	}

	private TransferTransaction prepareTransferTransaction(final Account sender, final Account recipient, final long amount, final int i) {
		// Arrange:
		final TransferTransaction transferTransaction = new TransferTransaction(
				new TimeInstant(i),
				sender,
				recipient,
				Amount.fromNem(amount),
				null
		);
		transferTransaction.sign();
		return transferTransaction;
	}

	private void addMapping(final MockAccountDao mockAccountDao, final Account account) {
		final org.nem.nis.dbmodel.Account dbSender = new org.nem.nis.dbmodel.Account(account.getAddress().getEncoded(), account.getKeyPair().getPublicKey());
		mockAccountDao.addMapping(account, dbSender);
	}

	private AccountDaoLookup prepareMapping(Account sender, Account recipient) {
		// Arrange:
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final org.nem.nis.dbmodel.Account dbSender = new org.nem.nis.dbmodel.Account(sender.getAddress().getEncoded(), sender.getKeyPair().getPublicKey());
		final org.nem.nis.dbmodel.Account dbRecipient = new org.nem.nis.dbmodel.Account(recipient.getAddress().getEncoded(), recipient.getKeyPair().getPublicKey());
		mockAccountDao.addMapping(sender, dbSender);
		mockAccountDao.addMapping(recipient, dbRecipient);
		return new AccountDaoLookupAdapter(mockAccountDao);
	}
}
