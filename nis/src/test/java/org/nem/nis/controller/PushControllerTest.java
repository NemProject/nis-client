package org.nem.nis.controller;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.nis.test.MockPeerConnector;

import java.net.MalformedURLException;

public class PushControllerTest {

	@Test
	public void transferIncorrectPush() throws MalformedURLException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", 123456);

		// Act:
		JsonDeserializer res = pc.pushTransaction(obj);

		// Assert:
		Assert.assertThat(res.readInt("status"), IsEqual.equalTo(400));
	}
}
