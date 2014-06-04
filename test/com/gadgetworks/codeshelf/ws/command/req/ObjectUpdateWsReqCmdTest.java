package com.gadgetworks.codeshelf.ws.command.req;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.UUID;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.MockModel;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;

@RunWith(MockitoJUnitRunner.class)
public class ObjectUpdateWsReqCmdTest {
	
	@Mock
	private ITypedDao<MockModel> mockTypedDao;
	
	@Mock
	private IDaoProvider mockDaoProvider;

	@Test
	public void shouldChangeValueOfBooleanSetter() throws JsonProcessingException, IOException {
		String testProperty = "testSetterBoolean";
		String testValue = "true";
		
		MockModel mockModel = testSetter(testProperty, testValue);
		Assert.assertEquals(new Boolean(testValue).booleanValue(), mockModel.getTestSetterBoolean()); //note that this correlates to the the test property getter/setter
	}



	
	@Test
	public void shouldChangeValueOfDoubleSetter() throws JsonProcessingException, IOException {
		String testProperty = "testSetterDouble";
		String testValue = "1.1";
		
		MockModel mockModel = testSetter(testProperty, testValue);
		Assert.assertEquals(Double.valueOf(testValue).doubleValue(), mockModel.getTestSetterDouble(), 0.0); //note that this correlates to the the test property getter/setter
	}


	
	@Test
	public void shouldChangeValueOfIntSetter() throws JsonProcessingException, IOException {
		String testProperty = "testSetterInt";
		String testValue = "1";
		
		MockModel mockModel = testSetter(testProperty, testValue);
		Assert.assertEquals(Integer.valueOf(testValue).intValue(), mockModel.getTestSetterInt()); //note that this correlates to the the test property getter/setter
	}

	@Test
	public void shouldChangeValueOfStringSetter() throws JsonProcessingException, IOException {
		String testValue = "testString";
		String testProperty = "testSetterString";
		
		MockModel mockModel = testSetter(testProperty, testValue);
		Assert.assertEquals(testValue, mockModel.getTestSetterString()); //note that this correlates to the the test property getter/setter
	}
	
	@Test
	public void shouldReturnErrorResponseWhenClassDoesntExist() throws JsonProcessingException, IOException {
		ObjectNode mockJsonNode = createReqCmdJsonNode();
		mockJsonNode.put(IWsReqCmd.CLASSNAME, "com.gadgetworks.codeshelf.model.domain.NOTFOUND");
		
		ObjectUpdateWsReqCmd subject = new ObjectUpdateWsReqCmd("commandId", mockJsonNode, mockDaoProvider);
		IWsRespCmd respCmd = subject.exec();

		assertIsErrorResponse(respCmd);
	}

	@Test
	public void shouldReturnErrorResponseWhenInstanceNotFound() throws JsonProcessingException, IOException {

		when(mockDaoProvider.getDaoInstance(MockModel.class)).thenReturn(mockTypedDao);
		when(mockTypedDao.findByPersistentId(any(UUID.class))).thenReturn(null);

		
		ObjectNode mockJsonNode = createReqCmdJsonNode();
		
		ObjectUpdateWsReqCmd subject = new ObjectUpdateWsReqCmd("commandId", mockJsonNode, mockDaoProvider);
		IWsRespCmd respCmd = subject.exec();
		
		assertIsErrorResponse(respCmd);
	}

	
	private MockModel testSetter(String testProperty, String testValue) throws JsonParseException, JsonMappingException, IOException {

		MockModel mockModel = new MockModel();
		when(mockDaoProvider.getDaoInstance(MockModel.class)).thenReturn(mockTypedDao);
		when(mockTypedDao.findByPersistentId(any(UUID.class))).thenReturn(mockModel);
		
		ObjectNode mockJsonNode = createReqCmdJsonNode(testProperty, testValue);
		
		ObjectUpdateWsReqCmd subject = new ObjectUpdateWsReqCmd("commandId", mockJsonNode, mockDaoProvider);
		IWsRespCmd respCmd = subject.exec();
		Assert.assertEquals(false, respCmd.isError());
		return mockModel;
	}

	private void assertIsErrorResponse(IWsRespCmd respCmd) throws IOException, JsonParseException, JsonMappingException {
		Assert.assertNotNull("Command response should not have been null", respCmd);
		String response = respCmd.getResponseMsg();
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.readValue(response, ObjectNode.class);
		String status = node.path("data").path("status").asText();
		Assert.assertEquals("ERROR", status);
		ArrayNode arrayNode = (ArrayNode) node.path("data").path("results").path("messages");
		Assert.assertEquals(1, arrayNode.size());
	}

	

	
	private ObjectNode createReqCmdJsonNode() throws JsonParseException, JsonMappingException, IOException {
		return createReqCmdJsonNode("testSetterString", "testString");
	}
	
	private ObjectNode createReqCmdJsonNode(String testProperty, String testValue) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode objectNode = mapper.createObjectNode();
		objectNode.put(IWsReqCmd.CLASSNAME, "com.gadgetworks.codeshelf.model.domain.MockModel");
		objectNode.put(IWsReqCmd.PERSISTENT_ID, UUID.randomUUID().toString());
		objectNode.put("properties", mapper.readValue(
			toDoubleQuote("{ '"+  testProperty + "': '"+  testValue +"'}"), ObjectNode.class));
		return objectNode;
	}
	
	private String toDoubleQuote(String simpleJSONSyntax) {
		return simpleJSONSyntax.replaceAll("'", "\"");
		
	}

}
