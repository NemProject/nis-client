package org.nem.nis.controller;

import net.minidev.json.JSONObject;
import org.apache.commons.codec.DecoderException;
import org.nem.core.dao.BlockDao;
import org.nem.core.dbmodel.Block;

import org.nem.core.mappers.BlockMapper;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.AccountAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
public class BlockController {
	private static final Logger LOGGER = Logger.getLogger(BlockController.class.getName());

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private AccountAnalyzer accountAnalyzer;

	@RequestMapping(value="/block/get", method = RequestMethod.GET)
	public String blockGet(@RequestParam(value = "blockHash") String blockHashString) {
		byte[] blockHash;
		try {
			blockHash = HexEncoder.getBytes(blockHashString);

		} catch (DecoderException e) {
			return Utils.jsonError(1, "invalid blockHash");
		}

		Block block = blockDao.findByHash(blockHash);
		if (block == null) {
			return Utils.jsonError(2, "hash not found in the db");
		}

		org.nem.core.model.Block response = BlockMapper.toModel(block, accountAnalyzer);

		JSONObject obj = JsonSerializer.serializeToJson(response);
		return obj.toJSONString() + "\r\n";
	}

}
