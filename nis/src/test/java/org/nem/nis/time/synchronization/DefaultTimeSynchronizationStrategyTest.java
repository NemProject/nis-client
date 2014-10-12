package org.nem.nis.time.synchronization;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.*;
import org.nem.core.test.TimeSyncUtils;
import org.nem.core.time.synchronization.TimeSynchronizationSample;
import org.nem.nis.poi.*;
import org.nem.nis.time.synchronization.filter.*;

import java.lang.reflect.Field;
import java.util.*;

public class DefaultTimeSynchronizationStrategyTest {

	@Test
	public void defaultTimeSynchronizationStrategyCanBeConstructed() {
		// Act:
		new DefaultTimeSynchronizationStrategy(this.createAggregateFilter(), this.createPoiFacade());
	}

	@Test(expected = TimeSynchronizationException.class)
	public void defaultTimeSynchronizationStrategyCtorThrowsIfFilterIsNull() {
		// Act:
		new DefaultTimeSynchronizationStrategy(null, this.createPoiFacade());
	}

	@Test(expected = TimeSynchronizationException.class)
	public void defaultTimeSynchronizationStrategyCtorThrowsIfPoiFacadeIsNull() {
		// Act:
		new DefaultTimeSynchronizationStrategy(this.createAggregateFilter(), null);
	}

	@Test(expected = TimeSynchronizationException.class)
	public void calculateTimeOffsetThrowsIfNoSamplesAreAvailable() {
		// Arrange:
		final DefaultTimeSynchronizationStrategy strategy = new DefaultTimeSynchronizationStrategy(this.createAggregateFilter(), this.createPoiFacade());

		// Act:
		strategy.calculateTimeOffset(new ArrayList<>(), new NodeAge(0));
	}

	@Test
	public void calculateTimeOffsetDelegatesToFilter() {
		// Act:
		final List<TimeSynchronizationSample> samples = TimeSyncUtils.createTolerableSortedSamples(0, 1);
		final AggregateSynchronizationFilter filter = Mockito.mock(AggregateSynchronizationFilter.class);
		Mockito.when(filter.filter(samples, new NodeAge(0))).thenReturn(samples);
		final PoiFacade facade = this.createPoiFacade();
		createAccountStatesWithUniformImportancesForPoiFacade(facade, samples);
		final DefaultTimeSynchronizationStrategy strategy = new DefaultTimeSynchronizationStrategy(filter, facade);

		// Act:
		strategy.calculateTimeOffset(samples, new NodeAge(0));

		// Assert:
		Mockito.verify(filter, Mockito.times(1)).filter(samples, new NodeAge(0));
	}

	@Test
	public void calculateTimeOffsetWithMaximumCouplingReturnsCorrectValue() {
		// Act:
		final List<TimeSynchronizationSample> samples = TimeSyncUtils.createTimeSynchronizationSamplesWithDifferentKeyPairs(1000, 100);
		final AggregateSynchronizationFilter filter = Mockito.mock(AggregateSynchronizationFilter.class);
		Mockito.when(filter.filter(samples, new NodeAge(0))).thenReturn(samples);
		final PoiFacade facade = this.createPoiFacade();
		createAccountStatesWithUniformImportancesForPoiFacade(facade, samples);
		final DefaultTimeSynchronizationStrategy strategy = new DefaultTimeSynchronizationStrategy(filter, facade);

		// Act:
		final TimeOffset offset = strategy.calculateTimeOffset(samples, new NodeAge(0));

		// Assert:
		Mockito.verify(filter, Mockito.times(1)).filter(samples, new NodeAge(0));
		Assert.assertThat(offset, IsEqual.equalTo(new TimeOffset((long)((100000 + 99.0 * 100.0 / 2.0) * TimeSynchronizationConstants.COUPLING_START / 100.0))));
	}

