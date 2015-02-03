package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.function.*;

/**
 * Contains transaction mapping metadata.
 */
public class TransactionRegistry {

	/**
	 * A registry entry.
	 */
	public static class Entry<TDbModel extends AbstractBlockTransfer, TModel extends Transaction> {

		/**
		 * The transaction type.
		 */
		public final int type;

		/**
		 * A function that will return db model transactions given a block
		 */
		public final Function<DbBlock, List<TDbModel>> getFromBlock;

		/**
		 * A function that will return set db model transactions given a block
		 */
		public final BiConsumer<DbBlock, List<TDbModel>> setInBlock;

		/**
		 * A function that will get db model transactions given a multisig transfer.
		 */
		public final Function<DbMultisigTransaction, TDbModel> getFromMultisig;

		/**
		 * A function that will get the recipient (if any) given an abstract block transfer.
		 */
		public final Function<TDbModel, DbAccount> getRecipient;

		/**
		 * A function that will get a list of db accounts (if any) given an abstract block transfer.
		 */
		public final Function<TDbModel, Collection<DbAccount>> getOtherAccounts;

		/**
		 * The db model transaction class.
		 */
		public final Class<TDbModel> dbModelClass;

		/**
		 * The model transaction class.
		 */
		public final Class<TModel> modelClass;

		private final Function<IMapper, IMapping<TModel, TDbModel>> createModelToDbModelMapper;
		private final Function<IMapper, IMapping<TDbModel, TModel>> createDbModelToModelMapper;

		private Entry(
				final int type,
				final Function<DbBlock, List<TDbModel>> getFromBlock,
				final BiConsumer<DbBlock, List<TDbModel>> setInBlock,
				final Function<DbMultisigTransaction, TDbModel> getFromMultisig,
				final Function<TDbModel, DbAccount> getRecipient,
				final Function<TDbModel, Collection<DbAccount>> getOtherAccounts,
				final Function<IMapper, IMapping<TModel, TDbModel>> createModelToDbModelMapper,
				final Function<IMapper, IMapping<TDbModel, TModel>> createDbModelToModelMapper,
				final Class<TDbModel> dbModelClass,
				final Class<TModel> modelClass) {
			this.type = type;

			this.getFromBlock = getFromBlock;
			this.setInBlock = setInBlock;

			this.getFromMultisig = getFromMultisig;

			this.getRecipient = getRecipient;
			this.getOtherAccounts = getOtherAccounts;

			this.createModelToDbModelMapper = createModelToDbModelMapper;
			this.createDbModelToModelMapper = createDbModelToModelMapper;
			this.dbModelClass = dbModelClass;
			this.modelClass = modelClass;
		}

		/**
		 * Adds model to db model mappers to the mapping repository.
		 *
		 * @param repository The mapping repository
		 */
		public void addModelToDbModelMappers(final MappingRepository repository) {
			repository.addMapping(this.modelClass, this.dbModelClass, this.createModelToDbModelMapper.apply(repository));
		}

		/**
		 * Adds db model to model mappers to the mapping repository.
		 *
		 * @param repository The mapping repository
		 */
		public void addDbModelToModelMappers(final MappingRepository repository) {
			repository.addMapping(this.dbModelClass, this.modelClass, this.createDbModelToModelMapper.apply(repository));
			repository.addMapping(this.dbModelClass, Transaction.class, this.createDbModelToModelMapper.apply(repository));
		}
	}

	private static final List<Entry<?, ?>> entries = new ArrayList<Entry<?, ?>>() {
		{
			this.add(new Entry<>(
					TransactionTypes.TRANSFER,
					DbBlock::getBlockTransferTransactions,
					(block, transfers) -> block.setBlockTransferTransactions(transfers),
					DbMultisigTransaction::getTransferTransaction,
					DbTransferTransaction::getRecipient,
					transfer -> new ArrayList<>(),
					TransferModelToDbModelMapping::new,
					TransferDbModelToModelMapping::new,
					DbTransferTransaction.class,
					TransferTransaction.class));

			this.add(new Entry<>(
					TransactionTypes.IMPORTANCE_TRANSFER,
					DbBlock::getBlockImportanceTransferTransactions,
					(block, transfers) -> block.setBlockImportanceTransferTransactions(transfers),
					DbMultisigTransaction::getImportanceTransferTransaction,
					DbImportanceTransferTransaction::getRemote,
					transfer -> new ArrayList<>(),
					ImportanceTransferModelToDbModelMapping::new,
					ImportanceTransferDbModelToModelMapping::new,
					DbImportanceTransferTransaction.class,
					ImportanceTransferTransaction.class));

			this.add(new Entry<>(
					TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION,
					DbBlock::getBlockMultisigAggregateModificationTransactions,
					(block, transfers) -> block.setBlockMultisigAggregateModificationTransactions(transfers),
					DbMultisigTransaction::getMultisigAggregateModificationTransaction,
					transfer -> null,
					DbMultisigAggregateModificationTransaction::getOtherAccounts,
					MultisigAggregateModificationModelToDbModelMapping::new,
					MultisigAggregateModificationDbModelToModelMapping::new,
					DbMultisigAggregateModificationTransaction.class,
					MultisigAggregateModificationTransaction.class));

			this.add(new Entry<>(
					TransactionTypes.MULTISIG,
					DbBlock::getBlockMultisigTransactions,
					(block, transfers) -> block.setBlockMultisigTransactions(transfers),
					multisig -> null,
					multisig -> null,
					DbMultisigTransaction::getOtherAccounts,
					MultisigTransactionModelToDbModelMapping::new,
					MultisigTransactionDbModelToModelMapping::new,
					DbMultisigTransaction.class,
					org.nem.core.model.MultisigTransaction.class));
		}
	};

	/**
	 * Gets the number of entries.
	 *
	 * @return The number of entries.
	 */
	public static int size() {
		return entries.size();
	}

	/**
	 * Gets the number of entries that can be embedded in a multisig transaction.
	 *
	 * @return The number of entries.
	 */
	public static int multisigEmbeddableSize() {
		return size() - 1;
	}

	/**
	 * Gets all entries.
	 *
	 * @return The entries.
	 */
	public static Iterable<Entry<?, ?>> iterate() {
		return entries;
	}

	/**
	 * Finds an entry given a transaction type.
	 *
	 * @param type The transaction type.
	 * @return The entry.
	 */
	public static Entry<?, ?> findByType(final Integer type) {
		for (final Entry<?, ?> entry : entries) {
			if (entry.type == type) {
				return entry;
			}
		}

		return null;
	}
}