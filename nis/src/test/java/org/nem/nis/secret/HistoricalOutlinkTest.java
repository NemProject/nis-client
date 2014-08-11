package org.nem.nis.secret;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;

import java.util.NoSuchElementException;

public class HistoricalOutlinkTest {
	@Test
	public void historicalOutlinkCanBeConstructed() {
		// Arrange:
		final HistoricalOutlink historicalOutlink = new HistoricalOutlink(new BlockHeight(1234));

		// Assert:
		Assert.assertThat(historicalOutlink.getHeight(), IsEqual.equalTo(new BlockHeight(1234)));
		Assert.assertThat(historicalOutlink.size(), IsEqual.equalTo(0));
	}

	//region add
	@Test
	public void canAddOutlinkToHistoricalOutlink() {
		// Arrange:
		final BlockHeight blockHeight = new BlockHeight(1234);
		final HistoricalOutlink historicalOutlink = new HistoricalOutlink(blockHeight);

		// Act:
		historicalOutlink.add(new AccountLink(blockHeight, Amount.fromNem(789), Utils.generateRandomAddress()));

		// Assert:
		Assert.assertThat(historicalOutlink.getHeight(), IsEqual.equalTo(blockHeight));
		Assert.assertThat(historicalOutlink.size(), IsEqual.equalTo(1));
	}

	@Test
	public void canAddOutlinksToHistoricalOutlink() {
		// Arrange:
		final BlockHeight blockHeight = new BlockHeight(1234);
		final HistoricalOutlink historicalOutlink = new HistoricalOutlink(blockHeight);

		// Act:
		historicalOutlink.add(new AccountLink(blockHeight, Amount.fromNem(789), Utils.generateRandomAddress()));
		historicalOutlink.add(new AccountLink(blockHeight, Amount.fromNem(456), Utils.generateRandomAddress()));

		// Assert:
		Assert.assertThat(historicalOutlink.getHeight(), IsEqual.equalTo(blockHeight));
		Assert.assertThat(historicalOutlink.size(), IsEqual.equalTo(2));
	}
	//endregion

	//region remove
	@Test
	public void canRemoveAddedOutlinksToHistoricalOutlink() {
		// Arrange:
		final BlockHeight blockHeight = new BlockHeight(1234);
		final HistoricalOutlink historicalOutlink = new HistoricalOutlink(blockHeight);
		final AccountLink accountLink1 = new AccountLink(blockHeight, Amount.fromNem(789), Utils.generateRandomAddress());
		final AccountLink accountLink2 = new AccountLink(blockHeight, Amount.fromNem(456), Utils.generateRandomAddress());

		// Act:
		historicalOutlink.add(accountLink1);
		historicalOutlink.add(accountLink2);
		historicalOutlink.remove(accountLink2);
		historicalOutlink.remove(accountLink1);

		// Assert:
		Assert.assertThat(historicalOutlink.getHeight(), IsEqual.equalTo(blockHeight));
		Assert.assertThat(historicalOutlink.size(), IsEqual.equalTo(0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void removingOutlinksInWrongOrderThrowsException() {
		// Arrange:
		final BlockHeight blockHeight = new BlockHeight(1234);
		final HistoricalOutlink historicalOutlink = new HistoricalOutlink(blockHeight);
		final AccountLink accountLink1 = new AccountLink(blockHeight, Amount.fromNem(789), Utils.generateRandomAddress());
		final AccountLink accountLink2 = new AccountLink(blockHeight, Amount.fromNem(456), Utils.generateRandomAddress());

		// Act:
		historicalOutlink.add(accountLink1);
		historicalOutlink.add(accountLink2);
		historicalOutlink.remove(accountLink1);
		historicalOutlink.remove(accountLink2);

		// Assert:
		Assert.assertThat(historicalOutlink.getHeight(), IsEqual.equalTo(blockHeight));
		Assert.assertThat(historicalOutlink.size(), IsEqual.equalTo(0));
	}

	@Test(expected = NoSuchElementException.class)
	public void removingFromEmptyThrowsException() {
		// Arrange:
		final BlockHeight blockHeight = new BlockHeight(1234);
		final HistoricalOutlink historicalOutlink = new HistoricalOutlink(blockHeight);
		final AccountLink accountLink1 = new AccountLink(blockHeight, Amount.fromNem(789), Utils.generateRandomAddress());

		// Act:
		historicalOutlink.remove(accountLink1);

		// Assert:
		Assert.assertThat(historicalOutlink.getHeight(), IsEqual.equalTo(blockHeight));
		Assert.assertThat(historicalOutlink.size(), IsEqual.equalTo(0));
	}
	//endregion

	//region copy

	@Test
	public void copyCopiesHeightAndOutlinks() {
		// Arrange:
		final BlockHeight blockHeight = new BlockHeight(1234);
		final HistoricalOutlink historicalOutlink = new HistoricalOutlink(blockHeight);
		historicalOutlink.add(new AccountLink(blockHeight, Amount.fromNem(789), Utils.generateRandomAddress()));
		historicalOutlink.add(new AccountLink(blockHeight, Amount.fromNem(456), Utils.generateRandomAddress()));

		// Act:
		final HistoricalOutlink copy = historicalOutlink.copy();

		// Assert:
		Assert.assertThat(copy.getHeight(), IsEqual.equalTo(new BlockHeight(1234)));
		Assert.assertThat(copy.size(), IsEqual.equalTo(2));
		Assert.assertThat(copy.getOutlinks().get(0), IsSame.sameInstance(historicalOutlink.getOutlinks().get(0)));
		Assert.assertThat(copy.getOutlinks().get(1), IsSame.sameInstance(historicalOutlink.getOutlinks().get(1)));
	}

	@Test
	public void copyCreatesDeepCopyOfOutlinksList() {
		// Arrange:
		final BlockHeight blockHeight = new BlockHeight(1234);
		final HistoricalOutlink historicalOutlink = new HistoricalOutlink(blockHeight);
		historicalOutlink.add(new AccountLink(blockHeight, Amount.fromNem(789), Utils.generateRandomAddress()));
		historicalOutlink.add(new AccountLink(blockHeight, Amount.fromNem(456), Utils.generateRandomAddress()));

		// Act:
		final HistoricalOutlink copy = historicalOutlink.copy();
		copy.add(new AccountLink(blockHeight, Amount.fromNem(111), Utils.generateRandomAddress()));

		// Assert:
		Assert.assertThat(historicalOutlink.size(), IsEqual.equalTo(2));
		Assert.assertThat(copy.size(), IsEqual.equalTo(3));
	}

	//endregion
}