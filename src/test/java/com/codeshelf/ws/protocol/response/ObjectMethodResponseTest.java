package com.codeshelf.ws.protocol.response;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.domain.Item;
import com.codeshelf.testframework.MinimalTest;
import com.codeshelf.validation.DefaultErrors;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.ws.protocol.response.ObjectMethodResponse;
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
}
