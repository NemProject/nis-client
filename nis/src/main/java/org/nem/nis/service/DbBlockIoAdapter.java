package org.nem.nis.service;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.MissingResourceException;

@Service
public class DbBlockIoAdapter implements BlockIo {
	private final ReadOnlyBlockDao blockDao;
	private final NisDbModelToModelMapper mapper;

	@Autowired(required = true)
	public DbBlockIoAdapter(final ReadOnlyBlockDao blockDao, final NisDbModelToModelMapper mapper) {
		this.blockDao = blockDao;
		this.mapper = mapper;
	}

	@Override
	public Block getBlock(final Hash blockHash) {
		final org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHash(blockHash);
		if (null == dbBlock) {
			throw createMissingResourceException(blockHash.toString());
		}

		return this.mapper.map(dbBlock);
	}

	@Override
	public Block getBlockAt(final BlockHeight blockHeight) {
		final org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHeight(blockHeight);
		if (null == dbBlock) {
			throw createMissingResourceException(blockHeight.toString());
		}

		return this.mapper.map(dbBlock);
	}

	private static MissingResourceException createMissingResourceException(final String key) {
		return new MissingResourceException("block not found in the db", Block.class.getName(), key);
	}
}
