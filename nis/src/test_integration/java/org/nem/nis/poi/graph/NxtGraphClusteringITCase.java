package org.nem.nis.poi.graph;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.nis.state.*;

import java.util.*;

/**
 * Integration test class for analysis the Proof-of-Importance algorithm with the NXT transaction graph.
 */
public class NxtGraphClusteringITCase extends GraphClusteringITCase {
	private static final int DEFAULT_END_HEIGHT = 300000;
	private static final String BLOCKCHAIN_TYPE = "Nxt";

	private static final long SUPPLY_NORMALIZATION_FACTOR = (long)(8999999999.0 / 100000000000.0); // Convert from 1 billion Nxt (10^8 precision)
	private static final long MKT_CAP_NORMALIZATION_FACTOR = (long)(12000000.0 / 4000000.0); // Nxt mkt cap / Nem mkt cap (in USD)

	public NxtGraphClusteringITCase() {
		super(DEFAULT_END_HEIGHT, BLOCKCHAIN_TYPE, new NxtDatabaseRepository());
	}

	@Test
	public void canQueryNxtTransactionTable() {
		// Act:
		final Collection<GraphClusteringTransaction> transactions = this.loadTransactionData(0, 0);

		// Assert:
		Assert.assertThat(transactions.size(), IsEqual.equalTo(73));
	}

	protected Map<Address, AccountState> createAccountStatesFromTransactionData(final Collection<GraphClusteringTransaction> transactions) {
		LOGGER.info("Creating PoiAccountStates from Nxt transaction data...");

		final Map<Address, AccountState> accountStateMap = new HashMap<>();

		// 1. Create accounts in the genesis block.
		final Amount genesisAmount = Amount.fromNem(this.normalizeNxtToNemMktCap(1000000000)); // normalize w.r.t. market cap
		final AccountState genesis = createAccountWithBalance(Address.fromEncoded("1739068987193023818"), 1, genesisAmount);
		accountStateMap.put(genesis.getAddress(), genesis);

		// 2. Iterate through transactions, creating new accounts as needed.
		for (final GraphClusteringTransaction trans : transactions) {
			final Amount amount = Amount.fromMicroNem(this.normalizeNxtToNemMktCap(trans.getAmount())); // normalize w.r.t. market cap
			final Address sender = Address.fromEncoded(Long.toString(trans.getSenderId()));
			final Address recipient = Address.fromEncoded(Long.toString(trans.getRecipientId()));
			final BlockHeight blockHeight = new BlockHeight(trans.getHeight() + 1); // NXT blocks start at 0 but NEM blocks start at 1

			if (!accountStateMap.containsKey(recipient)) {
				accountStateMap.put(recipient, new AccountState(recipient));
			}

			final AccountState senderAccountState = accountStateMap.get(sender);
			final AccountState recipientAccountState = accountStateMap.get(recipient);
			final long balance = mapPoiAccountStateToBalance(senderAccountState, blockHeight).getNumMicroNem();

			// We need to add some balance sometimes because the transactions don't account for fees earned from harvested blocks
			final long remainingBalance = balance - amount.getNumMicroNem();
			if (remainingBalance < 0) {
				senderAccountState.getWeightedBalances().addFullyVested(new BlockHeight(blockHeight.getRaw()), Amount.fromMicroNem(amount.getNumMicroNem()));
			}

			senderAccountState.getWeightedBalances().addSend(blockHeight, amount);
			senderAccountState.getImportanceInfo().addOutlink(
					new AccountLink(blockHeight, amount, recipientAccountState.getAddress()));

			recipientAccountState.getWeightedBalances().addReceive(blockHeight, amount);
		}

		LOGGER.info("Creating PoiAccountStates finished...");
		return accountStateMap;
	}

	private long normalizeNxtToNemSupply(final long amt) {
		return amt * SUPPLY_NORMALIZATION_FACTOR;
	}

	private long normalizeNxtToNemMktCap(final long amt) {
		return amt * MKT_CAP_NORMALIZATION_FACTOR;
	}
}
