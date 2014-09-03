package org.nem.nis.time.synchronization;

import org.nem.core.model.primitive.*;
import org.nem.core.time.synchronization.TimeSynchronizationSample;

import java.util.List;

/**
 * Calculates the offset in time between the local computer clock and the the network time
 * base on the list of synchronization samples.
 */
public interface TimeSynchronizationStrategy {

	/**
	 * Calculates the offset in time between the local computer clock and the the network time
	 * based on the list of synchronization samples.
	 *
	 * @param samples The list of synchronization samples.
	 * @return The time offset in ms.
	 */
	public TimeOffset calculateTimeOffset(final List<TimeSynchronizationSample> samples, final NodeAge age);
}
