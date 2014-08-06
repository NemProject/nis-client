package org.nem.core.math;

import java.text.DecimalFormat;
import java.util.function.DoubleConsumer;
import org.nem.core.utils.FormatUtils;

/**
 * Represents a dense matrix.
 */
public final class DenseMatrix extends Matrix {
	// TODO-CR: is there anyway to flag members like this without access modifiers and change them to private?
	// BR: At least in eclipse I don't see how to do this automatically.
	final int numCols;
	final double[] values;

	/**
	 * Creates a new matrix of the specified size.
	 *
	 * @param rows The desired number of rows.
	 * @param cols The desired number of columns.
	 */
	public DenseMatrix(final int rows, final int cols) {
		super(rows, cols);
		this.numCols = cols;
		this.values = new double[this.getElementCount()];
	}

	/**
	 * Creates a new matrix of the specified size and values.
	 *
	 * @param rows The desired number of rows.
	 * @param cols The desired number of columns.
	 * @param values The specified values.
	 */
	public DenseMatrix(final int rows, final int cols, final double[] values) {
		super(rows, cols);

		if (values.length != this.getElementCount()) {
			throw new IllegalArgumentException("incompatible number of values");
		}

		this.numCols = cols;
		this.values = values;
	}

	/**
	 * Gets the underlying, raw array.
	 *
	 * @return The underlying, raw array.
	 */
	public double[] getRaw() {
		return this.values;
	}

	// region Matrix abstract functions

	@Override
	protected final Matrix create(final int numRows, final int numCols) {
		return new DenseMatrix(numRows, numCols);
	}

	@Override
	protected final double getAtUnchecked(final int row, final int col) {
		return this.values[row * this.numCols + col];
	}

	@Override
	protected final void setAtUnchecked(final int row, final int col, final double val) {
		this.values[row * this.numCols + col] = val;
	}

	class SetWrapper implements DoubleConsumer {
		final int i;
		final int j;

		SetWrapper(final int i, final int j) {
			this.i = i;
			this.j = j;
		}

		@Override
		public void accept(final double value) {
			DenseMatrix.this.setAtUnchecked(this.i, this.j, value);
		}
	}

	@Override
	protected final void forEach(final ElementVisitorFunction func) {
		for (int i = 0; i < this.getRowCount(); ++i) {
			for (int j = 0; j < this.getColumnCount(); ++j) {
				final int iCopy = i;
				final int jCopy = j;
				final SetWrapper setWrapper = new SetWrapper(iCopy, jCopy);
				func.visit(i, j, this.getAtUnchecked(i, j), setWrapper);
			}
		}
	}

	// endregion

	@Override
	public String toString() {
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final StringBuilder builder = new StringBuilder();

		this.forEach((r, c, v) -> {
			if (0 != r && 0 == c) {
				builder.append(System.lineSeparator());
			}

			if (0 != c) {
				builder.append(" ");
			}

			builder.append(format.format(v));
		});

		return builder.toString();
	}
}