package org.nem.nis.service;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.node.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.test.*;
import org.nem.core.time.*;
import org.nem.nis.*;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.test.NisUtils;
import org.nem.nis.validators.*;
import org.nem.peer.*;

public class PushServiceTest {

	//region pushTransaction

	@Test
	public void pushTransactionVerifyFailureIncrementsFailedExperience() {
		// Arrange:
		final TransactionValidator validator = Mockito.mock(TransactionValidator.class);
		Mockito.when(validator.validate(Mockito.any())).thenReturn(ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
		final TestContext context = new TestContext(validator);
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 12);
		transaction.setDeadline(MockTransaction.TIMESTAMP.addDays(-10));
		transaction.signBy(Utils.generateRandomAccount());

		// Act:
		context.service.pushTransaction(transaction, context.remoteNodeIdentity);

		// Assert:
		context.assertSingleUpdateExperience(ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE);
		Mockito.verify(validator, Mockito.never()).validate(Mockito.any());
		Mockito.verify(context.network, Mockito.never()).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushTransactionValidFailureIncrementsFailedExperience() {
		// Arrange:
		final TransactionValidator validator = Mockito.mock(TransactionValidator.class);
		Mockito.when(validator.validate(Mockito.any())).thenReturn(ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
		final TestContext context = new TestContext(validator);
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 12);
		transaction.setDeadline(MockTransaction.TIMESTAMP.addDays(-10));
		transaction.sign();

		// Act:
		context.service.pushTransaction(transaction, context.remoteNodeIdentity);

		// Assert:
		context.assertSingleUpdateExperience(ValidationResult.FAILURE_UNKNOWN);
		Mockito.verify(validator, Mockito.only()).validate(Mockito.any());
		Mockito.verify(context.network, Mockito.never()).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushTransactionValidSuccessAcceptedFailureIncrementsFailedExperience() {
		// Assert:
		final TestContext context = assertPushTransactionExperienceChange(ValidationResult.FAILURE_UNKNOWN);
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushTransactionValidSuccessAcceptedNeutralDoesNotChangeExperience() {
		// Assert:
		final TestContext context = assertPushTransactionExperienceChange(ValidationResult.NEUTRAL);
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushTransactionValidSuccessAcceptedSuccessIncrementsSuccessExperienceAndBroadcasts() {
		// Assert:
		final TestContext context = assertPushTransactionExperienceChange(ValidationResult.SUCCESS);

		final ArgumentCaptor<SerializableEntity> broadcastEntityArgument = ArgumentCaptor.forClass(SerializableEntity.class);
		Mockito.verify(context.network, Mockito.times(1))
				.broadcast(Mockito.eq(NodeApiId.REST_PUSH_TRANSACTION), broadcastEntityArgument.capture());

		final SecureSerializableEntity<?> secureEntity = (SecureSerializableEntity<?>)(broadcastEntityArgument.getValue());
		Assert.assertThat(secureEntity.getIdentity(), IsEqual.equalTo(context.localNodeIdentity));
		Assert.assertThat(((MockTransaction)secureEntity.getEntity()).getCustomField(), IsEqual.equalTo(12));
	}

	private static TestContext assertPushTransactionExperienceChange(final ValidationResult result) {
		// Arrange:
		final TestContext context = new TestContext();
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 12);
		transaction.sign();

		Mockito.when(context.unconfirmedTransactions.addNew(transaction)).thenReturn(result);

		// Act:
		context.service.pushTransaction(transaction, context.remoteNodeIdentity);

		// Assert:
		context.assertSingleUpdateExperience(result);
		return context;
	}

	//endregion

	//region pushBlock

	@Test
	public void pushBlockValidFailureIncrementsFailedExperience() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = NisUtils.createRandomBlockWithHeight(12);
		block.sign();

		Mockito.when(context.blockChain.checkPushedBlock(block)).thenReturn(ValidationResult.FAILURE_UNKNOWN);

		// Act:
		context.service.pushBlock(block, context.remoteNodeIdentity);

		// Assert:
		context.assertSingleUpdateExperience(ValidationResult.FAILURE_UNKNOWN);
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushBlockValidFailureEntityUnusableDoesNotChangeExperienceAndDoesNotGetProcessedOrBroadcast() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = NisUtils.createRandomBlockWithHeight(12);
		block.sign();

		Mockito.when(context.blockChain.checkPushedBlock(block)).thenReturn(ValidationResult.FAILURE_ENTITY_UNUSABLE);

		// Act:
		context.service.pushBlock(block, context.remoteNodeIdentity);

		// Assert:
		context.assertNoUpdateExperience();
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
		Mockito.verify(context.blockChain, Mockito.times(0)).processBlock(Mockito.any());
	}

	@Test
	public void pushBlockValidNeutralAcceptedFailureIncrementsFailedExperience() {
		// Assert:
		final TestContext context = assertPushBlockExperienceChange(ValidationResult.NEUTRAL, ValidationResult.FAILURE_UNKNOWN);
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushBlockValidSuccessAcceptedFailureIncrementsFailedExperience() {
		// Assert:
		final TestContext context = assertPushBlockExperienceChange(ValidationResult.SUCCESS, ValidationResult.FAILURE_UNKNOWN);
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushBlockValidNeutralAcceptedNeutralDoesNotChangeExperience() {
		// Assert:
		final TestContext context = assertPushBlockExperienceChange(ValidationResult.NEUTRAL, ValidationResult.NEUTRAL);
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushBlockValidSuccessAcceptedNeutralDoesNotChangeExperience() {
		// Assert:
		final TestContext context = assertPushBlockExperienceChange(ValidationResult.SUCCESS, ValidationResult.NEUTRAL);
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushBlockValidNeutralAcceptedSuccessIncrementsSuccessExperienceAndBroadcasts() {
		// Assert:
		assertPushBlockAcceptedSuccessIncrementsSuccessExperienceAndBroadcasts(ValidationResult.NEUTRAL);
	}

	@Test
	public void pushBlockValidSuccessAcceptedSuccessIncrementsSuccessExperienceAndBroadcasts() {
		// Assert:
		assertPushBlockAcceptedSuccessIncrementsSuccessExperienceAndBroadcasts(ValidationResult.SUCCESS);
	}

	private static void assertPushBlockAcceptedSuccessIncrementsSuccessExperienceAndBroadcasts(
			final ValidationResult validResult) {
		// Assert:
		final TestContext context = assertPushBlockExperienceChange(validResult, ValidationResult.SUCCESS);

		final ArgumentCaptor<SerializableEntity> broadcastEntityArgument = ArgumentCaptor.forClass(SerializableEntity.class);
		Mockito.verify(context.network, Mockito.times(1))
				.broadcast(Mockito.eq(NodeApiId.REST_PUSH_BLOCK), broadcastEntityArgument.capture());

		final SecureSerializableEntity<?> secureEntity = (SecureSerializableEntity<?>)(broadcastEntityArgument.getValue());
		Assert.assertThat(secureEntity.getIdentity(), IsEqual.equalTo(context.localNodeIdentity));
		Assert.assertThat(((Block)secureEntity.getEntity()).getHeight(), IsEqual.equalTo(new BlockHeight(14)));
	}

	private static TestContext assertPushBlockExperienceChange(
			final ValidationResult validResult,
			final ValidationResult acceptedResult) {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = NisUtils.createRandomBlockWithHeight(14);
		block.sign();

		Mockito.when(context.blockChain.checkPushedBlock(block)).thenReturn(validResult);
		Mockito.when(context.blockChain.processBlock(block)).thenReturn(acceptedResult);

		// Act:
		context.service.pushBlock(block, context.remoteNodeIdentity);

		// Assert:
		context.assertSingleUpdateExperience(acceptedResult);
		return context;
	}

	//endregion

	//region other

	@Test
	public void pushCanFailForUnidentifiedNode() {
		// Arrange:
		final TestContext context = new TestContext();
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 12);
		transaction.setDeadline(MockTransaction.TIMESTAMP.addDays(-10));
		transaction.sign();

		// Act:
		context.service.pushTransaction(transaction, null);

		// Assert:
		context.assertNoUpdateExperience();
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushCanSucceedForUnidentifiedNode() {
		// Arrange:
		final TestContext context = new TestContext();
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 12);
		transaction.sign();

		Mockito.when(context.unconfirmedTransactions.addNew(transaction)).thenReturn(ValidationResult.SUCCESS);

		// Act:
		context.service.pushTransaction(transaction, null);

		// Assert:
		context.assertNoUpdateExperience();

		final ArgumentCaptor<SerializableEntity> broadcastEntityArgument = ArgumentCaptor.forClass(SerializableEntity.class);
		Mockito.verify(context.network, Mockito.times(1))
				.broadcast(Mockito.eq(NodeApiId.REST_PUSH_TRANSACTION), broadcastEntityArgument.capture());

		final SecureSerializableEntity<?> secureEntity = (SecureSerializableEntity<?>)(broadcastEntityArgument.getValue());
		Assert.assertThat(secureEntity.getIdentity(), IsEqual.equalTo(context.localNodeIdentity));
		Assert.assertThat(((MockTransaction)secureEntity.getEntity()).getCustomField(), IsEqual.equalTo(12));
	}

	@Test
	public void pushTransactionReturnsValidationResultNeutralIfTransactionIsInCache() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = createMockTransaction();
		Mockito.when(context.unconfirmedTransactions.addNew(transaction)).thenReturn(ValidationResult.SUCCESS);

		// Act:
		context.service.pushTransaction(transaction, null);
		final ValidationResult result = context.service.pushTransaction(transaction, null);

		// Assert:
		// if the transaction in the second pushTransaction call would have gone past the cache check,
		// then there would 2 calls to getTape().
		Mockito.verify(transaction, Mockito.times(1)).getType();
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	@Test
	public void pushTransactionPrunesTransactionHashCache() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = createMockTransaction();
		Mockito.when(context.unconfirmedTransactions.addNew(transaction)).thenReturn(ValidationResult.SUCCESS);

		// Act:
		context.service.pushTransaction(transaction, null);

		// time provider supplies time stamp 5 seconds later than first one --> first transaction not pruned.
		context.service.pushTransaction(transaction, null);

		// time provider supplies time stamp 11 seconds later than second one --> first and second transaction pruned.
		context.service.pushTransaction(transaction, null);

		// Assert:
		Mockito.verify(transaction, Mockito.times(2)).getType();
	}

	private Transaction createMockTransaction() {
		final Transaction transaction = Mockito.mock(Transaction.class);
		final MockTransaction mockTransaction = new MockTransaction(Utils.generateRandomAccount(), 12);
		mockTransaction.sign();
		final Account account = Utils.generateRandomAccount();
		Mockito.when(transaction.verify()).thenReturn(true);
		Mockito.when(transaction.getType()).thenReturn(TransactionTypes.TRANSFER);
		Mockito.when(transaction.getSigner()).thenReturn(account);
		Mockito.when(transaction.asNonVerifiable()).thenReturn(mockTransaction);
		Mockito.when(transaction.getTimeStamp()).thenReturn(new TimeInstant(1122448));
		Mockito.when(transaction.getDeadline()).thenReturn(new TimeInstant(1122448).addHours(2));
		Mockito.when(transaction.getFee()).thenReturn(new Amount(5));
		return transaction;
	}

	//endregion

	private static class TestContext {
		private final NodeIdentity remoteNodeIdentity;
		private final Node remoteNode;
		private final NodeIdentity localNodeIdentity;
		private final PeerNetwork network;
		private final UnconfirmedTransactions unconfirmedTransactions;
		private final BlockChain blockChain;
		private final TimeProvider timeProvider;
		private final PushService service;

		public TestContext() {
			this(new UniversalTransactionValidator());
		}

		public TestContext(final TransactionValidator validator) {
			this.remoteNodeIdentity = new NodeIdentity(new KeyPair());
			this.remoteNode = new Node(this.remoteNodeIdentity, NodeEndpoint.fromHost("10.0.0.1"));

			this.localNodeIdentity = new NodeIdentity(new KeyPair());
			final Node localNode = new Node(this.localNodeIdentity, NodeEndpoint.fromHost("10.0.0.2"));

			final NodeCollection collection = new NodeCollection();
			collection.update(this.remoteNode, NodeStatus.ACTIVE);

			this.network = Mockito.mock(PeerNetwork.class);
			Mockito.when(this.network.getNodes()).thenReturn(collection);
			Mockito.when(this.network.getLocalNode()).thenReturn(localNode);

			this.unconfirmedTransactions = Mockito.mock(UnconfirmedTransactions.class);
			this.blockChain = Mockito.mock(BlockChain.class);
			this.timeProvider = Mockito.mock(TimeProvider.class);
			Mockito.when(this.timeProvider.getCurrentTime())
					.thenReturn(
							new TimeInstant(1122448),
							new TimeInstant(1122448),
							new TimeInstant(1122448 + 5),
							new TimeInstant(1122448 + 16),
							new TimeInstant(1122448 + 16));

			final NisPeerNetworkHost host = Mockito.mock(NisPeerNetworkHost.class);
			Mockito.when(host.getNetwork()).thenReturn(this.network);

			this.service = new PushService(
					this.unconfirmedTransactions,
					validator,
					this.blockChain,
					host,
					this.timeProvider);
		}

		public void assertNoUpdateExperience() {
			Mockito.verify(this.network, Mockito.times(0)).updateExperience(Mockito.any(), Mockito.any());
		}

		public void assertSingleUpdateExperience(final ValidationResult expectedResult) {
			Mockito.verify(this.network, Mockito.times(1))
					.updateExperience(this.remoteNode, NodeInteractionResult.fromValidationResult(expectedResult));
			Mockito.verify(this.network, Mockito.times(1))
					.updateExperience(Mockito.eq(this.remoteNode), Mockito.any());
		}
	}
}