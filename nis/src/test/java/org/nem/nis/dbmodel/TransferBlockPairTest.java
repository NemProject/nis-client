package org.nem.nis.dbmodel;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;

public class TransferBlockPairTest {

	@Test
	public void canCreatePair() {
		// Arrange:
		final AbstractBlockTransfer transfer = Mockito.mock(AbstractBlockTransfer.class);
		final DbBlock block = Mockito.mock(DbBlock.class);

		// Act:
		final TransferBlockPair pair = new TransferBlockPair(transfer, block);

		// Assert:
		Assert.assertThat(pair.getTransfer(), IsSame.sameInstance(transfer));
		Assert.assertThat(pair.getDbBlock(), IsSame.sameInstance(block));
	}

	@Test
	public void compareToReturnsCorrectResultWhenComparingEqualObjects() {
		// Arrange:
		final TransferBlockPair pair1 = createTransferWithId(8);
		final TransferBlockPair pair2 = createTransferWithId(8);

		// Assert:
		Assert.assertThat(pair1.compareTo(pair2), IsEqual.equalTo(0));
		Assert.assertThat(pair2.compareTo(pair1), IsEqual.equalTo(0));
	}

	@Test
	public void compareToReturnsCorrectResultWhenComparingDifferentObjects() {
		// Arrange:
		final TransferBlockPair pair1 = createTransferWithId(7);
		final TransferBlockPair pair2 = createTransferWithId(8);

		// Assert:
		Assert.assertThat(pair1.compareTo(pair2), IsEqual.equalTo(1));
		Assert.assertThat(pair2.compareTo(pair1), IsEqual.equalTo(-1));
	}

	private static TransferBlockPair createTransferWithId(final long id) {
		final DbBlock block = Mockito.mock(DbBlock.class);
		final AbstractBlockTransfer transfer = new DbTransferTransaction();
		transfer.setId(id);
		return new TransferBlockPair(transfer, block);
	}
}