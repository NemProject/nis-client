package org.nem.nis.secret;

import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;

public class MultisigLinksTest {
	//region MultisigLinks
	@Test
	public void emptyMultisigLinksIsNeitherCosignatoryNorMultisig() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		Assert.assertFalse(context.multisigLinks.isCosignatory());
		Assert.assertFalse(context.multisigLinks.isMultisig());
	}

	@Test
	public void addingCosignatoryMakesMultisig() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.addCosignatory(context.address);

		// Assert:
		Assert.assertFalse(context.multisigLinks.isCosignatory());
		Assert.assertTrue(context.multisigLinks.isMultisig());
	}

	@Test
	public void addingToAccountMakesCosignatory() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.addMultisig(context.address);

		// Assert:
		Assert.assertTrue(context.multisigLinks.isCosignatoryOf(context.address));
		Assert.assertTrue(context.multisigLinks.isCosignatory());
		Assert.assertFalse(context.multisigLinks.isMultisig());
	}

	@Test
	public void addingBothMakesMultisigAndCosignatory() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.addMultisig(context.address);
		context.addCosignatory(context.address);

		// Assert:
		Assert.assertTrue(context.multisigLinks.isCosignatoryOf(context.address));
		Assert.assertTrue(context.multisigLinks.isCosignatory());
		Assert.assertTrue(context.multisigLinks.isMultisig());
	}
	//endregion

	//region removal
	@Test
	public void canRemoveCosignatory() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatory(context.address);

		// Act:
		context.removeCosignatory(context.address);

		// Assert:
		Assert.assertFalse(context.multisigLinks.isCosignatory());
		Assert.assertFalse(context.multisigLinks.isMultisig());
	}

	@Test
	public void canRemoveMultisig() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMultisig(context.address);

		// Act:
		context.removeMultisig(context.address);

		// Assert:
		Assert.assertFalse(context.multisigLinks.isCosignatory());
		Assert.assertFalse(context.multisigLinks.isMultisig());
	}
	//endregion

	//region
	@Test
	public void copyCopiesMultisig() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatory(context.address);

		// Act:
		final MultisigLinks multisigLinks = context.makeCopy();

		// Assert:
		Assert.assertFalse(multisigLinks.isCosignatory());
		Assert.assertTrue(multisigLinks.isMultisig());
	}

	@Test
	public void copyCopiesCosignatory() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMultisig(context.address);

		// Act:
		final MultisigLinks multisigLinks = context.makeCopy();

		// Assert:
		Assert.assertTrue(multisigLinks.isCosignatoryOf(context.address));
		Assert.assertTrue(multisigLinks.isCosignatory());
		Assert.assertFalse(multisigLinks.isMultisig());
	}
	//endregion

	private class TestContext {
		final MultisigLinks multisigLinks = new MultisigLinks();
		final Address address = Utils.generateRandomAddress();
		final BlockHeight blockHeight = new BlockHeight(1234L);

		public void addCosignatory(final Address address) {
			multisigLinks.addCosignatory(address, blockHeight);
		}

		public void removeCosignatory(final Address address) {
			multisigLinks.removeCosignatory(address, blockHeight);
		}

		public void addMultisig(final Address address) {
			multisigLinks.addMultisig(address, blockHeight);
		}

		public void removeMultisig(final Address address) {
			multisigLinks.removeMultisig(address, blockHeight);
		}

		public MultisigLinks makeCopy() {
			return multisigLinks.copy();
		}
	}
}
