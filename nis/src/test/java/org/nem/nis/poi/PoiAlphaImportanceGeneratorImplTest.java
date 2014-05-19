package org.nem.nis.poi;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Logger;

import org.junit.*;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.core.utils.FormatUtils;
import org.nem.nis.test.MockAccount;

/**
 * If someone can manipulate their importance so that they can often or at-will
 * be chosen to forage, then things like double-spend attacks become possible.
 * Thus the tests considered here focus on verifying that a user cannot
 * arbitrarily manipulate their importance to cause them to be chosen to forage.
 * 
 * some tests we should consider: - Sybil attack (master node creates a ton of
 * other nodes and transacts with them (and maybe some other nodes) to try to
 * boost score)</br> 
 * - infinite loop attack<br/>
 * - closed loop attack<br/>
 * - small transaction spam attack<br/>
 * -
 */
public class PoiAlphaImportanceGeneratorImplTest {
	private static final Logger LOGGER = Logger.getLogger(PoiAlphaImportanceGeneratorImplTest.class.getName());

	static private final int OUTLINK_STRATEGY_NONE = 0;
	static private final int OUTLINK_STRATEGY_RANDOM = 1;
	static private final int OUTLINK_STRATEGY_LOOP = 2;
	static private final int OUTLINK_STRATEGY_LOOP_SELF = 3;
	static private final int OUTLINK_STRATEGY_ALL_TO_ONE = 4;

	@Test
	public void threeSimpleAccounts() {
		// Arrange:
		Account a = createAccountWithBalance(100);
		Account b = createAccountWithBalance(100);
		Account c = createAccountWithBalance(100);

		final BlockHeight blockHeight = new BlockHeight(1337);

		// A sends all 100 NEM to B,
		a.addOutlink(blockHeight, new AccountLink(blockHeight, Amount.fromNem(100), b));

		List<Account> accts = Arrays.asList(a, b, c);

		// Act: calculate importances
		ColumnVector importances = getAccountImportances(blockHeight, accts);
		System.out.println(importances);

		// Assert
		//a > b > c
		Assert.assertTrue(importances.getAt(0) > importances.getAt(1)  && importances.getAt(1) > importances.getAt(2));
	}

	/**
	 * Four nodes (A, B, C, D) are owned by one person with 400 NEM who distributed the NEM 
	between the nodes and cycled the NEM around. The other three nodes are independent and have 400 NEM each.
	
	The following transactions occur (transaction fees are assumed to be 0):
	A, E, F, G all start with 400 NEM; ABCD are all controlled by actor A.
	A sends all 400 NEM to B, who sends 300 NEM to C, who sends 200 NEM to D, who sends 100 to A.
	
	E starts with 400 NEM and sends 100 to G.
	G starts with 400 NEM, gets 100 from E, and sends 100 to F.
	 */
	@Test
	public void fourNodeSimpleLoopAttack() {

		// Arrange:
		Account a = createAccountWithBalance(400);
		Account b = createAccountWithBalance(0);
		Account c = createAccountWithBalance(0);
		Account d = createAccountWithBalance(0);

		Account e = createAccountWithBalance(400);
		Account f = createAccountWithBalance(400);
		Account g = createAccountWithBalance(400);
		
		final BlockHeight blockHeight = new BlockHeight(1337);

		//TODO: we really need the infrastructure for adding coinday-weighted links and updating balances.
		// A sends all 400 NEM to B,
		addOutlink(a, b, blockHeight, 400);

		//who sends 300 NEM to C,
		addOutlink(b, c, blockHeight, 300);

		//who sends 200 NEM to D,
		addOutlink(c, d, blockHeight, 200);

		// who sends 100 to A.
		addOutlink(d, a, blockHeight, 100);

		// e sends 100 NEM to g
		addOutlink(e, g, blockHeight, 100);

		// g sends 100 NEM to f
		addOutlink(g, f, blockHeight, 100);

		List<Account> accts = Arrays.asList(a, b, c, d, e, f, g);

		// Act: calculate importances
		ColumnVector importances = getAccountImportances(blockHeight, accts);
		System.out.println(importances);

		// Assert:
		// G > E > F >> A > others
		Assert.assertTrue(importances.getAt(6) > importances.getAt(4));// g>e
		Assert.assertTrue(importances.getAt(4) > importances.getAt(5));// e>f
//		Assert.assertTrue(importances.getAt(5) > importances.getAt(0));// f>a // accts with no outlinks have 0 importance as currently designed
		Assert.assertTrue(importances.getAt(0) > importances.getAt(1));// a>b
		Assert.assertTrue(importances.getAt(0) > importances.getAt(2));// a>c
		Assert.assertTrue(importances.getAt(0) > importances.getAt(3));// a>d
	}

