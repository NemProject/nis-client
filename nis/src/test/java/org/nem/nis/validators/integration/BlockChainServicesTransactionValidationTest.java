package org.nem.nis.validators.integration;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.poi.GroupedHeight;
import org.nem.nis.secret.BlockTransactionObserverFactory;
import org.nem.nis.state.AccountState;
import org.nem.nis.sync.BlockChainServices;
import org.nem.nis.test.*;

import java.util.List;

public class BlockChainServicesTransactionValidationTest extends AbstractTransactionValidationTest {

	@Override
	protected void assertTransactions(
			final BlockHeight chainHeight,
			final ReadOnlyNisCache nisCache,
			final List<Transaction> all,
			final List<Transaction> expectedFiltered,
			final ValidationResult expectedResult) {
		while (true) {
			// Arrange:
			final BlockHeight parentHeight = chainHeight.prev(); // chainHeight is for block one (P -> [1] -> 2 -> 3)
			final BlockChainServices blockChainServices = new BlockChainServices(
					Mockito.mock(BlockDao.class),
					new BlockTransactionObserverFactory(),
					NisUtils.createBlockValidatorFactory(),
					NisUtils.createTransactionValidatorFactory(),
					MapperUtils.createNisMapperFactory());

			final NisCache copyCache = nisCache.copy();
			final Account blockSigner = createBlockSigner(copyCache, parentHeight);

			// create three blocks but put all transactions in second block
			final Block parentBlock = NisUtils.createParentBlock(blockSigner, parentHeight.getRaw());
			final List<Block> blocks = NisUtils.createBlockList(blockSigner, parentBlock, 3, parentBlock.getTimeStamp());
			blocks.get(1).addTransactions(all);
			NisUtils.signAllBlocks(blocks);

			// Act:
			final ValidationResult result = blockChainServices.isPeerChainValid(copyCache, parentBlock, blocks);
			if (isTransientFailure(result)) {
				// if the failure indicates a transient failure, retry
				continue;
			}

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(expectedResult));
			break;
		}
	}

	private static boolean isTransientFailure(final ValidationResult result) {
		// TODO 20150817 J-B: i guess due to the way the tests generate blocks, it's possible for some chains to contain blocks that are not hits
		// > (or at least that's what i'm observing); i think retrying when that error occurs is ok; thoughts?
		return ValidationResult.FAILURE_BLOCK_NOT_HIT == result;
	}

	private static Account createBlockSigner(final NisCache nisCache, final BlockHeight blockHeight) {
		final Amount amount = Amount.fromNem(1_000_000);
		final Account blockSigner = Utils.generateRandomAccount();
		final AccountState blockSignerState = nisCache.getAccountStateCache().findStateByAddress(blockSigner.getAddress());
		blockSignerState.getImportanceInfo().setImportance(GroupedHeight.fromHeight(blockHeight), 0.1);
		blockSignerState.getAccountInfo().incrementBalance(amount);
		blockSignerState.setHeight(BlockHeight.ONE);
		blockSignerState.getWeightedBalances().addFullyVested(BlockHeight.ONE, amount);
		return blockSigner;
	}
}
