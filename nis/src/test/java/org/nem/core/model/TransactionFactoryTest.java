package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

public class TransactionFactoryTest {

	@Test(expected = IllegalArgumentException.class)
	public void cannotDeserializeUnknownTransaction() {
		// Arrange:
		final JSONObject object = new JSONObject();
		object.put("type", 7);
		final JsonDeserializer deserializer = new JsonDeserializer(object, null);

		// Act:
		TransactionFactory.VERIFIABLE.deserialize(deserializer);
	}

	//region TransferTransaction

	@Test
	public void canDeserializeVerifiableTransferTransaction() {
		// Arrange:
		final Transaction originalTransaction = createTransferTransaction();

		// Assert:
		assertCanDeserializeVerifiable(originalTransaction, TransferTransaction.class, TransactionTypes.TRANSFER);
	}

	@Test
	public void canDeserializeNonVerifiableTransferTransaction() {
		// Arrange:
		final Transaction originalTransaction = createTransferTransaction();

		// Assert:
		assertCanDeserializeNonVerifiable(originalTransaction, TransferTransaction.class, TransactionTypes.TRANSFER);
	}

	private static Transaction createTransferTransaction() {
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		return new TransferTransaction(TimeInstant.ZERO, sender, recipient, new Amount(100), null);
	}

	//endregion

	//region ImportanceTransferTransaction

	@Test
	public void canDeserializeVerifiableImportanceTransferTransaction() {
		// Arrange:
		final Transaction originalTransaction = createImportanceTransferTransaction();

		// Assert:
		assertCanDeserializeVerifiable(originalTransaction, ImportanceTransferTransaction.class, TransactionTypes.IMPORTANCE_TRANSFER);
	}

	@Test
	public void canDeserializeNonVerifiableImportanceTransferTransaction() {
		// Arrange:
		final Transaction originalTransaction = createImportanceTransferTransaction();

		// Assert:
		assertCanDeserializeNonVerifiable(originalTransaction, ImportanceTransferTransaction.class, TransactionTypes.IMPORTANCE_TRANSFER);
	}

	private static Transaction createImportanceTransferTransaction() {
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		return new ImportanceTransferTransaction(
				TimeInstant.ZERO,
				sender,
				ImportanceTransferTransaction.Mode.Activate,
				recipient);
	}

	//endregion

	//region MultisigSignerModificationTransaction

	@Test
	public void canDeserializeVerifiableMultisigSignerModificationTransaction() {
		// Arrange:
		final Transaction originalTransaction = createMultisigSignerModificationTransaction();

		// Assert:
		assertCanDeserializeVerifiable(originalTransaction, MultisigSignerModificationTransaction.class, TransactionTypes.MULTISIG_SIGNER_MODIFY);
	}

	@Test
	public void canDeserializeNonVerifiableMultisigSignerModificationTransaction() {
		// Arrange:
		final Transaction originalTransaction = createMultisigSignerModificationTransaction();

		// Assert:
		assertCanDeserializeNonVerifiable(originalTransaction, MultisigSignerModificationTransaction.class, TransactionTypes.MULTISIG_SIGNER_MODIFY);
	}

	private static Transaction createMultisigSignerModificationTransaction() {
		final Account sender = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		return new MultisigSignerModificationTransaction(
				TimeInstant.ZERO,
				sender,
				MultisigSignerModificationTransaction.ModificationType.Add,
				cosignatory);
	}

	//endregion

	private static void assertCanDeserializeVerifiable(
			final Transaction originalTransaction,
			final Class expectedClass,
			final int expectedType) {
		// Act:
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, new MockAccountLookup());
		final Transaction transaction = TransactionFactory.VERIFIABLE.deserialize(deserializer);

		// Assert:
		Assert.assertThat(transaction, IsInstanceOf.instanceOf(expectedClass));
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(expectedType));
		Assert.assertThat(transaction.getSignature(), IsNull.notNullValue());
	}

	private static void assertCanDeserializeNonVerifiable(
			final Transaction originalTransaction,
			final Class expectedClass,
			final int expectedType) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction.asNonVerifiable(), new MockAccountLookup());
		final Transaction transaction = TransactionFactory.NON_VERIFIABLE.deserialize(deserializer);

		// Assert:
		Assert.assertThat(transaction, IsInstanceOf.instanceOf(expectedClass));
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(expectedType));
		Assert.assertThat(transaction.getSignature(), IsNull.nullValue());
	}
}
