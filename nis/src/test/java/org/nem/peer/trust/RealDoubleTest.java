package org.nem.peer.trust;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class RealDoubleTest {

	@Test
	public void canBeCreatedWithInitialValue() {
		// Arrange:
		final RealDouble value = new RealDouble(1.2);

		// Assert:
		Assert.assertThat(value.get(), IsEqual.equalTo(1.2));
	}

	@Test
	public void initialValueIsConstrained() {
		// Arrange:
		final RealDouble value = new RealDouble(Double.NaN);

		// Assert:
		Assert.assertThat(value.get(), IsEqual.equalTo(0.0));
	}

	@Test
	public void canBeSetToRealValue() {
		// Arrange:
		final RealDouble value = new RealDouble(1.2);

		// Act:
		value.set(-3.4);

		// Assert:
		Assert.assertThat(value.get(), IsEqual.equalTo(-3.4));
	}

	@Test
	public void cannotBeSetToNegativeInfinity() {
		// Assert:
		assertInvalidValue(Double.NEGATIVE_INFINITY);
	}

	@Test
	public void cannotBeSetToPositiveInfinity() {
		// Assert:
		assertInvalidValue(Double.POSITIVE_INFINITY);
	}

	@Test
	public void cannotBeSetToNaN() {
		// Assert:
		assertInvalidValue(Double.NaN);
	}

	private static void assertInvalidValue(double invalidValue) {
		// Arrange:
		final RealDouble value = new RealDouble(1.2);

		// Act:
		value.set(invalidValue);

		// Assert:
		Assert.assertThat(value.get(), IsEqual.equalTo(0.0));
	}
}
