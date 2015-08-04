package org.nem.nis.dao;

import org.nem.core.model.Account;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.nis.dbmodel.DbMosaicDefinition;

import java.util.Collection;

/**
 * Read-only DAO for accessing DbMosaicDefinition objects.
 */
public interface ReadOnlyMosaicDefinitionDao {
	/**
	 * Gets all mosaic definitions for the specified account, optionally confined to a specified namespace.
	 * The search is limited by a given max id and returns at most limit mosaic definitions.
	 *
	 * @param account The account.
	 * @param namespaceId The (optional) namespace id.
	 * @param maxId The id of "top-most" mosaic definition.
	 * @param limit The limit.
	 * @return The collection of db mosaic definitions.
	 */
	Collection<DbMosaicDefinition> getMosaicDefinitionsForAccount(
			final Account account,
			final NamespaceId namespaceId,
			final Long maxId,
			final int limit);

	/**
	 * Gets all mosaic definitions for the specified namespace id.
	 * The search is limited by a given max id and returns at most limit mosaic definitions.
	 *
	 * @param namespaceId The namespace id.
	 * @param maxId The id of "top-most" mosaic definition.
	 * @param limit The limit.
	 * @return The collection of db mosaic definitions.
	 */
	Collection<DbMosaicDefinition> getMosaicDefinitionsForNamespace(
			final NamespaceId namespaceId,
			final Long maxId,
			final int limit);

	/**
	 * Gets all mosaic definitions.
	 * The search is limited by a given max id and returns at most limit mosaic definitions.
	 *
	 * @param maxId The id of "top-most" mosaic definition.
	 * @param limit The limit.
	 * @return The collection of db mosaic definitions.
	 */
	Collection<DbMosaicDefinition> getMosaicDefinitions(
			final Long maxId,
			final int limit);
}
