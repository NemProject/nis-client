package org.nem.nis.validators;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.observers.Notification;
import org.nem.core.model.observers.TransactionObserver;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.ExceptionAssert;
import org.nem.core.test.NotificationUtils;
import org.nem.core.test.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(Enclosed.class)
public class MultisigAggregateModificationTransactionTest {
	public static class MultisigAggregateModificationTransactionAddTest extends AbstractMultisigSignerModificationTransactionTest {
		@Override
		protected MultisigModificationType getModification() {
			return MultisigModificationType.Add;
		}
	}

	public static class MultisigAggregateModificationTransactionDelTest extends AbstractMultisigSignerModificationTransactionTest {
		@Override
		protected MultisigModificationType getModification() {
			return MultisigModificationType.Del;
		}
	}

	public static class MultisigAggregateModificationTransactionMainTest {
		//region ctor
		@Test
		public void cannotCreateMultisigAggregateModificationWithNullModifications() {
			// Arrange:
			final Account signer = Mockito.mock(Account.class);

			// Act:
			ExceptionAssert.assertThrows(
					v -> new MultisigAggregateModificationTransaction(AbstractMultisigSignerModificationTransactionTest.TIME, signer, null),
					IllegalArgumentException.class);
		}

		@Test
		public void cannotCreateMultisigAggregateModificationWithEmptyModifications() {
			// Arrange:
			final Account signer = Mockito.mock(Account.class);

			// Act:
			ExceptionAssert.assertThrows(
					v -> new MultisigAggregateModificationTransaction(AbstractMultisigSignerModificationTransactionTest.TIME, signer, new ArrayList<>()),
					IllegalArgumentException.class);
		}
		//endregion

		@Test
		public void executeRaisesAccountNotificationForAllModifications() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account cosignatory1 = Utils.generateRandomAccount();
			final Account cosignatory2 = Utils.generateRandomAccount();
			final List<MultisigModification> modifications = Arrays.asList(
					new MultisigModification(MultisigModificationType.Add, cosignatory1),
					new MultisigModification(MultisigModificationType.Del, cosignatory2));

			final MultisigAggregateModificationTransaction transaction =
					AbstractMultisigSignerModificationTransactionTest.createTransaction(signer, modifications);
			transaction.setFee(Amount.fromNem(10));

			// Act:
			final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
			transaction.execute(observer);

			// Assert:
			final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			Mockito.verify(observer, Mockito.times(5)).notify(notificationCaptor.capture());
			NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(0), cosignatory1);
			NotificationUtils.assertCosignatoryModificationNotification(notificationCaptor.getAllValues().get(1), signer, modifications.get(0));
			NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(2), cosignatory2);
			NotificationUtils.assertCosignatoryModificationNotification(notificationCaptor.getAllValues().get(3), signer, modifications.get(1));
			NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(4), signer, Amount.fromNem(1000));
		}
	}
}
