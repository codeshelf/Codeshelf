package com.codeshelf.util;

import javax.websocket.DecodeException;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.ws.jetty.io.CompressedJsonMessage;
import com.codeshelf.ws.jetty.io.JsonDecoder;
import com.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.codeshelf.ws.jetty.protocol.response.LoginResponse;
public class CompressedJsonMessageTest {
	@Test
	public void testCompress() {
		
		String inputString = "{\"LoginResponse\":{\"messageId\":\"a3254500-b092-11e4-addd-08002784e22b\",\"status\":\"Success\",\"statusMessage\":null,\"requestId\":\"a1e94290-b092-11e4-addd-08002784e22b\",\"user\":{\"className\":\"User\",\"username\":\"5000\",\"type\":\"SITECON\",\"created\":1423510450397,\"active\":true},\"network\":{\"className\":\"CodeshelfNetwork\",\"domainId\":\"DEFAULT\",\"persistentId\":\"7b7e5918-a903-416c-9d02-87d6b1aa0ad1\",\"description\":\"\",\"channel\":10,\"networkNum\":1,\"active\":true,\"connected\":false,\"ches\":{\"CHE1\":{\"className\":\"Che\",\"domainId\":\"CHE1\",\"persistentId\":\"e58f4d24-9c50-4cca-a2ae-f18578f70db3\",\"deviceGuid\":\"AACZkQ==\",\"description\":\"\",\"lastBatteryLevel\":0,\"networkDeviceStatus\":null,\"lastContactTime\":null,\"networkAddress\":0,\"color\":\"MAGENTA\",\"processMode\":null},\"CHE2\":{\"className\":\"Che\",\"domainId\":\"CHE2\",\"persistentId\":\"1ba008bc-590f-44bb-b1ba-8342ca028f9d\",\"deviceGuid\":\"AACZkg==\",\"description\":\"\",\"lastBatteryLevel\":0,\"networkDeviceStatus\":null,\"lastContactTime\":null,\"networkAddress\":0,\"color\":\"WHITE\",\"processMode\":null}},\"ledControllers\":{},\"siteControllers\":{\"5000\":{\"className\":\"SiteController\",\"domainId\":\"5000\",\"persistentId\":\"19caee9f-86cc-4938-92d9-d141b8586d76\",\"deviceGuid\":\"AAAAAA==\",\"description\":\"Site Controller for DEFAULT\",\"lastBatteryLevel\":0,\"networkDeviceStatus\":null,\"lastContactTime\":null,\"networkAddress\":0,\"monitor\":false,\"describeLocation\":\"Default Area\"}}},\"autoShortValue\":true,\"pickInfoValue\":\"SKU\",\"containerTypeValue\":\"Order\"}}";
		
		CompressedJsonMessage message = new CompressedJsonMessage(inputString,false);
		JsonDecoder decoder = new JsonDecoder();
		MessageABC decoded=null;
		try {
			decoded = decoder.decode(message.getCompressed());
		} catch (DecodeException e) {
		}
		Assert.assertNotNull(decoded);
		Assert.assertTrue(decoded instanceof LoginResponse);
		LoginResponse login = (LoginResponse)decoded;
		Assert.assertTrue(login.getUser().getUsername().equals("5000"));
	}
	public void testPassthrough() {
		
	}
}
