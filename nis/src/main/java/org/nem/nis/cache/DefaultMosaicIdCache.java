package org.nem.nis.cache;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.nis.dbmodel.DbMosaicId;

/**
 * Cache which holds a bidirectional MosaicId <--> DbMosaicId map.
 */
public class DefaultMosaicIdCache implements MosaicIdCache {
	private final BidiMap<MosaicId, DbMosaicId> map = new DualHashBidiMap<>();

	// region ReadOnlyMosaicIdCache

	@Override
	public int size() {
		return this.map.size();
	}

	@Override
	public DbMosaicId get(final MosaicId mosaicId) {
		return this.map.get(mosaicId);
	}

	@Override
	public MosaicId get(final DbMosaicId dbMosaicId) {
		return this.map.inverseBidiMap().get(dbMosaicId);
	}

	@Override
	public boolean contains(final MosaicId mosaicId) {
		return this.map.containsKey(mosaicId);
	}

	@Override
	public boolean contains(final DbMosaicId dbMosaicId) {
		return map.containsValue(dbMosaicId);
	}

	// endregion

	// region MosaicIdCache

	// TODO 20150711 BR -> J: is it worth throwing in add/remove if mapping already exists/does not exist?
	// > if yes I'll add it along with tests.
	// TODO 20150715 J-B: i don't necessarily care if you check or not, but i would prefer tests for those edge cases regardless
	// TODO 20150716 BR -> J: added tests. (side note: add should definitely not throw because it will happen that we add an existing mosaic id)
	@Override
	public void add(final MosaicId mosaicId, final DbMosaicId dbMosaicId) {
		this.map.put(mosaicId, dbMosaicId);
	}

	@Override
	public void remove(final MosaicId mosaicId) {
		this.map.remove(mosaicId);
	}

	@Override
	public void remove(final DbMosaicId dbMosaicId) {
		this.map.removeValue(dbMosaicId);
	}

	// endregion
}
