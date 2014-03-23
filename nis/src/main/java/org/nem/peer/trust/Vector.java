package org.nem.peer.trust;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;

/**
 * Represents a linear algebra vector.
 */
public class Vector {

    final int size;
    final double[] vector;

    /**
     * Creates a new vector of the specified size.
     *
     * @param size The desired size.
     */
    public Vector(final int size) {
        this.size = size;
        this.vector = new double[this.size];
    }

    /**
     * Gets the size of the vector.
     *
     * @return The size of the vector.
     */
    public int getSize() { return this.size; }

    /**
     * Gets the value at the specified index.
     *
     * @param index The index.
     * @return The value.
     */
    public double getAt(final int index) {
        return this.vector[index];
    }

    /**
     * Sets a value at the specified index.
     *
     * @param index The index.
     * @param val The value.
     */
    public void setAt(final int index, double val) {
        this.vector[index] = val;
    }

    /**
     * Sets all the vector's elements to the specified value.
     *
     * @param val The value.
     */
    public void setAll(double val) {
        for (int i = 0; i < this.vector.length; ++i)
            this.vector[i] = val;
    }

    /**
     * Gets the sum of all the vector's elements.
     *
     * @return The sum of all the vectors elements.
     */
    public double sum() {
        double sum = 0.0;
        for (double value : this.vector)
            sum += value;

        return sum;
    }

    /**
     * Gets the sum of the absolute value of all the vector's elements.
     *
     * @return The sum of the absolute value of all the vector's elements.
     */
    public double absSum() {
        double sum = 0.0;
        for (double value : this.vector)
            sum += Math.abs(value);

        return sum;
    }

    /**
     * Normalizes this vector's elements so that the absolute value of all
     * elements sums to 1.0.
     */
    public void normalize() {
        double sum = this.absSum();
        if (0.0 == sum)
            return;

        for (int i = 0; i < this.size; ++i)
            this.vector[i] /= sum;
    }

    /**
     * Creates a new Vector by adding the specified vector to this vector.
     *
     * @param vector The specified vector.
     * @return The new vector.
     */
    public Vector add(final Vector vector) {
        if (this.size != vector.size)
            throw new InvalidParameterException("cannot add vectors with different sizes");

        final Vector result = new Vector(this.size);
        for (int i = 0; i < this.size; ++i)
            result.vector[i] = this.vector[i] + vector.vector[i];

        return result;
    }

    /**
     * Creates a new Vector by multiplying this vector by a scalar.
     *
     * @param scalar The scalar.
     * @return The new vector.
     */
    public Vector multiply(final double scalar) {
        final Vector result = new Vector(this.size);
        for (int i = 0; i < this.size; ++i)
            result.vector[i] = this.vector[i] * scalar;

        return result;
    }

    /**
     * Creates a new Vector by multiplying this vector by a matrix.
     *
     * @param matrix The matrix.
     * @return The new vector.
     */
    public Vector multiply(final Matrix matrix) {
        final int columnCount = matrix.getColumnCount();
        if (this.size != columnCount)
            throw new InvalidParameterException("vector size and matrix column count must be equal");

        final int rowCount = matrix.getRowCount();
        final Vector result = new Vector(rowCount);
        for (int i = 0; i < rowCount; ++i) {
            double sumProduct = 0.0;
            for (int j = 0; j < columnCount; ++j)
                sumProduct += matrix.getAt(i, j) * this.vector[j];

            result.vector[i] = sumProduct;
        }

        return result;
    }

    /**
     * Calculates the Euclidean distance between the specified vector and this vector.
     *
     * @param vector The specified vector.
     * @return The Euclidean distance.
     */
    public double distance(final Vector vector) {
        if (this.size != vector.size)
            throw new InvalidParameterException("cannot determine the distance between vectors with different sizes");

        double distance = 0;
        for (int i = 0; i < this.size; ++i) {
            double difference = this.vector[i] - vector.vector[i];
            distance += difference * difference;
        }

        return Math.sqrt(distance);
    }

    @Override
    public String toString() {
        final DecimalFormat format = new DecimalFormat("#0.000");
        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < this.size; ++i) {
            if (0 != i)
                builder.append(" ");

            builder.append(format.format(this.vector[i]));
        }

        return builder.toString();
    }
}