	@Test
	public void calculateTimeOffsetWitIntermediateCouplingReturnsCorrectValue() {
		// Act:
		final NodeAge age = new NodeAge(TimeSynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND + 5);
		final List<TimeSynchronizationSample> samples = TimeSyncUtils.createRandomTolerableSamplesWithDifferentKeyPairsAroundMean(100, 500);
		final AggregateSynchronizationFilter filter = Mockito.mock(AggregateSynchronizationFilter.class);
		Mockito.when(filter.filter(samples, age)).thenReturn(samples);
		final PoiFacade facade = this.createPoiFacade();
		createAccountStatesWithUniformImportancesForPoiFacade(facade, samples);
		final DefaultTimeSynchronizationStrategy strategy = new DefaultTimeSynchronizationStrategy(filter, facade);

		// Act:
		final TimeOffset offset = strategy.calculateTimeOffset(samples, age);

		// Assert:
		Mockito.verify(filter, Mockito.times(1)).filter(samples, age);
		Assert.assertThat(offset, IsEqual.equalTo(new TimeOffset((long)(100.0 * 500.0 * strategy.getCoupling(age) / 100.0))));
	}

	@Test
	public void calculateTimeOffsetWithMinimumCouplingReturnsCorrectValue() {
		// Act:
		final NodeAge age = new NodeAge(TimeSynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND + 10);
		final List<TimeSynchronizationSample> samples = TimeSyncUtils.createTimeSynchronizationSamplesWithDifferentKeyPairs(1000, 100);
		final AggregateSynchronizationFilter filter = Mockito.mock(AggregateSynchronizationFilter.class);
		Mockito.when(filter.filter(samples, age)).thenReturn(samples);
		final PoiFacade facade = this.createPoiFacade();
		createAccountStatesWithUniformImportancesForPoiFacade(facade, samples);
		final DefaultTimeSynchronizationStrategy strategy = new DefaultTimeSynchronizationStrategy(filter, facade);

		// Act:
		final TimeOffset offset = strategy.calculateTimeOffset(samples, age);

		// Assert:
		Mockito.verify(filter, Mockito.times(1)).filter(samples, age);
		Assert.assertThat(
				offset,
				IsEqual.equalTo(new TimeOffset((long)((100000 + 99.0 * 100.0 / 2.0) * TimeSynchronizationConstants.COUPLING_MINIMUM / 100.0))));
	}

	@Test
	public void calculateTimeOffsetIsInfluencedByImportance() {
		// Act:
		final NodeAge age = new NodeAge(0);
		final List<TimeSynchronizationSample> samples = TimeSyncUtils.createRandomTolerableSamplesWithDifferentKeyPairsAroundMean(2, 1000);
		final AggregateSynchronizationFilter filter = Mockito.mock(AggregateSynchronizationFilter.class);
		Mockito.when(filter.filter(samples, age)).thenReturn(samples);
		final PoiFacade facade = this.createPoiFacade();
		final double[] importances = { 0.1, 0.9 };
		createAccountStatesWithGivenImportancesAndLastPoiVectorSizeForPoiFacade(facade, samples, importances, 2);
		final DefaultTimeSynchronizationStrategy strategy = new DefaultTimeSynchronizationStrategy(filter, facade);

		// Act:
		final TimeOffset offset = strategy.calculateTimeOffset(samples, age);

		// Assert:
		Mockito.verify(filter, Mockito.times(1)).filter(samples, age);

		// Second sample has offset < 1000 and is dominant.
		Assert.assertThat(offset.getRaw() < 1000, IsEqual.equalTo(true));
	}

	@Test
	public void calculateTimeOffsetDoesNotScaleImportanceMoreThanViewSizePercentage() {
		// Act:
		final NodeAge age = new NodeAge(0);
		final List<TimeSynchronizationSample> samples = TimeSyncUtils.createRandomTolerableSamplesWithDifferentKeyPairsAroundMean(2, 10000);
		final AggregateSynchronizationFilter filter = Mockito.mock(AggregateSynchronizationFilter.class);
		Mockito.when(filter.filter(samples, age)).thenReturn(samples);
		final PoiFacade facade = this.createPoiFacade();
		final double[] importances = { 0.0005, 0.0005 };
		createAccountStatesWithGivenImportancesAndLastPoiVectorSizeForPoiFacade(facade, samples, importances, 20);
		final DefaultTimeSynchronizationStrategy strategy = new DefaultTimeSynchronizationStrategy(filter, facade);

		// Act:
		final TimeOffset offset = strategy.calculateTimeOffset(samples, age);

		// Assert:
		Mockito.verify(filter, Mockito.times(1)).filter(samples, age);
		Assert.assertThat(offset, IsEqual.equalTo(new TimeOffset(100L)));
	}

