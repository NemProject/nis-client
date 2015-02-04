package org.nem.nis.dbmodel;

import org.hamcrest.core.IsNull;
import org.junit.*;

import java.util.List;
import java.util.function.*;
import java.util.stream.*;

public class DbBlockTest {

	// TODO 20150204 BR -> J: I guess you want me to use the transaction registry instead ? ^^
	@Test
	public void setBlockTransferTransactionsFilterTransactionsWithNullSignature() {
		final DbBlock dbBlock = new DbBlock();
		assertTransactionsWithNullSignatureGetFiltered(
				dbBlock,
				DbBlock::getBlockTransferTransactions,
				DbBlock::setBlockTransferTransactions,
				DbTransferTransaction::new);
	}

	@Test
	public void setBlockImportanceTransferTransactionsFilterTransactionsWithNullSignature() {
		final DbBlock dbBlock = new DbBlock();
		assertTransactionsWithNullSignatureGetFiltered(
				dbBlock,
				DbBlock::getBlockImportanceTransferTransactions,
				DbBlock::setBlockImportanceTransferTransactions,
				DbImportanceTransferTransaction::new);
	}

	@Test
	public void setBlockMultisigAggregateModificationTransactions() {
		final DbBlock dbBlock = new DbBlock();
		assertTransactionsWithNullSignatureGetFiltered(
				dbBlock,
				DbBlock::getBlockMultisigAggregateModificationTransactions,
				DbBlock::setBlockMultisigAggregateModificationTransactions,
				DbMultisigAggregateModificationTransaction::new);
	}

	private <T extends AbstractBlockTransfer> void assertTransactionsWithNullSignatureGetFiltered(
			final DbBlock block,
			final Function<DbBlock, List<T>> getFromBlock,
			final BiConsumer<DbBlock, List<T>> setInBlock,
			final Supplier<T> activator) {
		// Arrange:
		final List<T> dbTransactions = createTransactions(activator);
		addTransactionsWithNullSignature(dbTransactions, activator);

		// Act:
		setInBlock.accept(block, dbTransactions);

		// Assert:
		getFromBlock.apply(block).stream()
				.forEach(t -> Assert.assertThat(t.getSenderProof(), IsNull.notNullValue()));
	}

	private static <T extends AbstractBlockTransfer> List<T> createTransactions(final Supplier<T> activator) {
		return IntStream.range(0, 10)
				.mapToObj(i -> {
					final T t = activator.get();
					t.setSenderProof(new byte[64]);
					return t;
				})
				.collect(Collectors.toList());
	}

	private static <T extends AbstractBlockTransfer> void addTransactionsWithNullSignature(final List<T> dbTransactions, final Supplier<T> activator) {
		dbTransactions.add(0, activator.get());
		dbTransactions.add(5, activator.get());
		dbTransactions.add(activator.get());
	}
}
