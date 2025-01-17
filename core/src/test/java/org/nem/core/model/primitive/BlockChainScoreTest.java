package org.nem.core.model.primitive;

import java.math.BigInteger;
import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

public class BlockChainScoreTest {

	// region constructor

	@Test
	public void cannotBeCreatedAroundNegativeScore() {
		// Act:
		ExceptionAssert.assertThrows(v -> new BlockChainScore(BigInteger.valueOf(-1)), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new BlockChainScore(-1), IllegalArgumentException.class);
	}

	@Test
	public void canBeCreatedAroundZeroScore() {
		// Assert:
		MatcherAssert.assertThat(new BlockChainScore(0).getRaw(), IsEqual.equalTo(BigInteger.ZERO));
		MatcherAssert.assertThat(new BlockChainScore(BigInteger.ZERO).getRaw(), IsEqual.equalTo(BigInteger.ZERO));
	}

	@Test
	public void canBeCreatedAroundPositiveScore() {
		// Assert:
		MatcherAssert.assertThat(new BlockChainScore(1).getRaw(), IsEqual.equalTo(BigInteger.ONE));
		MatcherAssert.assertThat(new BlockChainScore(BigInteger.ONE).getRaw(), IsEqual.equalTo(BigInteger.ONE));
	}

	// endregion

	// region add/subtract

	@Test
	public void scoresCanBeAdded() {
		// Arrange:
		final BlockChainScore score1 = new BlockChainScore(BigInteger.valueOf(17));
		final BlockChainScore score2 = new BlockChainScore(BigInteger.valueOf(3));

		// Act:
		final BlockChainScore result = score1.add(score2);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(new BlockChainScore(BigInteger.valueOf(20))));
	}

	@Test
	public void scoresCanBeSubtracted() {
		// Arrange:
		final BlockChainScore score1 = new BlockChainScore(BigInteger.valueOf(17));
		final BlockChainScore score2 = new BlockChainScore(BigInteger.valueOf(3));

		// Act:
		final BlockChainScore result = score1.subtract(score2);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(new BlockChainScore(BigInteger.valueOf(14))));
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotSubtractLargerScoreFromSmallerScore() {
		// Arrange:
		final BlockChainScore score1 = new BlockChainScore(BigInteger.valueOf(17));
		final BlockChainScore score2 = new BlockChainScore(BigInteger.valueOf(3));

		// Act:
		score2.subtract(score1);
	}

	// endregion

	// region serialization

	@Test
	public void scoreCanBeSerialized() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final BlockChainScore score = new BlockChainScore(BigInteger.valueOf(142));

		// Act:
		score.serialize(serializer);
		final JSONObject jsonObject = serializer.getObject();

		// Assert:
		MatcherAssert.assertThat(jsonObject.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(jsonObject.get("score"), IsEqual.equalTo("008e"));
	}

	@Test
	public void scoreCanBeRoundTripped() {
		// Act:
		final BlockChainScore score = createRoundTrippedScore(new BlockChainScore(BigInteger.valueOf(142)));

		// Assert:
		MatcherAssert.assertThat(score, IsEqual.equalTo(new BlockChainScore(BigInteger.valueOf(142))));
	}

	private static BlockChainScore createRoundTrippedScore(final BlockChainScore originalScore) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalScore, null);
		return new BlockChainScore(deserializer);
	}
}
