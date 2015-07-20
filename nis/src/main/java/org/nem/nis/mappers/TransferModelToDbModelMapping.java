package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A mapping that is able to map a model transfer transaction to a db transfer.
 */
public class TransferModelToDbModelMapping extends AbstractTransferModelToDbModelMapping<TransferTransaction, DbTransferTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public TransferModelToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	public DbTransferTransaction mapImpl(final TransferTransaction source) {
		final DbAccount recipient = this.mapAccount(source.getRecipient());

		final DbTransferTransaction dbTransfer = new DbTransferTransaction();
		dbTransfer.setRecipient(recipient);
		dbTransfer.setAmount(source.getAmount().getNumMicroNem());
		dbTransfer.setReferencedTransaction(0L);

		final Message message = source.getMessage();
		if (null != message) {
			dbTransfer.setMessageType(message.getType());
			dbTransfer.setMessagePayload(message.getEncodedPayload());
		}

		final Set<DbSmartTile> dbSmartTiles = source.getAttachment().getMosaicTransfers().stream()
				.map(st -> this.mapper.map(st, DbSmartTile.class))
				.collect(Collectors.toSet());
		dbTransfer.setSmartTiles(dbSmartTiles);
		return dbTransfer;
	}
}