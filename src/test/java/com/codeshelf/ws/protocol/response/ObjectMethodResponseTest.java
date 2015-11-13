package com.codeshelf.ws.protocol.response;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.domain.Item;
import com.codeshelf.testframework.MinimalTest;
import com.codeshelf.validation.DefaultErrors;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.codeshelf.ws.protocol.message.NotificationMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMethodResponseTest extends MinimalTest {

	@Test
	public void testSerialization() throws IOException {
		ObjectMethodResponse response = new ObjectMethodResponse();
		DefaultErrors errors = new DefaultErrors(Item.class);
		errors.rejectValue("quantity", null, ErrorCode.FIELD_REQUIRED);
		response.setErrors(errors);
		ObjectMapper mapper = new ObjectMapper();
		String result = mapper.writeValueAsString(response);
		JsonNode json = mapper.readTree(result);
		JsonNode responseNode = json.get("ObjectMethodResponse");
		Assert.assertNotNull(responseNode);
		JsonNode errorsNode = responseNode.get("errors");
		Assert.assertNotNull(errorsNode);
		JsonNode fieldErrors = errorsNode.get("fieldErrors");
		Assert.assertNotNull(fieldErrors);
		Assert.assertEquals(fieldErrors, mapper.readTree("{'quantity': [{'message': 'quantity is required'}]}".replace('\'', '"')));
	}
	
	/**
	 * Yes, guilty of testing java itself and showing some ignorance. But we have this confusion in our code
	 * See SiteControllerMessageProcecessor.handleResponse() for lots of instanceof.
	 * class GenericDeviceResponse extends DeviceResponseABC
	 * DeviceResponseABC and NotificationMessage extend MessageABC
	 */
	@Test
	public void testInstanceAssignable(){
		// Phase 1: compile time knowledge
		GenericDeviceResponse response1 = new GenericDeviceResponse();
		// as used  by SiteControllerMessageProcecessor.handleResponse()
		Assert.assertTrue(response1 instanceof DeviceResponseABC);
		Assert.assertTrue(response1 instanceof GenericDeviceResponse);
		
		// as used many places
		Assert.assertFalse(response1.getClass().isAssignableFrom(DeviceResponseABC.class));
		Assert.assertTrue(DeviceResponseABC.class.isAssignableFrom(response1.getClass()));
		Assert.assertTrue(GenericDeviceResponse.class.isAssignableFrom(response1.getClass()));
		
		// Phase 2: can still infer at compile time
		MessageABC response2 = new GenericDeviceResponse();
		Assert.assertTrue(response2 instanceof DeviceResponseABC);
		Assert.assertTrue(response2 instanceof GenericDeviceResponse);

		Assert.assertFalse(response2.getClass().isAssignableFrom(DeviceResponseABC.class));
		Assert.assertTrue(DeviceResponseABC.class.isAssignableFrom(response2.getClass()));
		Assert.assertTrue(GenericDeviceResponse.class.isAssignableFrom(response2.getClass()));

		// Phase 3: not as easy to determine at compile time. More similar to random message types coming in to SiteControllerMessageProcecessor.handleResponse()
		MessageABC response3 = new GenericDeviceResponse();
		MessageABC response4 = new VerifyBadgeResponse();
		MessageABC response5 = new NotificationMessage();
		MessageABC response6 = response5;
		response6 = response3;

		Assert.assertTrue(response6 instanceof DeviceResponseABC);
		Assert.assertTrue(response6 instanceof GenericDeviceResponse);

		Assert.assertFalse(response6.getClass().isAssignableFrom(DeviceResponseABC.class));
		Assert.assertTrue(DeviceResponseABC.class.isAssignableFrom(response6.getClass()));
		Assert.assertTrue(GenericDeviceResponse.class.isAssignableFrom(response6.getClass()));

		response6 = response4;

		Assert.assertTrue(response6 instanceof DeviceResponseABC);
		Assert.assertFalse(response6 instanceof GenericDeviceResponse);

		Assert.assertFalse(response6.getClass().isAssignableFrom(DeviceResponseABC.class));
		Assert.assertTrue(DeviceResponseABC.class.isAssignableFrom(response6.getClass()));
		Assert.assertFalse(GenericDeviceResponse.class.isAssignableFrom(response6.getClass()));

	}
}
