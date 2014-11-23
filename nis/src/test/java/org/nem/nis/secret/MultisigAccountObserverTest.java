package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.poi.*;

public class MultisigAccountObserverTest {
	@Test
	public void notifyTransferExecuteAddAddsMultisigLinks() {
		final TestContext context = notifyTransferPrepare(MultisigModificationType.Add.value(), NotificationTrigger.Execute);

		// Assert:
		Mockito.verify(context.poiAccount1State, Mockito.times(1)).addCosignatory(context.account2.getAddress(), new BlockHeight(111));
		Mockito.verify(context.poiAccount2State, Mockito.times(1)).addMultisig(context.account1.getAddress(), new BlockHeight(111));
	}

	@Test
	public void notifyTransferUndoAddRemovesMultisigLinks() {
		final TestContext context = notifyTransferPrepare(MultisigModificationType.Add.value(), NotificationTrigger.Undo);

		// Assert:
		Mockito.verify(context.poiAccount1State, Mockito.times(1)).removeCosignatory(context.account2.getAddress(), new BlockHeight(111));
		Mockito.verify(context.poiAccount2State, Mockito.times(1)).removeMultisig(context.account1.getAddress(), new BlockHeight(111));
	}

	@Test
	public void notifyTransferExecuteDelRemovedMultisigLinks() {
		final TestContext context = notifyTransferPrepare(MultisigModificationType.Add.value() + 1, NotificationTrigger.Execute);

		// Assert:
		Mockito.verify(context.poiAccount1State, Mockito.times(1)).removeCosignatory(context.account2.getAddress(), new BlockHeight(111));
		Mockito.verify(context.poiAccount2State, Mockito.times(1)).removeMultisig(context.account1.getAddress(), new BlockHeight(111));
	}

	@Test
	public void notifyTransferUndoDelAddsMultisigLinks() {
		final TestContext context = notifyTransferPrepare(MultisigModificationType.Add.value() + 1, NotificationTrigger.Undo);

		// Assert:
		Mockito.verify(context.poiAccount1State, Mockito.times(1)).addCosignatory(context.account2.getAddress(), new BlockHeight(111));
		Mockito.verify(context.poiAccount2State, Mockito.times(1)).addMultisig(context.account1.getAddress(), new BlockHeight(111));
	}


	private TestContext notifyTransferPrepare(final int value, final NotificationTrigger notificationTrigger) {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigAccountObserver observer = context.createObserver();

		// Act:
		observer.notify(
				new CosignatoryModificationNotification(context.account1, context.account2, value),
				new BlockNotificationContext(new BlockHeight(111), notificationTrigger));
		return context;
	}

	private class TestContext {
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final Account account1 = Utils.generateRandomAccount();
		private final Account account2 = Utils.generateRandomAccount();
		final PoiAccountState poiAccount1State = Mockito.mock(PoiAccountState.class);
		final PoiAccountState poiAccount2State = Mockito.mock(PoiAccountState.class);

		public MultisigAccountObserver createObserver() {
			this.hook(this.account1, this.poiAccount1State);
			this.hook(this.account2, this.poiAccount2State);
			return new MultisigAccountObserver(this.poiFacade);
		}

		public void hook(final Account account, final PoiAccountState accountState) {
			final Address address = account.getAddress();
			Mockito.when(this.poiFacade.findStateByAddress(account.getAddress())).thenReturn(accountState);
			Mockito.when(accountState.getAddress()).thenReturn(address);
		}
	}
}
