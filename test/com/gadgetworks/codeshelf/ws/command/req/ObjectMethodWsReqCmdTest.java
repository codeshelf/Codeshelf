package com.gadgetworks.codeshelf.ws.command.req;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.MockModel;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;

@RunWith(MockitoJUnitRunner.class)
public class ObjectMethodWsReqCmdTest {
	
	@Mock
	private ITypedDao<MockModel> mockTypedDao;
	
	@Mock
	private IDaoProvider mockDaoProvider;

	@Mock
	private MockModel mockModel;

	
	@Test
	public void shouldReturnErrorResponseWhenClassDoesntExist() throws JsonProcessingException, IOException {
		ObjectNode mockJsonNode = createReqCmdJsonNode();
		mockJsonNode.put(IWsReqCmd.CLASSNAME, "com.gadgetworks.codeshelf.model.domain.NOTFOUND");
		
		ObjectMethodWsReqCmd subject = new ObjectMethodWsReqCmd("commandId", mockJsonNode, mockDaoProvider);
		IWsRespCmd respCmd = subject.exec();

		assertIsErrorResponse(respCmd);
	}

	@Test
	public void shouldReturnErrorResponseWhenInstanceNotFound() throws JsonProcessingException, IOException {

		when(mockDaoProvider.getDaoInstance(MockModel.class)).thenReturn(mockTypedDao);
		when(mockTypedDao.findByPersistentId(any(UUID.class))).thenReturn(null);

		
		ObjectNode mockJsonNode = createReqCmdJsonNode();
		
		ObjectMethodWsReqCmd subject = new ObjectMethodWsReqCmd("commandId", mockJsonNode, mockDaoProvider);
		IWsRespCmd respCmd = subject.exec();
		
		assertIsErrorResponse(respCmd);
	}

	private void assertIsErrorResponse(IWsRespCmd respCmd) throws IOException, JsonParseException, JsonMappingException {
		String response = respCmd.getResponseMsg();
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.readValue(response, ObjectNode.class);
		String status = node.path("data").path("status").asText();
		Assert.assertEquals("ERROR", status);
		ArrayNode arrayNode = (ArrayNode) node.path("data").path("results").path("messages");
		Assert.assertEquals(1, arrayNode.size());
	}

	
	private ObjectNode createReqCmdJsonNode() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode objectNode = mapper.createObjectNode();
		objectNode.put(IWsReqCmd.CLASSNAME, "com.gadgetworks.codeshelf.model.domain.MockModel");
		objectNode.put(IWsReqCmd.PERSISTENT_ID, UUID.randomUUID().toString());
		objectNode.put(IWsReqCmd.METHODNAME, "testMethod");
		objectNode.put(IWsReqCmd.METHODARGS, mapper.readValue(
			toDoubleQuote("[	{ 'name': 'testParam', 'value': 'testName', 'classType': 'java.lang.String'}]"), ArrayNode.class));
		return objectNode;
	}
	
	private String toDoubleQuote(String simpleJSONSyntax) {
		return simpleJSONSyntax.replaceAll("'", "\"");
		
	}

}