	private void addOutlink(final Account a, final Account b, final BlockHeight blockHeight, final long amount) {
		a.addOutlink(blockHeight, new AccountLink(blockHeight, Amount.fromNem(amount), b));
	}

	@Test
	public void twoAccountsLoopSelfVersusTwoAccountsLoop() {
		LOGGER.info("Self loop vs. normal loop");
		
		// Arrange:
		// TODO: Loops should be detected.
		List<Account> accounts = new ArrayList<>();
		accounts.addAll(createUserAccounts(1, 2, 1000, 1, 500, OUTLINK_STRATEGY_LOOP_SELF));
		accounts.addAll(createUserAccounts(1, 2, 1000, 1, 500, OUTLINK_STRATEGY_LOOP));

		// Act: calculate importances
		ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);

		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		double ratio = (importances.getAt(0) + importances.getAt(1))/(importances.getAt(2) + importances.getAt(3));
		System.out.print("Self loop vs. normal loop: User 1 importance is " + format.format(importances.getAt(0) + importances.getAt(1)));
		System.out.print(", User 2 cumulative importance is " + format.format(importances.getAt(2) + importances.getAt(3)));
		System.out.println(", ratio is " + format.format(ratio));
		System.out.println("");
		
		// Assert
		Assert.assertTrue(0.95 < ratio && ratio < 1.05);
	}
	
	@Test
	public void accountSplittingDoesNotInfluenceImportanceDistribution() {
		LOGGER.info("1 account vs. many accounts");

		// Arrange 1 vs many:
		// Splitting of one account into many small accounts should have no influence on the importance distribution.
		List<Account> accounts = new ArrayList<>();
		for (int i=2; i<10; i++) {
			accounts.clear();
			accounts.addAll(createUserAccounts(1, 1, 800, 1, 400, OUTLINK_STRATEGY_LOOP_SELF));
			accounts.addAll(createUserAccounts(1, i, 800, 1, 400, OUTLINK_STRATEGY_LOOP));
	
			// Act: calculate importances
			ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			double ratio = importances.getAt(0)/(importances.sum() - importances.getAt(0));
			System.out.print("1 vs. " + i + ": User 1 importance is " + format.format(importances.getAt(0)));
			System.out.print(", User 2 cumulative importance is " + format.format(importances.sum() - importances.getAt(0)));
			System.out.println(", ratio is " + format.format(ratio));
			
			// Assert
			Assert.assertTrue(0.9 < ratio && ratio < 1.1);
		}
		System.out.println("");
	}
	
	@Test
	public void manySmallLazyAcountsDoNotInfluenceImportanceDistribution() {
		LOGGER.info("1 account vs. 8 accounts with many lazy accounts");

		// Arrange 1 vs 8, with lazy accounts:
		// The presence of many small lazy accounts should have no influence on the importance distribution.
		List<Account> accounts = new ArrayList<>();
		for (int i=0; i<400; i=i+40) {
			accounts.clear();
			accounts.addAll(createUserAccounts(1, 1, 800, 1, 400, OUTLINK_STRATEGY_LOOP_SELF));
			accounts.addAll(createUserAccounts(1, 8, 800, 1, 400, OUTLINK_STRATEGY_LOOP));
			accounts.addAll(createUserAccounts(1, i, i*50, 0, 0, OUTLINK_STRATEGY_NONE));
	
			// Act: calculate importances
			ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			double user2Importance = 0;
			for (int j=1; j<9; j++) {
				user2Importance += importances.getAt(j);
			}
			double ratio = importances.getAt(0)/user2Importance;
			System.out.print("1 vs. 8 with " + i + " small lazy accounts: User 1 importance is " + format.format(importances.getAt(0)));
			System.out.print(", User 2 cumulative importance is " + format.format(user2Importance));
			System.out.println(", ratio is " + format.format(ratio));
			
			// Assert
			Assert.assertTrue(0.9 < ratio && ratio < 1.1);
		}
		System.out.println("");
	}
	
	@Test
	public void oneBigLazyAcountDoesNotInfluencesImportanceDistribution() {
		LOGGER.info("1 account vs. 8 accounts with 0 or 1 big lazy account");

		// Arrange 1 vs 8, with 0 or 1 big lazy account:
		// The presence of a big lazy account should have no influence on the relative importance distribution.
		List<Account> accounts = new ArrayList<>();
		for (int i=0; i<2; i++) {
			accounts.clear();
			accounts.addAll(createUserAccounts(1, 1, 800, 1, 400, OUTLINK_STRATEGY_LOOP_SELF));
			accounts.addAll(createUserAccounts(1, 8, 800, 1, 400, OUTLINK_STRATEGY_LOOP_SELF));
			accounts.addAll(createUserAccounts(1, i, i*8000, 0, 0, OUTLINK_STRATEGY_NONE));
	
			// Act: calculate importances
			ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			double user2Importance = 0;
			for (int j=1; j<9; j++) {
				user2Importance += importances.getAt(j);
			}
			double ratio = importances.getAt(0)/user2Importance;
			System.out.print("1 vs. 8 with " + i + " big lazy account: User 1 importance is " + format.format(importances.getAt(0)));
			System.out.print(", User 2 cumulative importance is " + format.format(user2Importance));
			System.out.println(", ratio is " + format.format(ratio));
			
			// Assert
			Assert.assertTrue(0.9 < ratio && ratio < 1.1);
		}
		System.out.println("");
	}
	
	@Test
	public void outlinkStrengthSplittingDoesNotInfluenceImportanceDistribution() {
		LOGGER.info("1 account with 1 outlink vs. 1 account many outlinks (same cumulative strength)");

		// Arrange: 1 vs 1, the latter distributes the strength to many outlinks:
		// Splitting one transaction into many small transactions should have no influence on the importance distribution.
		List<Account> accounts = new ArrayList<>();
		for (int i=1; i<10; i++) {
			accounts.clear();
			accounts.addAll(createUserAccounts(1, 1, 800, 1, 400, OUTLINK_STRATEGY_LOOP_SELF));
			accounts.addAll(createUserAccounts(1, 1, 800, i, 400, OUTLINK_STRATEGY_LOOP_SELF));
	
			// Act: calculate importances
			ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			double ratio = importances.getAt(0)/importances.getAt(1);
			System.out.print("1 outlink vs. " + i + " outlinks: User 1 importance is " + format.format(importances.getAt(0)));
			System.out.print(", User 2 cumulative importance is " + format.format(importances.getAt(1)));
			System.out.println(", ratio is " + format.format(ratio));
			
			// Assert
			Assert.assertTrue(0.95 < ratio && ratio < 1.05);
		}
		System.out.println("");
	}
	
	@Test
	public void outlinkStrengthDoesInfluenceImportanceDistribution() {
		LOGGER.info("High outlink strength vs. low outlink strength");
		
		// Arrange:
		// The strength of an outlink should have influence on the importance distribution (but how much?).
		List<Account> accounts = new ArrayList<>();
		accounts.addAll(createUserAccounts(1, 2, 1000, 1, 500, OUTLINK_STRATEGY_LOOP));
		accounts.addAll(createUserAccounts(1, 2, 1000, 1, 50, OUTLINK_STRATEGY_LOOP));

		// Act: calculate importances
		ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);

		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		double ratio = (importances.getAt(0) + importances.getAt(1))/(importances.getAt(2) + importances.getAt(3));
		System.out.print("High outlink strength vs. low outlink strength: User 1 importance is " + format.format(importances.getAt(0) + importances.getAt(1)));
		System.out.print(", User 2 cumulative importance is " + format.format(importances.getAt(2) + importances.getAt(3)));
		System.out.println(", ratio is " + format.format(ratio));
		System.out.println("");
		
		// Assert
		Assert.assertTrue(ratio > 1.0);
	}
	
	@Test
	public void vestedBalanceDoesInfluenceImportanceDistribution() { 
		LOGGER.info("High balance vs. low balance");
		
		// Arrange:
		// The vested balance of an account should have influence on the importance distribution.
		List<Account> accounts = new ArrayList<>();
		accounts.addAll(createUserAccounts(1, 2, 1000000, 1, 500, OUTLINK_STRATEGY_LOOP));
		accounts.addAll(createUserAccounts(1, 2, 1000, 1, 500, OUTLINK_STRATEGY_LOOP));

		// Act: calculate importances
		ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);

		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		double ratio = (importances.getAt(0) + importances.getAt(1))/(importances.getAt(2) + importances.getAt(3));
		System.out.print("High balance vs. low balance: User 1 importance is " + format.format(importances.getAt(0) + importances.getAt(1)));
		System.out.print(", User 2 cumulative importance is " + format.format(importances.getAt(2) + importances.getAt(3)));
		System.out.println(", ratio is " + format.format(ratio));
		System.out.println("");
		
		// Assert
		Assert.assertTrue(ratio > 10.0);
	}

	@Test
	public void poiIsFairerThanPOS() {
		LOGGER.info("Check that POI distributes importance differently than POS");
		//TODO: I don't know why the outlinks vectors are the same for the two groups of accounts below.
		// Arrange:
		// Accounts with smaller vested balance should be able to have more importance than accounts with high balance and low activity
		final int numAccounts = 10;
		
		List<Account> accounts = new ArrayList<>();
		accounts.addAll(createUserAccounts(1, numAccounts, 10000, 1, 500, OUTLINK_STRATEGY_RANDOM));
		accounts.addAll(createUserAccounts(1, numAccounts, 1000, 10, 500, OUTLINK_STRATEGY_RANDOM));

		// Act: calculate importances
		ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);

		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		
		double highBalanceSum = 0;
		double lowBalanceSum = 0;
		
		for (int ndx = 0; ndx < numAccounts; ndx++) {
			highBalanceSum += importances.getAt(ndx);
			lowBalanceSum  += importances.getAt(ndx + numAccounts);
		}
		
		double ratio = highBalanceSum/lowBalanceSum;
		System.out.print("High balance vs. low balance: User 1 importance is " + format.format(highBalanceSum));
		System.out.print(", User 2 cumulative importance is " + format.format(lowBalanceSum));
		System.out.println(", ratio is " + format.format(ratio));
		System.out.println("Importances: " + importances);
		System.out.println("");

		// Assert
		Assert.assertTrue(ratio > 1.0);
	}
	
	@Test
	public void accountCannotBoostPOIWithVeryLowBalance() {
		LOGGER.info("Check that an account can't just send most of their balance to another account to boost their score");
		// Arrange:
		// Accounts should not just be able to transfer all their balance to another account to boost their score
		List<Account> accounts = new ArrayList<>();
		accounts.addAll(createUserAccounts(1, 2, 10000, 2, 100, OUTLINK_STRATEGY_LOOP));
		accounts.addAll(createUserAccounts(1, 2, 10000, 2, 9900, OUTLINK_STRATEGY_LOOP));

		// Act: calculate importances
		ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);

		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		double ratio = (importances.getAt(0) + importances.getAt(1))/(importances.getAt(2) + importances.getAt(3));
		System.out.print("High balance vs. low balance: User 1 importance is " + format.format(importances.getAt(0) + importances.getAt(1)));
		System.out.print(", User 2 cumulative importance is " + format.format(importances.getAt(2) + importances.getAt(3)));
		System.out.println(", ratio is " + format.format(ratio));
		System.out.println("Importances: " + importances);
		System.out.println("");

		// Assert
		Assert.assertTrue(ratio > .95);
	}
	
	@Test
	public void pushOneAccountDoesNotInfluenceImportanceDistribution() {
		LOGGER.info("1 account with 1 outlink vs. many accounts with outlinks to one account (same cumulative strength)");

		// Arrange 1 vs many, the latter concentrate the strength to one account:
		// Colluding accounts that try to push one account with many links should have no influence on the importance distribution.
		List<Account> accounts = new ArrayList<>();
		for (int i=4; i<40; i++) {
			accounts.clear();
			accounts.addAll(createUserAccounts(1, 1, 800, 1, 400, OUTLINK_STRATEGY_LOOP_SELF));
			accounts.addAll(createUserAccounts(1, i, 800, 1, 400, OUTLINK_STRATEGY_ALL_TO_ONE));
	
			// Act: calculate importances
			ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
//			System.out.println("importances: " + importances);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			double ratio = importances.getAt(0)/(importances.sum() - importances.getAt(0));
			System.out.print("1 vs. " + i + ", outlink directed to one account: User 1 importance is " + format.format(importances.getAt(0)));
			System.out.print(", User 2 cumulative importance is " + format.format(importances.sum() - importances.getAt(0)));
			System.out.println(", ratio is " + format.format(ratio));
			
			// Assert
			Assert.assertTrue(0.9 < ratio && ratio < 1.1);
		}
		System.out.println("");
	}

	@Test
	public void poiCalculationIsPerformantEnough() {
		LOGGER.info("Testing performance of the poi calculation");

		// Arrange:
		// The poi calculation should take no more than a second even for MANY accounts (~ million)
		// TODO: why 1s?
		System.out.println("Setting up accounts.");
		int numAccounts = 5000;
		List<Account> accounts = new ArrayList<>();
		accounts.addAll(createUserAccounts(1, numAccounts, 1000, 1, 500, OUTLINK_STRATEGY_LOOP));

		// Act: calculate importances
		System.out.println("Starting poi calculation.");
		long start = System.currentTimeMillis();
		getAccountImportances(new BlockHeight(1), accounts);
		long stop = System.currentTimeMillis();
		System.out.println("Finished poi calculation.");

		System.out.println("For " + numAccounts + " accounts the poi calculation needed " + (stop-start) + "ms.");

		// Assert
		Assert.assertTrue(stop-start < 1000);
	}

	/**
	 * Test to see if the calculation time grows approximately linearly with the input.
	 */
	@Test
	public void poiCalculationHasLinearPerformance() {
		LOGGER.info("Testing linear performance of the poi calculation");

		// The poi calculation should take no more than a second even for MANY accounts (~ million)

		final BlockHeight height = new BlockHeight(1);
		long prevTimeDiff = -1;
		for (int numAccounts = 5; numAccounts < 10000; numAccounts*=10) {
			// Arrange:
			List<Account> accounts = new ArrayList<>();
			accounts.addAll(createUserAccounts(1, numAccounts, 1000, 1, 500, OUTLINK_STRATEGY_LOOP));

			// Act: calculate importances
			System.out.println("Starting poi calculation.");
			long start = System.currentTimeMillis();
			getAccountImportances(height, accounts);
			long stop = System.currentTimeMillis();
			System.out.println("Finished poi calculation.");

			System.out.println("For " + numAccounts + " accounts the poi calculation needed " + (stop-start) + "ms.");

			// Assert
			long currTimeDiff = stop-start;

			if (prevTimeDiff > 0) {
				double ratio = prevTimeDiff * 10. / currTimeDiff;
				System.out.println("Prev time: " + prevTimeDiff
						+ "\tCurr Time:" + currTimeDiff + "\tRatio: " + ratio);

				Assert.assertTrue(ratio > .9);
			}

			prevTimeDiff = currTimeDiff;
		}
	}
	
	private List<MockAccount> createUserAccounts(long blockHeight, int numAccounts, long totalVestedBalance, int numOutLinksPerAccount, long totalOutLinkStrength, int outLinkStrategy) {
		List<MockAccount> accounts = new ArrayList<>();
		
		for (int i=0; i<numAccounts; i++) {
			if (outLinkStrategy == OUTLINK_STRATEGY_ALL_TO_ONE) {
				if (i == 0) {
					accounts.add(createMockAccountWithBalance(totalVestedBalance - totalOutLinkStrength - numAccounts + 1));
				} else {
					accounts.add(createMockAccountWithBalance(1));					
				}
			} else {
				accounts.add(createMockAccountWithBalance((totalVestedBalance - totalOutLinkStrength)/numAccounts));
			}
		}
		
		SecureRandom sr = new SecureRandom();
		MockAccount otherAccount = null;
		for (int i=0; i<numAccounts; i++) {
			MockAccount account = accounts.get(i);
			account.setVestedBalanceAt(account.getBalance(), new BlockHeight(blockHeight));
			for (int j=0; j< numOutLinksPerAccount; j++) {
				switch (outLinkStrategy) {
					case OUTLINK_STRATEGY_RANDOM:
						otherAccount = accounts.get(sr.nextInt(numAccounts));
						break;
					case OUTLINK_STRATEGY_LOOP:
						otherAccount = accounts.get((i+1) % numAccounts);
						break;
					case OUTLINK_STRATEGY_LOOP_SELF:
						otherAccount = account;
						break;
					case OUTLINK_STRATEGY_ALL_TO_ONE:
						otherAccount = accounts.get(0);
						break;
				}
				long outlinkStrength = (account.getBalance().getNumNem() * totalOutLinkStrength)/((totalVestedBalance - totalOutLinkStrength) * numOutLinksPerAccount);
				addOutlink(account, otherAccount, BlockHeight.ONE, outlinkStrength);
			}
		}
		
		return accounts;
	}

	private static Account createAccountWithBalance(long numNEM) {
		final Account account = Utils.generateRandomAccount();
		account.incrementBalance(Amount.fromNem(numNEM));
		return account;
	}

	private static MockAccount createMockAccountWithBalance(long numNEM) {
		final MockAccount account = new MockAccount(Utils.generateRandomAddress());
		account.incrementBalance(Amount.fromNem(numNEM));
		return account;
	}

	private static ColumnVector getAccountImportances(
			final BlockHeight blockHeight,
			final Collection<Account> accounts) {
		final PoiImportanceGenerator poi = new PoiAlphaImportanceGeneratorImpl();
		return poi.getAccountImportances(blockHeight, accounts);
	}
}
