package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.DbSmartTileSupplyChangeTransaction;

/**
 * A mapping that is able to map a db smart tile supply change transaction to a model smart tile supply change transaction.
 */
public class SmartTileSupplyChangeDbModelToModelMapping extends AbstractTransferDbModelToModelMapping<DbSmartTileSupplyChangeTransaction, SmartTileSupplyChangeTransaction> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public SmartTileSupplyChangeDbModelToModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	protected SmartTileSupplyChangeTransaction mapImpl(final DbSmartTileSupplyChangeTransaction source) {
		final Account sender = this.mapper.map(source.getSender(), Account.class);
		final MosaicId mosaicId = new MosaicId(new NamespaceId(source.getNamespaceId()), source.getMosaicName());

		return new SmartTileSupplyChangeTransaction(
				new TimeInstant(source.getTimeStamp()),
				sender,
				mosaicId,
				SmartTileSupplyType.fromValueOrDefault(source.getSupplyType()),
				Quantity.fromValue(source.getQuantity()));
	}
}
