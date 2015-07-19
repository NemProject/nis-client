package org.nem.nis.state;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Quantity;

/**
 * A writable mosaic entry.
 */
public class MosaicEntry implements ReadOnlyMosaicEntry {
	private final Mosaic mosaic;
	private Quantity supply;

	/**
	 * Creates a new mosaic entry.
	 *
	 * @param mosaic The mosaic.
	 */
	public MosaicEntry(final Mosaic mosaic) {
		this(mosaic, new Quantity(mosaic.getProperties().getInitialQuantity()));
	}

	/**
	 * Creates a new mosaic entry.
	 *
	 * @param mosaic The mosaic.
	 * @param supply The quantity.
	 */
	public MosaicEntry(final Mosaic mosaic, final Quantity supply) {
		this.mosaic = mosaic;
		this.supply = Quantity.ZERO;
		this.increaseSupplyImpl(supply);
	}

	@Override
	public Mosaic getMosaic() {
		return this.mosaic;
	}

	@Override
	public Quantity getSupply() {
		return this.supply;
	}

	/**
	 * Increases the supply of the current mosaic.
	 *
	 * @param increase The increase.
	 */
	public void increaseSupply(final Quantity increase) {
		this.increaseSupplyImpl(increase);
	}

	private void increaseSupplyImpl(final Quantity increase) {
		final int divisibility = this.mosaic.getProperties().getDivisibility();
		this.supply = MosaicUtils.add(divisibility, this.supply, increase);
	}

	/**
	 * Decreases the supply of the current mosaic.
	 *
	 * @param decrease The decrease.
	 */
	public void decreaseSupply(final Quantity decrease) {
		this.supply = this.getSupply().subtract(decrease);
	}

	/**
	 * Creates a copy of this entry.
	 *
	 * @return A copy of this entry.
	 */
	public MosaicEntry copy() {
		return new MosaicEntry(this.mosaic, this.supply);
	}
}
