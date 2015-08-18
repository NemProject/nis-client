package org.nem.nis.sync;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.cache.*;
import org.nem.nis.mappers.NisMapperFactory;
import org.nem.nis.state.ReadOnlyAccountState;
import org.nem.nis.test.BlockChain.*;
import org.nem.nis.test.MapperUtils;

import java.util.*;
import java.util.stream.Collectors;

public class BlockChainServicesTest {

	//region createMapper

	@Test
	public void createMapperDelegatesToMapperFactory() {
		// Arrange:
		// note: createMapper() only uses the NisMapperFactory object
		final NisMapperFactory factory = Mockito.mock(NisMapperFactory.class);
		final AccountLookup lookup = new DefaultAccountCache();
		final BlockChainServices services = new BlockChainServices(null, null, null, null, factory);

		// Act:
		services.createMapper(lookup);

		// Assert:
		Mockito.verify(factory, Mockito.only()).createDbModelToModelNisMapper(lookup);
	}

	//endregion

	//region isPeerChainValid

	@Test
	public void isPeerChainValidReturnsTrueIfPeerChainIsValid() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<Block> blocks = context.createPeerChain(5);

		// Act:
		final boolean isValid = context.getBlockChainServices().isPeerChainValid(
				context.getNisCacheCopy(),
				context.getLastBlock(),
				blocks);

		// Assert:
		Assert.assertThat(isValid, IsEqual.equalTo(true));
	}

	@Test
	public void isPeerChainValidReturnsFalseIfPeerChainIsInvalid() {
		// Arrange: change the peer data without changing the peer signature so that the peer chain is not verifiable
		final TestContext context = new TestContext();
		final List<Block> blocks = context.createPeerChain(5);
		blocks.get(0).getTransactions().get(0).setFee(Amount.fromNem(1234));

		// Act:
		final boolean isValid = context.getBlockChainServices().isPeerChainValid(
				context.getNisCacheCopy(),
				context.getLastBlock(),
				blocks);

		// Assert:
		Assert.assertThat(isValid, IsEqual.equalTo(false));
	}

	@Test
	public void isPeerChainValidSetsBlockDifficulties() {
		// Arrange: remember difficulties, then reset them
		final TestContext context = new TestContext();
		final List<Block> blocks = context.createPeerChain(5);
		final Collection<BlockDifficulty> expectedDifficulties = blocks.stream().map(Block::getDifficulty).collect(Collectors.toList());
		blocks.forEach(b -> b.setDifficulty(new BlockDifficulty(0)));

		// Act:
		context.getBlockChainServices().isPeerChainValid(context.getNisCacheCopy(), context.getLastBlock(),	blocks);
		final Collection<BlockDifficulty> difficulties = blocks.stream().map(Block::getDifficulty).collect(Collectors.toList());

		// Assert:
		Assert.assertThat(difficulties, IsEqual.equalTo(expectedDifficulties));
	}

	//endregion

	//region undoAndGetScore

	@Test
	public void undoAndGetScoreReturnsExpectedBlockChainScore() {
		// Arrange:
		// - the peer chain has empty blocks because if the blocks in the chain are undone, transaction.undo() would throw
		//   the reason is that processing the chain is done manually and not via block.execute(). Weighted balances are not updated correctly
		//   causing problems when trying to undo.
		final TestContext context = new TestContext();
		final BlockChainScore initialScore = context.getBlockChainScore();
		final BlockHeight height = context.getChainHeight();
		context.processPeerChain(context.createPeerChain(0));
		final BlockChainScore peerScore = context.getBlockChainScore();
		final BlockLookup blockLookup = context.createBlockLookup();

		// sanity check
		Assert.assertThat(peerScore, IsNot.not(IsEqual.equalTo(initialScore)));

		// Act:
		final BlockChainScore score = context.getBlockChainServices().undoAndGetScore(context.getNisCacheCopy(), blockLookup, height);

		// Assert:
		Assert.assertThat(score, IsEqual.equalTo(peerScore.subtract(initialScore)));
	}

	@Test
	public void undoAndGetScoreUnwindsWeightedBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockHeight height = context.getChainHeight();
		final BlockLookup blockLookup = context.createBlockLookup();

		// - save the existing weighed balances
		final Map<Address, Integer> addressToWeightedBalancesSize = context.getNisCacheCopy().getAccountStateCache().contents().stream()
				.collect(Collectors.toMap(ReadOnlyAccountState::getAddress, a -> a.getWeightedBalances().size()));

		// - add a bunch of blocks that change the number of weighted balances
		context.processPeerChain(context.createPeerChain(1500, 0));
		final NisCache copy = context.getNisCacheCopy();

		// sanity check
		copy.getAccountStateCache().contents().stream()
				.filter(a -> !context.getNemesisAccount().getAddress().equals(a.getAddress()))
				.forEach(a -> Assert.assertThat(
						a.getWeightedBalances().size(),
						IsNot.not(IsEqual.equalTo(addressToWeightedBalancesSize.get(a.getAddress())))));

		// Act:
		context.getBlockChainServices().undoAndGetScore(copy, blockLookup, height);
		copy.commit();

		// Assert:
		copy.getAccountStateCache().contents().stream()
				.forEach(a -> Assert.assertThat(a.getWeightedBalances().size(), IsEqual.equalTo(addressToWeightedBalancesSize.get(a.getAddress()))));
	}

	//endregion

	private class TestContext {
		private final TestOptions options = new TestOptions(10, 1, 10);
		private final BlockChainContext blockChainContext = new BlockChainContext(this.options);
		private final NodeContext nodeContext = this.blockChainContext.getNodeContexts().get(0);

		private List<Block> createPeerChain(final int transactionsPerBlock) {
			return this.createPeerChain(10, transactionsPerBlock);
		}

		private List<Block> createPeerChain(final int size, final int transactionsPerBlock) {
			return this.blockChainContext.newChainPart(this.nodeContext.getChain(), size, transactionsPerBlock);
		}

		private void processPeerChain(final List<Block> peerChain) {
			this.nodeContext.processChain(peerChain);
		}

		private BlockChainServices getBlockChainServices() {
			return this.nodeContext.getBlockChainServices();
		}

		private Block getLastBlock() {
			return this.nodeContext.getLastBlock();
		}

		private NisCache getNisCacheCopy() {
			return this.nodeContext.getNisCache().copy();
		}

		private BlockChainScore getBlockChainScore() {
			return this.nodeContext.getBlockChainUpdater().getScore();
		}

		private BlockHeight getChainHeight() {
			return this.nodeContext.getBlockChain().getHeight();
		}

		private BlockLookup createBlockLookup() {
			return new LocalBlockLookupAdapter(
					this.nodeContext.getMockBlockDao(),
					MapperUtils.createDbModelToModelNisMapper(this.nodeContext.getNisCache().getAccountCache()),
					this.nodeContext.getBlockChainLastBlockLayer().getLastDbBlock(),
					this.getBlockChainScore(),
					0);
		}

		private Account getNemesisAccount() {
			return this.nodeContext.getChain().get(0).getSigner();
		}
	}
}
