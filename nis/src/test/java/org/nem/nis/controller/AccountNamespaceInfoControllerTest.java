package org.nem.nis.controller;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.Utils;
import org.nem.nis.test.*;

import java.util.*;
import java.util.stream.Collectors;

public class AccountNamespaceInfoControllerTest {

	@Test
	public void accountGetMosaicDefinitionsDelegatesToNamespaceCache() {
		// Arrange:
		final ThreeMosaicsTestContext context = new ThreeMosaicsTestContext();

		// Act:
		final SerializableList<MosaicDefinition> returnedMosaicDefinitions1 = this.getAccountMosaicDefinitions(context, context.address);
		final SerializableList<MosaicDefinition> returnedMosaicDefinitions2 = this.getAccountMosaicDefinitions(context, context.another);

		// Assert:
		context.assertAccountStateDelegation();
		context.assertNamespaceCacheNumGetDelegations(4); // two from first call and two from second
		context.assertMosaicDefinitionsOwned(
				returnedMosaicDefinitions1.asCollection(),
				Arrays.asList(MosaicConstants.MOSAIC_ID_XEM, context.mosaicId1, context.mosaicId2));
		context.assertMosaicDefinitionsOwned(
				returnedMosaicDefinitions2.asCollection(),
				Arrays.asList(MosaicConstants.MOSAIC_ID_XEM, context.mosaicId2, context.mosaicId3));
	}

	@Test
	public void accountGetMosaicDefinitionsBatchDelegatesToNamespaceCache() {
		// Arrange:
		final ThreeMosaicsTestContext context = new ThreeMosaicsTestContext();

		// Act:
		final SerializableList<MosaicDefinition> returnedMosaicDefinitions = this.getAccountMosaicDefinitionsBatch(
				context,
				Arrays.asList(context.address, context.another));

		// Assert:
		context.assertAccountStateDelegation();
		context.assertNamespaceCacheNumGetDelegations(4);
		context.assertMosaicDefinitionsOwned(
				returnedMosaicDefinitions.asCollection(),
				Arrays.asList(MosaicConstants.MOSAIC_ID_XEM, context.mosaicId1, context.mosaicId2, context.mosaicId3));
	}

	@Test
	public void accountGetOwnedMosaicsDelegatesToNamespaceCache() {
		// Arrange:
		final ThreeMosaicsTestContext context = new ThreeMosaicsTestContext();
		context.setBalance(context.mosaicId1, context.address, new Quantity(123));
		context.setBalance(context.mosaicId1, context.another, new Quantity(789));
		context.setBalance(context.mosaicId3, context.address, new Quantity(456));
		context.setBalance(context.mosaicId2, context.another, new Quantity(528));

		// Act:
		final SerializableList<Mosaic> returnedMosaics1 = this.getOwnedMosaics(context, context.address);
		final SerializableList<Mosaic> returnedMosaics2 = this.getOwnedMosaics(context, context.another);

		// Assert:
		// - note that the returned mosaics are based on what the account is reported to own via its info
		// - in "production" zero-balance mosaics should be excluded out and non-zero-balance mosaics should be included
		// - but this is a test where we do not have that constraint
		context.assertAccountStateDelegation();
		context.assertNamespaceCacheNumGetDelegations(4 + 4); // two from first call and two from second + four additional calls in setBalance
		context.assertMosaicsOwned(
				returnedMosaics1.asCollection(),
				Arrays.asList(new Mosaic(context.mosaicId1, new Quantity(123)), new Mosaic(context.mosaicId2, Quantity.ZERO)));
		context.assertMosaicsOwned(
				returnedMosaics2.asCollection(),
				Arrays.asList(new Mosaic(context.mosaicId2, new Quantity(528)), new Mosaic(context.mosaicId3, Quantity.ZERO)));
	}

	private SerializableList<Mosaic> getOwnedMosaics(final ThreeMosaicsTestContext context, final Address address) {
		return context.controller.accountGetOwnedMosaics(context.getBuilder(address));
	}

	private SerializableList<MosaicDefinition> getAccountMosaicDefinitions(final ThreeMosaicsTestContext context, final Address address) {
		return context.controller.accountGetMosaicDefinitions(context.getBuilder(address));
	}

	private SerializableList<MosaicDefinition> getAccountMosaicDefinitionsBatch(final ThreeMosaicsTestContext context, final List<Address> addresses) {
		final Collection<AccountId> accountIds = addresses.stream().map(a -> new AccountId(a.getEncoded())).collect(Collectors.toList());
		return context.controller.accountGetMosaicDefinitionsBatch(NisUtils.getAccountIdsDeserializer(accountIds));
	}

	private static class ThreeMosaicsTestContext extends MosaicTestContext {
		private final MosaicId mosaicId1 = this.createMosaicId("gimre.games.pong", "paddle");
		private final MosaicId mosaicId2 = this.createMosaicId("gimre.games.pong", "ball");
		private final MosaicId mosaicId3 = this.createMosaicId("gimre.games.pong", "goals");
		private final Address address = Utils.generateRandomAddressWithPublicKey();
		private final Address another = Utils.generateRandomAddressWithPublicKey();

		private final AccountNamespaceInfoController controller;

		public ThreeMosaicsTestContext() {
			this.addXemMosaic();
			this.prepareMosaics(Arrays.asList(this.mosaicId1, this.mosaicId2, this.mosaicId3));
			this.ownsMosaic(this.address, Arrays.asList(this.mosaicId1, this.mosaicId2));
			this.ownsMosaic(this.another, Arrays.asList(this.mosaicId2, this.mosaicId3));

			this.controller = new AccountNamespaceInfoController(
					this.accountStateCache,
					this.namespaceCache);
		}

		public AccountIdBuilder getBuilder(final Address address) {
			final AccountIdBuilder builder = new AccountIdBuilder();
			builder.setAddress(address.getEncoded());
			return builder;
		}

		public void assertAccountStateDelegation() {
			Mockito.verify(this.accountStateCache, Mockito.times(1)).findStateByAddress(this.address);
			Mockito.verify(this.accountStateCache, Mockito.times(1)).findStateByAddress(this.another);
		}

		public void assertNamespaceCacheNumGetDelegations(final int count) {
			// 3 get calls were made by prepareMosaics in the constructor
			Mockito.verify(this.namespaceCache, Mockito.times(3 + count)).get(Mockito.any());
		}
	}
}