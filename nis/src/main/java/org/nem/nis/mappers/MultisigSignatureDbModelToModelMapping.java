package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.DbMultisigSignatureTransaction;

/**
 * A mapping that is able to map a db multisig signature transfer to a model multisig signature.
 */
public class MultisigSignatureDbModelToModelMapping extends AbstractTransferDbModelToModelMapping<DbMultisigSignatureTransaction, MultisigSignatureTransaction> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigSignatureDbModelToModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public MultisigSignatureTransaction mapImpl(final DbMultisigSignatureTransaction source) {
		final Account sender = this.mapper.map(source.getSender(), Account.class);
		return new MultisigSignatureTransaction(
				new TimeInstant(source.getTimeStamp()),
				sender,
				DbModelUtils.getInnerTransaction(source.getMultisigTransaction()).getTransferHash());
	}
}