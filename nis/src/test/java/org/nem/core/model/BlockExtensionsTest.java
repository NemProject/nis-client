package org.nem.core.model;

import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.stream.*;

public class BlockExtensionsTest {

	@Test
	public void canStreamDirectTransactions() {
		// Arrange:
		final Block block = createTestBlock();

		// Act:
		final List<Integer> customFields = getCustomFields(BlockExtensions.streamDirectTransactions(block));
		// Assert:
		Assert.assertThat(
				customFields,
				IsEquivalent.equivalentTo(50, 100, 150));
	}

	@Test
	public void canStreamDirectAndFirstChildTransactions() {
		// Arrange:
		final Block block = createTestBlock();

		// Act:
		final List<Integer> customFields = getCustomFields(BlockExtensions.streamDirectAndFirstChildTransactions(block));

		// Assert:
		Assert.assertThat(
				customFields,
				IsEquivalent.equivalentTo(50, 60, 70, 80, 100, 110, 120, 130, 150, 160, 170, 180));
	}

	@Test
	public void canStreamDefaultTransactions() {
		// Arrange:
		final Block block = createTestBlock();

		// Act:
		final List<Integer> customFields = getCustomFields(BlockExtensions.streamDefault(block));

		// Assert:
		Assert.assertThat(
				customFields,
				IsEquivalent.equivalentTo(50, 60, 70, 80, 100, 110, 120, 130, 150, 160, 170, 180));
	}

	@Test
	public void canStreamAllTransactions() {
		// Arrange:
		final Block block = createTestBlock();

		// Act:
		final List<Integer> customFields = getCustomFields(BlockExtensions.streamAllTransactions(block));

		// Assert:
		Assert.assertThat(
				customFields,
				IsEquivalent.equivalentTo(
						50, 60, 61, 62, 70, 80, 81, 82,
						100, 110, 111, 112, 120, 130, 131, 132,
						150, 160, 161, 162, 170, 180, 181, 182));
	}

	private static List<Integer> getCustomFields(final Stream<Transaction> stream) {
		return stream
				.map(t -> ((MockTransaction)t).getCustomField())
				.collect(Collectors.toList());
	}

	private static Block createTestBlock() {
		final Block block = new Block(Utils.generateRandomAccount(), Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, BlockHeight.ONE);
		block.addTransaction(createMockTransaction(50));
		block.addTransaction(createMockTransaction(100));
		block.addTransaction(createMockTransaction(150));
		return block;
	}

	private static Transaction createMockTransaction(final int seed) {
		final Transaction child1 = createMockTransactionWithTwoChildren(seed + 10);
		final Transaction child2 = new MockTransaction(Utils.generateRandomAccount(), seed + 20);
		final Transaction child3 = createMockTransactionWithTwoChildren(seed + 30);

		final MockTransaction parent = new MockTransaction(Utils.generateRandomAccount(), seed);
		parent.setChildTransactions(Arrays.asList(child1, child2, child3));
		return parent;
	}

	private static Transaction createMockTransactionWithTwoChildren(final int seed) {
		final MockTransaction parent = new MockTransaction(Utils.generateRandomAccount(), seed);
		final MockTransaction child1 = new MockTransaction(Utils.generateRandomAccount(), seed + 1);
		final MockTransaction child2 = new MockTransaction(Utils.generateRandomAccount(), seed + 2);
		parent.setChildTransactions(Arrays.asList(child1, child2));
		return parent;
	}
}