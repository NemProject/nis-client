package org.nem.nis.secret;

import org.nem.core.model.observers.BalanceCommitTransferObserver;
import org.nem.nis.*;
import org.nem.nis.poi.PoiFacade;

/**
 * Factory for creating BlockTransactionObserver objects.
 */
public class BlockTransactionObserverFactory {

	/**
	 * Creates a block transaction observer that commits all changes in order.
	 * This observer is appropriate for an execute operation.
	 *
	 * @param accountAnalyzer The account analyzer.
	 * @return The observer.
	 */
	public BlockTransactionObserver createExecuteCommitObserver(final AccountAnalyzer accountAnalyzer) {
		return createBuilder(accountAnalyzer).build();
	}

	/**
	 * Creates a block transaction observer that commits all changes in reverse order.
	 * This observer is appropriate for an undo operation.
	 *
	 * @param accountAnalyzer The account analyzer.
	 * @return The observer.
	 */
	public BlockTransactionObserver createUndoCommitObserver(final AccountAnalyzer accountAnalyzer) {
		return createBuilder(accountAnalyzer).buildReverse();
	}

	private static AggregateBlockTransactionObserverBuilder createBuilder(final AccountAnalyzer accountAnalyzer) {
		final PoiFacade poiFacade = accountAnalyzer.getPoiFacade();
		final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();
		builder.add(new WeightedBalancesObserver(accountAnalyzer.getPoiFacade()));
		builder.add(new AccountsHeightObserver(accountAnalyzer));
		builder.add(new BalanceCommitTransferObserver());
		builder.add(new HarvestRewardCommitObserver(poiFacade, accountAnalyzer.getAccountCache()));
		builder.add(new RemoteObserver(poiFacade));
		builder.add(new OutlinkObserver(poiFacade));
		return builder;
	}
}
