package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

public class SmartTileSupplyChangeModelToDbModelMappingTest extends AbstractTransferModelToDbModelMappingTest<SmartTileSupplyChangeTransaction, DbSmartTileSupplyChangeTransaction> {

	@Test
	public void transactionCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final SmartTileSupplyChangeTransaction transfer = context.createModel();

		// Act:
		final DbSmartTileSupplyChangeTransaction dbModel = context.mapping.map(transfer);

		// Assert:
		Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));
		Assert.assertThat(dbModel.getNamespaceId(), IsEqual.equalTo("alice.food"));
		Assert.assertThat(dbModel.getName(), IsEqual.equalTo("apples"));
		Assert.assertThat(dbModel.getSupplyType(), IsEqual.equalTo(SmartTileSupplyType.CreateSmartTiles.value()));
		Assert.assertThat(dbModel.getQuantity(), IsEqual.equalTo(123L));
	}

	@Override
	protected SmartTileSupplyChangeTransaction createModel(final TimeInstant timeStamp, final Account sender) {
		return RandomTransactionFactory.createSmartTileSupplyChangeTransaction(timeStamp, sender);
	}

	@Override
	protected SmartTileSupplyChangeModelToDbModelMapping createMapping(final IMapper mapper) {
		return new SmartTileSupplyChangeModelToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final Account signer = Utils.generateRandomAccount();
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final SmartTileSupplyChangeModelToDbModelMapping mapping = new SmartTileSupplyChangeModelToDbModelMapping(this.mapper);

		public SmartTileSupplyChangeTransaction createModel() {
			return new SmartTileSupplyChangeTransaction(
					TimeInstant.ZERO,
					this.signer,
					new MosaicId(new NamespaceId("alice.food"), "apples"),
					SmartTileSupplyType.CreateSmartTiles,
					Quantity.fromValue(123));
		}
	}
}
