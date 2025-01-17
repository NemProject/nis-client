package org.nem.core.crypto.ed25519.arithmetic;

import java.math.BigInteger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class Ed25519FieldTest {

	@Test
	public void pIsAsExpected() {
		// Arrange:
		final BigInteger P = BigInteger.ONE.shiftLeft(255).subtract(new BigInteger("19"));

		// Assert:
		MatcherAssert.assertThat(P, IsEqual.equalTo(Ed25519Field.P));
	}

	@Test
	public void zeroIsAsExpected() {
		// Assert:
		MatcherAssert.assertThat(BigInteger.ZERO, IsEqual.equalTo(MathUtils.toBigInteger(Ed25519Field.ZERO)));
	}

	@Test
	public void oneIsAsExpected() {
		// Assert:
		MatcherAssert.assertThat(BigInteger.ONE, IsEqual.equalTo(MathUtils.toBigInteger(Ed25519Field.ONE)));
	}

	@Test
	public void twoIsAsExpected() {
		// Assert:
		MatcherAssert.assertThat(new BigInteger("2"), IsEqual.equalTo(MathUtils.toBigInteger(Ed25519Field.TWO)));
	}

	@Test
	public void dIsAsExpected() {
		// Arrange:
		final BigInteger D = new BigInteger("37095705934669439343138083508754565189542113879843219016388785533085940283555");

		// Assert:
		MatcherAssert.assertThat(D, IsEqual.equalTo(MathUtils.toBigInteger(Ed25519Field.D)));
	}

	@Test
	public void dTimesTwoIsAsExpected() {
		// Arrange:
		final BigInteger DTimesTwo = new BigInteger("16295367250680780974490674513165176452449235426866156013048779062215315747161");

		// Assert:
		MatcherAssert.assertThat(DTimesTwo, IsEqual.equalTo(MathUtils.toBigInteger(Ed25519Field.D_Times_TWO)));
	}

	@Test
	public void iIsAsExpected() {
		// Arrange:
		final BigInteger I = new BigInteger("19681161376707505956807079304988542015446066515923890162744021073123829784752");

		// Assert (i^2 == -1):
		MatcherAssert.assertThat(I, IsEqual.equalTo(MathUtils.toBigInteger(Ed25519Field.I)));
		MatcherAssert.assertThat(I.multiply(I).mod(Ed25519Field.P),
				IsEqual.equalTo(BigInteger.ONE.shiftLeft(255).subtract(new BigInteger("20"))));
	}
}
