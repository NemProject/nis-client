package org.nem.nis.state;

import org.nem.core.model.primitive.*;

/**
 * Read-only account info.
 */
public interface ReadOnlyAccountInfo {

	/**
	 * Gets the account's balance.
	 *
	 * @return This account's balance.
	 */
	Amount getBalance();

	/**
	 * Gets number of harvested blocks.
	 *
	 * @return Number of blocks harvested by the account.
	 */
	BlockAmount getHarvestedBlocks();

	/**
	 * Gets the account's label.
	 *
	 * @return The account's label.
	 */
	String getLabel();

	/**
	 * Returns the reference count.
	 * <br>
	 * Note that this is readonly because ReferenceCount is immutable.
	 *
	 * @return The reference count.
	 */
	ReferenceCount getReferenceCount();
}