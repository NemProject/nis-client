package org.nem.nis.time.synchronization.filter;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.primitive.NodeAge;
import org.nem.nis.test.TimeSyncUtils;
import org.nem.nis.time.synchronization.TimeSynchronizationSample;

import java.util.*;

public class AggregateSynchronizationFilterTest {

	@Test
	public void filterDelegatesToAllFilters() {
		// Arrange:
		final ClampingFilter filter1 = Mockito.mock(ClampingFilter.class);
		final AlphaTrimmedMeanFilter filter2 = Mockito.mock(AlphaTrimmedMeanFilter.class);
		Mockito.when(filter1.filter(Mockito.any(), Mockito.any())).thenReturn(null);
		Mockito.when(filter2.filter(Mockito.any(), Mockito.any())).thenReturn(null);
		final AggregateSynchronizationFilter filter = new AggregateSynchronizationFilter(
				Arrays.asList(filter1, filter2));

		// Act:
		filter.filter(null, null);

		// Assert:
		Mockito.verify(filter1, Mockito.times(1)).filter(Mockito.any(), Mockito.any());
		Mockito.verify(filter2, Mockito.times(1)).filter(Mockito.any(), Mockito.any());
	}

	@Test
	public void filtersAreAppliedInGivenOrder() {
		// Arrange:
		final ClampingFilter filter1 = Mockito.mock(ClampingFilter.class);
		final AlphaTrimmedMeanFilter filter2 = Mockito.mock(AlphaTrimmedMeanFilter.class);
		Mockito.when(filter1.filter(Mockito.any(), Mockito.any())).thenReturn(null);
		Mockito.when(filter1.filter(Mockito.any(), Mockito.any())).thenReturn(null);
		final InOrder inOrder = Mockito.inOrder(filter1, filter2);
		final AggregateSynchronizationFilter filter = new AggregateSynchronizationFilter(
				Arrays.asList(filter1, filter2));

		// Act:
		filter.filter(null, null);

		// Assert:
		inOrder.verify(filter1).filter(Mockito.any(), Mockito.any());
		inOrder.verify(filter2).filter(Mockito.any(), Mockito.any());
	}

	@Test
	public void filtersAreChained() {
		// Arrange:
		final ClampingFilter filter1 = Mockito.mock(ClampingFilter.class);
		final AlphaTrimmedMeanFilter filter2 = Mockito.mock(AlphaTrimmedMeanFilter.class);
		final List<TimeSynchronizationSample> originalSamples = TimeSyncUtils.createTolerableSortedSamples(0, 10);
		final List<TimeSynchronizationSample> samples1 = TimeSyncUtils.createTolerableSortedSamples(20, 10);
		final List<TimeSynchronizationSample> samples2 = TimeSyncUtils.createTolerableSortedSamples(40,10);
		Mockito.when(filter1.filter(originalSamples, new NodeAge(0))).thenReturn(samples1);
		Mockito.when(filter2.filter(samples1, new NodeAge(0))).thenReturn(samples2);
		final AggregateSynchronizationFilter filter = new AggregateSynchronizationFilter(
				Arrays.asList(filter1, filter2));

		// Act:
		List<TimeSynchronizationSample> samples = filter.filter(originalSamples, new NodeAge(0));

		// Assert:
		Mockito.verify(filter1, Mockito.times(1)).filter(originalSamples, new NodeAge(0));
		Mockito.verify(filter2, Mockito.times(1)).filter(samples1, new NodeAge(0));
		Assert.assertThat(samples, IsEqual.equalTo(samples2));
	}

	//TODO-CR: can you add an additional test that ensures the filters are chained (e.g. the output of the first filter is the input to the second and the output of the last is returned)
	// TODO: BR -> J done.
	//TODO-CR: i will also make a general comment here that when using mocks it's important to validate that the return from the mock is used (even if it is passed through the function)
	// TODO: BR -> J not sure I understand, this only is important in some tests like filtersAreChained but not in the first two tests.
}