	@Test
	public void getCouplingReturnsCouplingStartForAgeSmallerThanOrEqualToStartDecayAfterRound() {
		// Arrange:
		final DefaultTimeSynchronizationStrategy strategy = new DefaultTimeSynchronizationStrategy(this.createAggregateFilter(), this.createPoiFacade());

		// Assert:
		Assert.assertThat(strategy.getCoupling(new NodeAge(0)),
				IsEqual.equalTo(TimeSynchronizationConstants.COUPLING_START));
		Assert.assertThat(strategy.getCoupling(new NodeAge(TimeSynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND)),
				IsEqual.equalTo(TimeSynchronizationConstants.COUPLING_START));
	}

	@Test
	public void getCouplingDecaysToCouplingMinimumAfterStartDecayAfterRound() {
		// Arrange:
		final DefaultTimeSynchronizationStrategy strategy = new DefaultTimeSynchronizationStrategy(this.createAggregateFilter(), this.createPoiFacade());

		// Assert:
		final double epsilon = 1e-10;
		// Assuming decay strength 0.3 for the following tests:
		// coupling = exp(-0.3) = 0.74081822068171786606687377931782
		Assert.assertEquals(0.74081822068171, strategy.getCoupling(new NodeAge(TimeSynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND + 1)), epsilon);
		// coupling = exp(-0.9) = 0.40656965974059911188345423964563
		Assert.assertEquals(0.40656965974059, strategy.getCoupling(new NodeAge(TimeSynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND + 3)), epsilon);
		// coupling = exp(-1.5) = 0.22313016014842982893328047076401
		Assert.assertEquals(0.22313016014842, strategy.getCoupling(new NodeAge(TimeSynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND + 5)), epsilon);
		// coupling = exp(-2.1) = 0.12245642825298191021864737607263
		Assert.assertEquals(0.12245642825298, strategy.getCoupling(new NodeAge(TimeSynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND + 7)), epsilon);
		// exp(-2.4) = 0.09071795328941250337517222007969 < COUPLING_MINIMUM
		Assert.assertThat(strategy.getCoupling(new NodeAge(TimeSynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND + 8)),
				IsEqual.equalTo(TimeSynchronizationConstants.COUPLING_MINIMUM));
	}

	private SynchronizationFilter createAggregateFilter() {
		return new AggregateSynchronizationFilter(Arrays.asList(new ClampingFilter(), new AlphaTrimmedMeanFilter()));
	}

	private PoiFacade createPoiFacade() {
		return new PoiFacade(Mockito.mock(ImportanceCalculator.class));
	}

	private static void setFacadeLastPoiVectorSize(final PoiFacade facade, final int lastPoiVectorSize) {
		try {
			final Field field = PoiFacade.class.getDeclaredField("lastPoiVectorSize");
			field.setAccessible(true);
			field.set(facade, lastPoiVectorSize);
		} catch (IllegalAccessException | NoSuchFieldException e) {
			throw new RuntimeException("Exception in setFacadeLastPoiVectorSize");
		}
	}

	private static List<PoiAccountState> createAccountStatesWithUniformImportancesForPoiFacade(
			final PoiFacade facade,
			final List<TimeSynchronizationSample> samples) {
		final List<PoiAccountState> accountStates = new ArrayList<>();
		for (int i = 0; i < samples.size(); i++) {
			accountStates.add(facade.findStateByAddress(samples.get(i).getNode().getIdentity().getAddress()));
			accountStates.get(i).getImportanceInfo().setImportance(new BlockHeight(10), 1.0 / samples.size());
		}
		setFacadeLastPoiVectorSize(facade, samples.size());

		return accountStates;
	}

	private static List<PoiAccountState> createAccountStatesWithGivenImportancesAndLastPoiVectorSizeForPoiFacade(
			final PoiFacade facade,
			final List<TimeSynchronizationSample> samples,
			final double[] importances,
			final int lastPoiVectorSize) {
		final List<PoiAccountState> accountStates = new ArrayList<>();
		for (int i = 0; i < samples.size(); i++) {
			accountStates.add(facade.findStateByAddress(samples.get(i).getNode().getIdentity().getAddress()));
			accountStates.get(i).getImportanceInfo().setImportance(new BlockHeight(10), importances[i]);
		}
		setFacadeLastPoiVectorSize(facade, lastPoiVectorSize);

		return accountStates;
	}
}