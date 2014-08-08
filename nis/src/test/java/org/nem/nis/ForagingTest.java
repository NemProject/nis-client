package org.nem.nis;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.*;
import org.nem.nis.poi.PoiImportanceGenerator;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.test.MockForaging;

import java.lang.reflect.*;
import java.util.*;

import static org.mockito.Mockito.mock;

public class ForagingTest {
	private static org.nem.core.model.Account RECIPIENT1 = new org.nem.core.model.Account(Utils.generateRandomAddress());
	private static org.nem.core.model.Account RECIPIENT2 = new org.nem.core.model.Account(Utils.generateRandomAddress());

	static void setFinalStatic(Field field, Object newValue) throws Exception {
		field.setAccessible(true);

		// remove final modifier from field
		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

		field.set(null, newValue);
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		// TODO: is there some way to use mockito for this?
		setFinalStatic(NisMain.class.getField("TIME_PROVIDER"), new SystemTimeProvider());
	}

	// region add/remove unlocked account
	
	@Test
	public void canUnlockKnownForagingEligibleAccount() {
		// Arrange:
		final Foraging foraging = createMockForaging();
		final Account account = copyAccountWithBalance(RECIPIENT1, Amount.fromNem(10000));
		
		// Act:
		final UnlockResult result = foraging.addUnlockedAccount(account);
		
		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(UnlockResult.SUCCESS));
		assertAccountIsUnlocked(foraging, account);
	}

	@Test
	public void cannotUnlockForagingIneligibleAccount() {
		// Arrange:
		final Foraging foraging = createMockForaging();
		final Account account = copyAccountWithBalance(RECIPIENT1, Amount.fromNem(100));

		// Act:
		final UnlockResult result = foraging.addUnlockedAccount(account);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(UnlockResult.FAILURE_FORAGING_INELIGIBLE));
		assertAccountIsLocked(foraging, account);
	}

	@Test
	public void cannotUnlockUnknownAccount() {
		// Arrange:
		final Foraging foraging = createMockForaging();
		final Account account = copyAccountWithBalance(RECIPIENT2, Amount.fromNem(10000));
		
		// Act:
		final UnlockResult result = foraging.addUnlockedAccount(account);
		
		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(UnlockResult.FAILURE_UNKNOWN_ACCOUNT));
		assertAccountIsLocked(foraging, account);
	}

	private static Account copyAccountWithBalance(final Account original, final Amount amount) {
		final Account copy = original.copy();
		copy.getWeightedBalances().addFullyVested(BlockHeight.ONE, amount);
		copy.incrementBalance(amount);
		return copy;
	}

	@Test
	public void canLockUnlockedAccount() {
		// Arrange:
		final Foraging foraging = createMockForaging();
		final Account account = copyAccountWithBalance(RECIPIENT1, Amount.fromNem(10000));
		
		// Act:
		foraging.addUnlockedAccount(account);
		
		// Assert:
		assertAccountIsUnlocked(foraging, account);

		// Act:
		foraging.removeUnlockedAccount(account);
		
		// Assert:
		assertAccountIsLocked(foraging, account);
	}

	private static void assertAccountIsLocked(final Foraging foraging, final Account account) {
		Assert.assertThat(foraging.isAccountUnlocked(account), IsEqual.equalTo(false));
		Assert.assertThat(foraging.isAccountUnlocked(account.getAddress()), IsEqual.equalTo(false));
	}

	private static void assertAccountIsUnlocked(final Foraging foraging, final Account account) {
		Assert.assertThat(foraging.isAccountUnlocked(account), IsEqual.equalTo(true));
		Assert.assertThat(foraging.isAccountUnlocked(account.getAddress()), IsEqual.equalTo(true));
	}

	// endregion
	
	@Test
	public void processTransactionsSavesTransactions() throws InterruptedException {
		// Arrange:
		final Transaction tx = dummyTransaction(RECIPIENT1, 12345);
		final Foraging foraging = createMockForaging();
		tx.sign();

		// Act:
		foraging.processTransaction(tx);

		// Assert:
		Assert.assertThat(foraging.getNumUnconfirmedTransactions(), IsEqual.equalTo(1));
	}

	@Test
	public void processTransactionsDoesNotSaveDuplicates() throws InterruptedException {
		// Arrange:
		final Transaction tx = dummyTransaction(RECIPIENT1, 12345);
		final Foraging foraging = createMockForaging();
		tx.sign();

		// Act:
		final ValidationResult result1 = foraging.processTransaction(tx);
		final ValidationResult result2 = foraging.processTransaction(tx);

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(result2, IsEqual.equalTo(ValidationResult.NEUTRAL));
		Assert.assertThat(foraging.getNumUnconfirmedTransactions(), IsEqual.equalTo(1));
	}

	@Test
	public void processTransactionsDoesNotSaveDoubleSpendTransaction() throws InterruptedException {
		// Arrange (category boost trust attack, stop foraging attack):
		final TimeInstant now = (new SystemTimeProvider()).getCurrentTime();
		final Account signer = createAccountWithBalance(1000);
		final Foraging foraging = createMockForaging();
		Transaction tx = new TransferTransaction(now, signer, RECIPIENT1, Amount.fromNem(800), null);
		tx.setFee(Amount.fromNem(1));
		tx.setDeadline(now.addMinutes(100));
		tx.sign();

		// Assert:
		Assert.assertThat(tx.checkValidity(), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Act:
		foraging.processTransaction(tx);
		tx = new TransferTransaction(now.addSeconds(5), signer, RECIPIENT1, Amount.fromNem(800), null);
		tx.setFee(Amount.fromNem(1));
		tx.setDeadline(now.addMinutes(100));
		tx.sign();
		foraging.processTransaction(tx);

		// Assert:
		Assert.assertThat(foraging.getNumUnconfirmedTransactions(), IsEqual.equalTo(1));
	}

	@Test
	public void canProcessTransaction() {
		// Arrange:
		final Account recipient = Utils.generateRandomAccount();
		final Foraging foraging = createMockForaging();

		// Act:
		Transaction transaction = dummyTransaction(recipient, 123);
		transaction.sign();
		final ValidationResult result = foraging.processTransaction(transaction);

		// Assert:
		Assert.assertThat(transaction.verify(), IsEqual.equalTo(true));
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void cannotProcessSameTransaction() {
		// Arrange:
		final Account signer = createAccountWithBalance(150);
		final Account recipient = Utils.generateRandomAccount();
		final Foraging foraging = createMockForaging();
		final TimeInstant now = (new SystemTimeProvider()).getCurrentTime();

		// Act:
		TransferTransaction transaction = createSignedTransactionWithTime(signer, recipient, Amount.fromNem(5), now.addSeconds(2));

		final ValidationResult result1 = foraging.processTransaction(transaction);
		final ValidationResult result2 = foraging.processTransaction(transaction);

		// Assert:
		Assert.assertThat(transaction.verify(), IsEqual.equalTo(true));
		Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(result2, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	@Test
	public void transactionsForNewBlockHappenedBeforeBlock() {
		// Arrange:
		final Account signer = createAccountWithBalance(400);
		final Account recipient = Utils.generateRandomAccount();
		final Foraging foraging = createMockForaging();
		final TimeInstant now = (new SystemTimeProvider()).getCurrentTime();

		// Act:
		TransferTransaction transaction1 = createSignedTransactionWithTime(signer, recipient, Amount.fromNem(5), now);
		TransferTransaction transaction2 = createSignedTransactionWithTime(signer, recipient, Amount.fromNem(5), now.addSeconds(20));

		final ValidationResult result1 = foraging.processTransaction(transaction1);
		final ValidationResult result2 = foraging.processTransaction(transaction2);

		List<Transaction> transactionsList = foraging.getUnconfirmedTransactionsForNewBlock(now.addSeconds(10));

		// Assert
		Assert.assertThat(transaction1.verify(), IsEqual.equalTo(true));
		Assert.assertThat(transaction2.verify(), IsEqual.equalTo(true));
		Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(result2, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(transactionsList.size(), IsEqual.equalTo(1));
	}

	@Test
	public void transactionsForNewBlockAreSortedByFee() {
		// Arrange:
		final Account signer = createAccountWithBalance(400);
		final Account recipient = Utils.generateRandomAccount();
		final Foraging foraging = createMockForaging();
		final TimeInstant now = (new SystemTimeProvider()).getCurrentTime();

		// Act:
		Transaction transaction1 = createSignedTransactionWithTime(signer, recipient, Amount.fromNem(5), now.addSeconds(2));
		Transaction transaction2 = createSignedTransactionWithTime(signer, recipient, Amount.fromNem(10), now.addSeconds(2));

		final Hash transactionHash1 = HashUtils.calculateHash(transaction1);
		final Hash transactionHash2 = HashUtils.calculateHash(transaction2);

		final ValidationResult result1 = foraging.processTransaction(transaction1);
		final ValidationResult result2 = foraging.processTransaction(transaction2);

		List<Transaction> transactionsList = foraging.getUnconfirmedTransactionsForNewBlock(now.addSeconds(20));

		// Assert
		// this indicates wrong amounts or fees
		Assert.assertThat(transactionHash1, IsNot.not(IsEqual.equalTo(transactionHash2)));

		Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(result2, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(transactionsList.size(), IsEqual.equalTo(2));
		// higher fee goes first
		Assert.assertThat(transactionsList.get(0), IsEqual.equalTo(transaction2));
		Assert.assertThat(transactionsList.get(1), IsEqual.equalTo(transaction1));
	}

	@Test
	public void transactionsForNewBlockAreSortedByTime() {
		// Arrange:
		final Account signer = createAccountWithBalance(400);
		final Account recipient = Utils.generateRandomAccount();
		final Foraging foraging = createMockForaging();
		final TimeInstant now = (new SystemTimeProvider()).getCurrentTime();

		// Act:
		Transaction transaction1 = createSignedTransactionWithTime(signer, recipient, Amount.fromNem(5), now.addSeconds(2));
		Transaction transaction2 = createSignedTransactionWithTime(signer, recipient, Amount.fromNem(5), now.addSeconds(-2));

		final ValidationResult result1 = foraging.processTransaction(transaction1);
		final ValidationResult result2 = foraging.processTransaction(transaction2);

		List<Transaction> transactionsList = foraging.getUnconfirmedTransactionsForNewBlock(now.addSeconds(20));

		// Assert
		Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(result2, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(transactionsList.size(), IsEqual.equalTo(2));
		// earlier transaction goes first
		Assert.assertThat(transactionsList.get(0), IsEqual.equalTo(transaction2));
		Assert.assertThat(transactionsList.get(1), IsEqual.equalTo(transaction1));
	}

	@Test
	public void transactionsCanBeFilteredForHarvester() {
		// Arrange:
		final Account harvester = createAccountWithBalance(200);
		final Account signer = createAccountWithBalance(400);
		final Account recipient = Utils.generateRandomAccount();
		final Foraging foraging = createMockForaging();
		final TimeInstant now = (new SystemTimeProvider()).getCurrentTime();

		// Act:
		Transaction transaction1 = createSignedTransactionWithTime(signer, recipient, Amount.fromNem(5), now.addSeconds(2));
		Transaction transaction2 = createSignedTransactionWithTime(signer, recipient, Amount.fromNem(5), now.addSeconds(-2));
		Transaction transaction3 = createSignedTransactionWithTime(harvester, recipient, Amount.fromNem(5), now.addSeconds(5));
		final ValidationResult result1 = foraging.processTransaction(transaction1);
		final ValidationResult result2 = foraging.processTransaction(transaction2);
		final ValidationResult result3 = foraging.processTransaction(transaction3);
		List<Transaction> transactionsList = foraging.getUnconfirmedTransactionsForNewBlock(now.addSeconds(20));
		List<Transaction> filteredTransactionsList = foraging.filterTransactionsForHarvester(transactionsList, harvester);
		List<Transaction> filteredTransactionsList2 = foraging.filterTransactionsForHarvester(transactionsList, Utils.generateRandomAccount());

		// Assert
		Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(result2, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(result3, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(transactionsList.size(), IsEqual.equalTo(3));
		Assert.assertThat(filteredTransactionsList.size(), IsEqual.equalTo(2));
		Assert.assertThat(filteredTransactionsList2.size(), IsEqual.equalTo(3));
	}
	
	private TransferTransaction createSignedTransactionWithTime(Account signer, Account recipient, Amount fee, TimeInstant now) {
		TransferTransaction transaction1 = new TransferTransaction(now, signer, recipient, Amount.fromNem(123), null);
		transaction1.setDeadline(now.addHours(1));
		transaction1.setFee(fee);
		transaction1.sign();
		return transaction1;
	}

	@Test
	public void canSignBlock() {
		// Arrange:
		final BlockChainLastBlockLayer lastBlockLayer = mock(BlockChainLastBlockLayer.class);
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(Mockito.mock(PoiImportanceGenerator.class));
		final MockForaging foraging = new MockForaging(accountAnalyzer, lastBlockLayer);
		final Account account = Utils.generateRandomAccount();
		accountAnalyzer.addAccountToCache(account.getAddress());
		final Account accountWithoutSecret = accountAnalyzer.findByAddress(account.getAddress());
		accountWithoutSecret.incrementBalance(Amount.fromNem(100));

		final Account signer = createAccountWithBalance(100);
		final TimeInstant parentTime = new TimeInstant(0);
		final Block parent = new Block(
				signer,
				Hash.ZERO,
				Hash.ZERO,
				parentTime,
				BlockHeight.ONE);
		parent.sign();

		// Act:
		final Block block = foraging.createSignedBlock(
				new TimeInstant(10),
				new LinkedList<>(),
				parent,
				account,
				BlockDifficulty.INITIAL_DIFFICULTY);

		// Assert:
		Assert.assertThat(accountWithoutSecret.getKeyPair().getPrivateKey(), IsNull.nullValue());
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(block.getSigner(), IsEqual.equalTo(accountWithoutSecret));
	}

	private Transaction dummyTransaction(org.nem.core.model.Account recipient, long amount) {
		Transaction transaction = new TransferTransaction(
				(new SystemTimeProvider()).getCurrentTime(),
				createAccountWithBalance(amount*3),
				recipient,
				new Amount(amount),
				null);
		transaction.setDeadline(transaction.getTimeStamp().addHours(1));
		return transaction;
	}

	private static Account createAccountWithBalance(long balance) {
		final Account account = Utils.generateRandomAccount();
		account.incrementBalance(Amount.fromNem(balance));
		return account;
	}

	private Foraging createMockForaging() {
		final BlockChainLastBlockLayer lastBlockLayer = mock(BlockChainLastBlockLayer.class);
		Mockito.when(lastBlockLayer.getLastBlockHeight()).thenReturn(9L);

		final AccountAnalyzer accountAnalyzer = createAccountAnalyzer();
		accountAnalyzer.addAccountToCache(RECIPIENT1.getAddress());
		return new MockForaging(accountAnalyzer, lastBlockLayer);
	}

	private static AccountAnalyzer createAccountAnalyzer() {
		return new AccountAnalyzer(Mockito.mock(PoiImportanceGenerator.class));
	}
}
