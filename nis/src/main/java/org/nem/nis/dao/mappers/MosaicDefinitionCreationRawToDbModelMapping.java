package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.IMapper;

import java.util.Arrays;

/**
 * A mapping that is able to map raw mosaic definition creation transaction data to a db mosaic definition creation transaction.
 */
public class MosaicDefinitionCreationRawToDbModelMapping extends AbstractTransferRawToDbModelMapping<DbMosaicDefinitionCreationTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MosaicDefinitionCreationRawToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	public DbMosaicDefinitionCreationTransaction mapImpl(final Object[] source) {
		final DbMosaicDefinition dbMosaicDefinition = this.mapper.map(Arrays.copyOfRange(source, 12, source.length), DbMosaicDefinition.class);
		final DbMosaicDefinitionCreationTransaction dbTransaction = new DbMosaicDefinitionCreationTransaction();
		dbTransaction.setBlock(RawMapperUtils.mapBlock(source[0]));
		dbTransaction.setBlkIndex((Integer)source[10]);
		dbTransaction.setReferencedTransaction(RawMapperUtils.castToLong(source[11]));
		dbTransaction.setMosaicDefinition(dbMosaicDefinition);
		return dbTransaction;
	}
}
