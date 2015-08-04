package org.nem.nis.dao;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.TransactionTypes;
import org.nem.nis.dbmodel.*;

public class MultisigTransferMapTest {

	@Test
	public void mapHasExactlyMultisigEmbeddableTypeEntries() {
		// Act:
		final MultisigTransferMap map = new MultisigTransferMap();

		// Assert:
		Assert.assertThat(map.size(), IsEqual.equalTo(TransactionTypes.getMultisigEmbeddableTypes().size()));
	}

	@Test
	public void mapHasEntryForEachMultisigEmbeddableType() {
		// Arrange:
		final MultisigTransferMap map = new MultisigTransferMap();

		for (final int type : TransactionTypes.getMultisigEmbeddableTypes()) {
			// Act:
			final MultisigTransferMap.Entry entry = map.getEntry(type);

			// Assert:
			Assert.assertThat(entry, IsNull.notNullValue());
			Assert.assertThat(entry.getType(), IsEqual.equalTo(type));
		}
	}

	@Test
	public void entryAddAddsTransferToEntry() {
		// Arrange:
		final MultisigTransferMap map = new MultisigTransferMap();
		final MultisigTransferMap.Entry entry = map.getEntry(TransactionTypes.TRANSFER);
		final DbTransferTransaction transfer = new DbTransferTransaction();
		transfer.setId(1234L);

		// Act:
		entry.add(transfer);
		final AbstractBlockTransfer result = entry.getOrDefault(1234L);

		// Assert:
		Assert.assertThat(result, IsSame.sameInstance(transfer));
	}

	@Test
	public void entryAddAddsTransferToEntryButNotOtherEntries() {
		// Arrange:
		final MultisigTransferMap map = new MultisigTransferMap();
		final DbTransferTransaction transfer = new DbTransferTransaction();
		transfer.setId(1234L);
		map.getEntry(TransactionTypes.TRANSFER).add(transfer);

		for (final int type : TransactionTypes.getMultisigEmbeddableTypes()) {
			// Act:
			final MultisigTransferMap.Entry entry = map.getEntry(type);
			final AbstractBlockTransfer result = entry.getOrDefault(1234L);

			// Assert:
			if (TransactionTypes.TRANSFER == type) {
				Assert.assertThat(result, IsSame.sameInstance(transfer));
			} else {
				Assert.assertThat(result, IsNull.nullValue());
			}
		}
	}

	@Test
	public void entryGetOrDefaultReturnsNullIfIdIsNull() {
		// Arrange:
		final MultisigTransferMap map = new MultisigTransferMap();
		final MultisigTransferMap.Entry entry = map.getEntry(TransactionTypes.TRANSFER);

		// Act:
		final AbstractBlockTransfer result = entry.getOrDefault(null);

		// Assert:
		Assert.assertThat(result, IsNull.nullValue());
	}

	@Test
	public void entryGetOrDefaultReturnsNullIfIdIsUnknown() {
		// Arrange:
		final MultisigTransferMap map = new MultisigTransferMap();
		final MultisigTransferMap.Entry entry = map.getEntry(TransactionTypes.TRANSFER);

		// Act:
		final AbstractBlockTransfer result = entry.getOrDefault(1234L);

		// Assert:
		Assert.assertThat(result, IsNull.nullValue());
	}
}