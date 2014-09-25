package com.gadgetworks.codeshelf.util;

import javax.websocket.DecodeException;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.ws.jetty.io.CompressedJsonMessage;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonDecoder;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.LoginResponse;
public class CompressedJsonMessageTest {
	@Test
	public void testCompress() {
		String inputString = "{\"LoginResponse\":{\"messageId\":\"db321e90-42bb-11e4-9477-bc764e055d8e\",\"status\":\"Success\",\"statusMessage\":null,\"requestId\":\"d68622b0-42bb-11e4-9287-72f840a438e3\",\"organization\":{\"className\":\"Organization\",\"domainId\":\"DEMO1\",\"persistentId\":\"76794820-3eb5-11e4-b85d-bc764e055d8e\",\"version\":1410990982567,\"description\":\"\"},\"user\":{\"className\":\"User\",\"domainId\":\"5000\",\"persistentId\":\"17445d50-405d-11e4-9fb6-bc764e055d8e\",\"version\":1411172929445,\"created\":1411172929345,\"active\":true},\"network\":{\"className\":\"CodeshelfNetwork\",\"domainId\":\"DEFAULT\",\"persistentId\":\"12fe4060-3eb6-11e4-b85d-bc764e055d8e\",\"version\":1410991245158,\"description\":\"\",\"channel\":10,\"networkNum\":1,\"active\":true,\"connected\":false,\"ches\":{\"CHE3\":{\"className\":\"Che\",\"domainId\":\"CHE3\",\"persistentId\":\"1304d010-3eb6-11e4-b85d-bc764e055d8e\",\"version\":1410991245201,\"deviceGuid\":\"AACZkw==\",\"description\":\"\",\"lastBatteryLevel\":0,\"networkDeviceStatus\":null,\"lastContactTime\":null,\"networkAddress\":0,\"color\":\"BLUE\"},\"CHE4\":{\"className\":\"Che\",\"domainId\":\"CHE4\",\"persistentId\":\"13060890-3eb6-11e4-b85d-bc764e055d8e\",\"version\":1410991245209,\"deviceGuid\":\"AACZlA==\",\"description\":\"\",\"lastBatteryLevel\":0,\"networkDeviceStatus\":null,\"lastContactTime\":null,\"networkAddress\":0,\"color\":\"BLUE\"},\"CHE5\":{\"className\":\"Che\",\"domainId\":\"CHE5\",\"persistentId\":\"1306f2f0-3eb6-11e4-b85d-bc764e055d8e\",\"version\":1410991245216,\"deviceGuid\":\"AACZlQ==\",\"description\":\"\",\"lastBatteryLevel\":0,\"networkDeviceStatus\":null,\"lastContactTime\":null,\"networkAddress\":0,\"color\":\"BLUE\"},\"CHE6\":{\"className\":\"Che\",\"domainId\":\"CHE6\",\"persistentId\":\"13080460-3eb6-11e4-b85d-bc764e055d8e\",\"version\":1410991245222,\"deviceGuid\":\"AACZlg==\",\"description\":\"\",\"lastBatteryLevel\":0,\"networkDeviceStatus\":null,\"lastContactTime\":null,\"networkAddress\":0,\"color\":\"BLUE\"},\"CHE1\":{\"className\":\"Che\",\"domainId\":\"CHE1\",\"persistentId\":\"13014da0-3eb6-11e4-b85d-bc764e055d8e\",\"version\":1410991626257,\"deviceGuid\":\"AAAALQ==\",\"description\":\"\",\"lastBatteryLevel\":0,\"networkDeviceStatus\":null,\"lastContactTime\":null,\"networkAddress\":0,\"color\":\"BLUE\"},\"CHE2\":{\"className\":\"Che\",\"domainId\":\"CHE2\",\"persistentId\":\"13039790-3eb6-11e4-b85d-bc764e055d8e\",\"version\":1410991638001,\"deviceGuid\":\"AAAAIg==\",\"description\":\"\",\"lastBatteryLevel\":0,\"networkDeviceStatus\":null,\"lastContactTime\":null,\"networkAddress\":0,\"color\":\"BLUE\"}},\"ledControllers\":{\"00000016\":{\"className\":\"LedController\",\"domainId\":\"00000016\",\"persistentId\":\"71eac9e0-3eb6-11e4-b85d-bc764e055d8e\",\"version\":1410991569741,\"deviceGuid\":\"AAAAFg==\",\"description\":\"\",\"lastBatteryLevel\":0,\"networkDeviceStatus\":null,\"lastContactTime\":null,\"networkAddress\":0},\"00000012\":{\"className\":\"LedController\",\"domainId\":\"00000012\",\"persistentId\":\"71ed3ae0-3eb6-11e4-b85d-bc764e055d8e\",\"version\":1410991558408,\"deviceGuid\":\"AAAAEg==\",\"description\":\"\",\"lastBatteryLevel\":0,\"networkDeviceStatus\":null,\"lastContactTime\":null,\"networkAddress\":0},\"0000000e\":{\"className\":\"LedController\",\"domainId\":\"0000000e\",\"persistentId\":\"71f10b70-3eb6-11e4-b85d-bc764e055d8e\",\"version\":1410991547472,\"deviceGuid\":\"AAAADg==\",\"description\":\"\",\"lastBatteryLevel\":0,\"networkDeviceStatus\":null,\"lastContactTime\":null,\"networkAddress\":0},\"00000027\":{\"className\":\"LedController\",\"domainId\":\"00000027\",\"persistentId\":\"71ef36b0-3eb6-11e4-b85d-bc764e055d8e\",\"version\":1410991537537,\"deviceGuid\":\"AAAAJw==\",\"description\":\"\",\"lastBatteryLevel\":0,\"networkDeviceStatus\":null,\"lastContactTime\":null,\"networkAddress\":0}},\"siteControllers\":{\"5000\":{\"className\":\"SiteController\",\"domainId\":\"5000\",\"persistentId\":\"172ed980-405d-11e4-9fb6-bc764e055d8e\",\"version\":1411172929313,\"deviceGuid\":\"AAAAAA==\",\"description\":\"Site Controller for DEFAULT\",\"lastBatteryLevel\":0,\"networkDeviceStatus\":null,\"lastContactTime\":null,\"networkAddress\":0,\"monitor\":false,\"describeLocation\":\"Test Area\"}}}}}";
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
		Assert.assertTrue(login.getUser().getDomainId().equals("5000"));
	}
	public void testPassthrough() {
		
	}
}
