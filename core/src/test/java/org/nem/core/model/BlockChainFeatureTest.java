package org.nem.core.model;

import java.util.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class BlockChainFeatureTest {

	// region fromString

	@Test
	public void fromStringCanParseValidBlockChainFeaturesStringRepresentation() {
		// Arrange:
		@SuppressWarnings("serial")
		final Map<String, BlockChainFeature> expectedMappings = new HashMap<String, BlockChainFeature>() {
			{
				this.put("PROOF_OF_IMPORTANCE", BlockChainFeature.PROOF_OF_IMPORTANCE);
				this.put("PROOF_OF_STAKE", BlockChainFeature.PROOF_OF_STAKE);
				this.put("WB_TIME_BASED_VESTING", BlockChainFeature.WB_TIME_BASED_VESTING);
				this.put("WB_IMMEDIATE_VESTING", BlockChainFeature.WB_IMMEDIATE_VESTING);
				this.put("STABILIZE_BLOCK_TIMES", BlockChainFeature.STABILIZE_BLOCK_TIMES);
			}
		};

		// Act:
		for (final Map.Entry<String, BlockChainFeature> entry : expectedMappings.entrySet()) {
			final BlockChainFeature feature = BlockChainFeature.fromString(entry.getKey());

			// Assert:
			MatcherAssert.assertThat(feature, IsEqual.equalTo(entry.getValue()));
		}

		// Assert:
		MatcherAssert.assertThat(expectedMappings.size(), IsEqual.equalTo(BlockChainFeature.values().length));
	}

	@Test
	public void fromStringCannotParseInvalidBlockChainFeaturesStringRepresentation() {
		// Act:
		ExceptionAssert.assertThrows(v -> BlockChainFeature.fromString("BLAH"), IllegalArgumentException.class);
	}

	// endregion

	// region value / or / explode

	@Test
	public void valueReturnsUnderlyingValue() {
		// Act:
		final BlockChainFeature feature = BlockChainFeature.PROOF_OF_IMPORTANCE;

		// Assert:
		MatcherAssert.assertThat(feature.value(), IsEqual.equalTo(1));
	}

	@Test
	public void canBitwiseOrTogetherZeroFeatures() {
		// Act:
		final int value = BlockChainFeature.or();

		// Assert:
		MatcherAssert.assertThat(value, IsEqual.equalTo(0));
	}

	@Test
	public void canBitwiseOrTogetherSingleFeature() {
		// Act:
		final int value = BlockChainFeature.or(BlockChainFeature.PROOF_OF_IMPORTANCE);

		// Assert:
		MatcherAssert.assertThat(value, IsEqual.equalTo(1));
	}

	@Test
	public void canBitwiseOrTogetherMultipleFeatures() {
		// Act:
		final int value = BlockChainFeature.or(BlockChainFeature.PROOF_OF_IMPORTANCE, BlockChainFeature.STABILIZE_BLOCK_TIMES);

		// Assert:
		MatcherAssert.assertThat(value, IsEqual.equalTo(17));
	}

	@Test
	public void canExplodeZeroFeatures() {
		// Act:
		final BlockChainFeature[] features = BlockChainFeature.explode(0);

		// Assert:
		MatcherAssert.assertThat(features, IsEqual.equalTo(new BlockChainFeature[]{}));
	}

	@Test
	public void canExplodeOneFeature() {
		// Act:
		final BlockChainFeature[] features = BlockChainFeature.explode(2);

		// Assert:
		MatcherAssert.assertThat(features, IsEqual.equalTo(new BlockChainFeature[]{
				BlockChainFeature.PROOF_OF_STAKE
		}));
	}

	@Test
	public void canExplodeMultipleFeatures() {
		// Act:
		final BlockChainFeature[] features = BlockChainFeature.explode(17);

		// Assert:
		MatcherAssert.assertThat(features, IsEqual.equalTo(new BlockChainFeature[]{
				BlockChainFeature.PROOF_OF_IMPORTANCE, BlockChainFeature.STABILIZE_BLOCK_TIMES
		}));
	}

	// endregion
}
