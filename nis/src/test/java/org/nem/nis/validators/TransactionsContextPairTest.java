package org.nem.nis.validators;

import org.hamcrest.core.IsSame;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Transaction;
import org.nem.core.test.IsEquivalent;

import java.util.*;

public class TransactionsContextPairTest {

	@Test
	public void canCreatePairWithSingleTransaction() {
		// Arrange:
		final ValidationContext context = new ValidationContext();
		final Transaction transaction = Mockito.mock(Transaction.class);

		// Act:
		final TransactionsContextPair pair = new TransactionsContextPair(transaction, context);

		// Assert:
		Assert.assertThat(pair.getContext(), IsSame.sameInstance(context));
		Assert.assertThat(pair.getTransactions(), IsEquivalent.equivalentTo(Arrays.asList(transaction)));
	}

	@Test
	public void canCreatePairWithMultipleTransactions() {
		// Arrange:
		final ValidationContext context = new ValidationContext();
		final Collection<Transaction> transactions = Arrays.asList(
				Mockito.mock(Transaction.class),
				Mockito.mock(Transaction.class),
				Mockito.mock(Transaction.class));

		// Act:
		final TransactionsContextPair pair = new TransactionsContextPair(transactions, context);

		// Assert:
		Assert.assertThat(pair.getContext(), IsSame.sameInstance(context));
		Assert.assertThat(pair.getTransactions(), IsSame.sameInstance(transactions));
	}
}