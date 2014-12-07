package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class MultisigSignerModificationTransactionTest {
	private static final TimeInstant TIME = new TimeInstant(123);
	final MultisigModificationType MODIFICATION_ADD = MultisigModificationType.Add;
	final MultisigModificationType MODIFICATION_UNKNOWN = MultisigModificationType.Unknown;

	//region constructor

	@Test
	public void ctorCanCreateMultisigModification() {
		// Arrange:
		final MultisigModificationType modificationType = MODIFICATION_ADD;
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();

		// Act:
		final MultisigSignerModificationTransaction transaction = createMultisigSignerModificationTransaction(signer, modificationType, cosignatory);

		// Assert:
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getModifications().size(), IsEqual.equalTo(1));
		final MultisigModification modification = transaction.getModifications().get(0);
		Assert.assertThat(modification.getCosignatory(), IsEqual.equalTo(cosignatory));
		Assert.assertThat(modification.getModificationType(), IsEqual.equalTo(modificationType));
	}

	@Test(expected = IllegalArgumentException.class)
	public void multisigModificationCannotBeCreatedWithoutCosignatory() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();

		// Act:
		createMultisigSignerModificationTransaction(signer, MODIFICATION_ADD, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void multisigModificationCannotBeCreatedWithUnknownModificationType() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();

		// Act:
		createMultisigSignerModificationTransaction(signer, MODIFICATION_UNKNOWN, cosignatory);
	}

	@Test
	public void deserializationFailsWhenCosignatoryIsMissing() {
		// Assert:
		this.assertDeserializationFailure(jsonObject -> jsonObject.put("cosignatoryAccount", null));
	}

	@Test
	public void deserializationFailsWhenModeIsInvalid() {
		// Assert:
		this.assertDeserializationFailure(jsonObject -> jsonObject.put("modificationType", 123));
	}

	private void assertDeserializationFailure(final Consumer<JSONObject> invalidateJsonConsumer) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, cosignatory);

		final MultisigSignerModificationTransaction originalEntity = createMultisigSignerModificationTransaction(
				signer,
				MODIFICATION_ADD,
				cosignatory);
		originalEntity.sign();
		final JSONObject jsonObject = JsonSerializer.serializeToJson(originalEntity);
		invalidateJsonConsumer.accept(jsonObject); // invalidate the json

		// Act:
		final Deserializer deserializer = new JsonDeserializer(jsonObject, new DeserializationContext(accountLookup));
		deserializer.readInt("type");
		ExceptionAssert.assertThrows(
				v -> new MultisigSignerModificationTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer),
				SerializationException.class);
	}

	// endregion

	// region roundtrip

	@Test
	public void canRoundtripMultisigModification() {
		// Arrange:
		final MultisigModificationType modificationType = MODIFICATION_ADD;
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, cosignatory);
		final MultisigSignerModificationTransaction originalTransaction = createMultisigSignerModificationTransaction(signer, modificationType, cosignatory);

		// Act:
		final MultisigSignerModificationTransaction transaction = this.createRoundTrippedTransaction(originalTransaction, accountLookup);

		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MULTISIG_SIGNER_MODIFY));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		final MultisigModification modification = transaction.getModifications().get(0);
		Assert.assertThat(modification.getCosignatory(), IsEqual.equalTo(cosignatory));
		Assert.assertThat(modification.getModificationType(), IsEqual.equalTo(modificationType));
	}

	private MultisigSignerModificationTransaction createRoundTrippedTransaction(
			final MultisigSignerModificationTransaction originalTransaction,
			final AccountLookup accountLookup) {
		// Act:
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, accountLookup);
		deserializer.readInt("type");
		return new MultisigSignerModificationTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
	}

	// endregion

	//region fee

	@Test
	public void minimumFeeIsOneThousandNem() {
		// Arrange:
		final MultisigModificationType modificationType = MODIFICATION_ADD;
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final MultisigSignerModificationTransaction transaction = createMultisigSignerModificationTransaction(signer, modificationType, cosignatory);

		// Act + Assert:
		Assert.assertThat(transaction.getMinimumFee(), IsEqual.equalTo(Amount.fromNem(1000)));
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(Amount.fromNem(1000)));
	}

	//endregion

	//region execute / undo

	@Test
	public void executeRaisesAppropriateNotifications() {
		// Arrange:
		final MultisigModificationType modificationType = MODIFICATION_ADD;
		final Account signer = Utils.generateRandomAccount(Amount.fromNem(90));
		final Account cosignatory = Utils.generateRandomAccount();
		final MultisigSignerModificationTransaction transaction = createMultisigSignerModificationTransaction(signer, modificationType, cosignatory);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(0), cosignatory);
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(1), signer, Amount.fromNem(1000));
		NotificationUtils.assertCosignatoryModificationNotification(
				notificationCaptor.getAllValues().get(2),
				signer,
				cosignatory,
				ImportanceTransferTransaction.Mode.Activate.value());
	}

	@Test
	public void undoRaisesAppropriateNotifications() {
		// Arrange:
		final MultisigModificationType modificationType = MODIFICATION_ADD;
		final Account signer = Utils.generateRandomAccount(Amount.fromNem(90));
		final Account cosignatory = Utils.generateRandomAccount();
		final MultisigSignerModificationTransaction transaction = createMultisigSignerModificationTransaction(signer, modificationType, cosignatory);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(2), cosignatory);
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(1), signer, Amount.fromNem(1000));
		NotificationUtils.assertCosignatoryModificationNotification(
				notificationCaptor.getAllValues().get(0),
				signer,
				cosignatory,
				ImportanceTransferTransaction.Mode.Activate.value());
	}

	// endregion

	private static MultisigSignerModificationTransaction createMultisigSignerModificationTransaction(
			final Account sender,
			final MultisigModificationType modificationType,
			final Account cosignatory) {
		final List<MultisigModification> modifications = Arrays.asList(new MultisigModification(modificationType, cosignatory));
		return new MultisigSignerModificationTransaction(TIME, sender, modifications);
	}
}
