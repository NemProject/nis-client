package org.nem.nis.dao.mappers;

import org.nem.core.crypto.Hash;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;

/**
 * Base class for mappings of transfer model types to transfer db model types.
 *
 * @param <TDbModel> The db model type.
 */
public abstract class AbstractTransferRawToDbModelMapping<TDbModel extends AbstractTransfer>
		implements IMapping<Object[], TDbModel> {
	protected final IMapper mapper;

	/**
	 * Creates a mapper.
	 *
	 * @param mapper The mapper.
	 */
	protected AbstractTransferRawToDbModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public TDbModel map(final Object[] source) {
		final TDbModel dbModel = this.mapImpl(source);
		final DbAccount sender = this.mapAccount(castBigIntegerToLong((BigInteger)source[7]));

		dbModel.setId(castBigIntegerToLong((BigInteger)source[1]));
		dbModel.setTransferHash(new Hash((byte[])source[2]));
		dbModel.setVersion((Integer)source[3]);
		dbModel.setFee(castBigIntegerToLong((BigInteger)source[4]));
		dbModel.setTimeStamp((Integer)source[5]);
		dbModel.setDeadline((Integer)source[6]);
		dbModel.setSender(sender);
		dbModel.setSenderProof((byte[])source[8]);

		return dbModel;
	}

	/**
	 * Function overridden by derived classes to preform custom derived-mapping logic.
	 *
	 * @param source The source object.
	 * @return The target object.
	 */
	protected abstract TDbModel mapImpl(final Object[] source);

	/**
	 * Maps an account id to a db model account.
	 *
	 * @param id The account id.
	 * @return The db model account.
	 */
	protected DbAccount mapAccount(final Long id) {
		return this.mapper.map(id, DbAccount.class);
	}

	/**
	 * Maps a block id to a db block.
	 *
	 * @param id The block id.
	 * @return The db block.
	 */
	protected DbBlock mapBlock(final Long id) {
		final DbBlock dbBlock = new DbBlock();
		dbBlock.setId(id);
		return dbBlock;
	}

	protected Long castBigIntegerToLong(final BigInteger value) {
		return null == value ? null : value.longValue();
	}
}
