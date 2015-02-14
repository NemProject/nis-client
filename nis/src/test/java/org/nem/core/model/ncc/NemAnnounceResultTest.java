package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.crypto.Hash;
import org.nem.core.model.ValidationResult;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

@RunWith(Enclosed.class)
public class NemAnnounceResultTest {

	private abstract static class NemAnnounceResultTestBase {

		@Test
		public void canCreateResult() {
			// Act:
			final NemAnnounceResult result = this.createResult(ValidationResult.FAILURE_CHAIN_INVALID);

			// Assert:
			assertNemRequestResult(result);
			Assert.assertThat(result.getInnerTransactionHash(), IsEqual.equalTo(this.getExpectedHash()));
		}

		@Test
		public void canRoundtripResult() {
			// Act:
			final Deserializer deserializer = Utils.roundtripSerializableEntity(
					this.createResult(ValidationResult.FAILURE_CHAIN_INVALID),
					null);
			final NemAnnounceResult result = new NemAnnounceResult(deserializer);

			// Assert:
			assertNemRequestResult(result);
			Assert.assertThat(result.getInnerTransactionHash(), IsEqual.equalTo(this.getExpectedHash()));
		}

		@Test
		public void canRoundtripResultAsNemRequestResult() {
			// Act:
			final Deserializer deserializer = Utils.roundtripSerializableEntity(
					this.createResult(ValidationResult.FAILURE_CHAIN_INVALID),
					null);
			final NemRequestResult result = new NemRequestResult(deserializer);

			// Assert:
			assertNemRequestResult(result);
		}

		private static void assertNemRequestResult(final NemRequestResult result) {
			Assert.assertThat(result.getType(), IsEqual.equalTo(NemRequestResult.TYPE_VALIDATION_RESULT));
			Assert.assertThat(result.getCode(), IsEqual.equalTo(ValidationResult.FAILURE_CHAIN_INVALID.getValue()));
			Assert.assertThat(result.getMessage(), IsEqual.equalTo("FAILURE_CHAIN_INVALID"));
		}

		protected abstract NemAnnounceResult createResult(final ValidationResult result);

		protected abstract Hash getExpectedHash();
	}

	public static class WithoutInnerTransactionHashTest extends NemAnnounceResultTestBase {

		@Override
		protected NemAnnounceResult createResult(final ValidationResult result) {
			return new NemAnnounceResult(result);
		}

		@Override
		protected Hash getExpectedHash() {
			return null;
		}
	}

	public static class WithInnerTransactionHashTest extends NemAnnounceResultTestBase {
		private static final Hash DEFAULT_HASH = Utils.generateRandomHash();

		protected NemAnnounceResult createResult(final ValidationResult result) {
			return new NemAnnounceResult(result, DEFAULT_HASH);
		}

		@Override
		protected Hash getExpectedHash() {
			return DEFAULT_HASH;
		}
	}
}