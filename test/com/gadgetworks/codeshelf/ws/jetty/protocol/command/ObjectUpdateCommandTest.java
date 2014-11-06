package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.MockModel;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectUpdateRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;

@RunWith(MockitoJUnitRunner.class)
public class ObjectUpdateCommandTest {
	@Mock
	private ITypedDao<MockModel> mockTypedDao;
	
	@Test
	public void shouldChangeValueOfBooleanSetter() throws JsonProcessingException, IOException {
		String testProperty = "testSetterBoolean";
		Boolean testValue = Boolean.TRUE;
		
		MockModel mockModel = testSetter(testProperty, testValue);
		Assert.assertEquals(new Boolean(testValue).booleanValue(), mockModel.getTestSetterBoolean()); //note that this correlates to the the test property getter/setter
	}
	
	@Test
	public void shouldChangeValueOfDoubleSetter() throws JsonProcessingException, IOException {
		String testProperty = "testSetterDouble";
		Double testValue = 1.1d;
		
		MockModel mockModel = testSetter(testProperty, testValue);
		Assert.assertEquals(Double.valueOf(testValue).doubleValue(), mockModel.getTestSetterDouble(), 0.0); //note that this correlates to the the test property getter/setter
	}

	@Test
	public void shouldFailDoubleSetterWrongType() throws JsonProcessingException, IOException {
		String testProperty = "testSetterDouble";
		String testValue = "1.1";
		
		ResponseABC resp = testSetterFail(testProperty, testValue);
		Assert.assertTrue(resp.getStatus().equals(ResponseStatus.Fail));
	}
	
	@Test
	public void shouldFailIntSetterWrongType() throws JsonProcessingException, IOException {
		String testProperty = "testSetterInt";
		String testValue = "1";
		
		ResponseABC resp = testSetterFail(testProperty, testValue);
		Assert.assertTrue(resp.getStatus().equals(ResponseStatus.Fail));
	}

	@Test
	public void shouldChangeValueOfIntSetter() throws JsonProcessingException, IOException {
		String testProperty = "testSetterInt";
		Integer testValue = 1;
		
		MockModel mockModel = testSetter(testProperty, testValue);
		Assert.assertEquals(Integer.valueOf(testValue).intValue(), mockModel.getTestSetterInt()); //note that this correlates to the the test property getter/setter
	}
	
	@Test
	public void shouldRespondAsFailed() throws JsonProcessingException, IOException {
		String testProperty = "testSetterInt";
		String testValue = "a";
	
		ResponseABC resp = testSetterFail(testProperty, testValue);
		Assert.assertTrue(resp.getStatus().equals(ResponseStatus.Fail));
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
		ObjectUpdateRequest objectUpdateRequest = createReq();
		objectUpdateRequest.setClassName("com.gadgetworks.codeshelf.model.domain.NOTFOUND");
		
		ObjectUpdateCommand subject = new ObjectUpdateCommand(null, objectUpdateRequest);
		ResponseABC respCmd = subject.exec();

		assertIsErrorResponse(respCmd);
	}

	@Test
	public void shouldReturnErrorResponseWhenInstanceNotFound() throws JsonProcessingException, IOException {

		when(mockTypedDao.findByPersistentId(any(UUID.class))).thenReturn(null);

		ObjectUpdateRequest objectUpdateRequest = createReq();
		
		ObjectUpdateCommand subject = new ObjectUpdateCommand(null, objectUpdateRequest);
		ResponseABC respCmd = subject.exec();
		
		assertIsErrorResponse(respCmd);
	}

	
	private ResponseABC testSetterFail(String testProperty, String testValue) throws JsonParseException, JsonMappingException, IOException {
		MockModel mockModel = new MockModel();
		when(mockTypedDao.findByPersistentId(any(UUID.class))).thenReturn(mockModel);
		
		ObjectUpdateRequest objectUpdateRequest = createReqCmdJsonNode(testProperty, testValue);
		
		ObjectUpdateCommand subject = new ObjectUpdateCommand(null, objectUpdateRequest);
		ResponseABC respCmd = subject.exec();
		return respCmd;
	}
	
	private MockModel testSetter(String testProperty, Object testValue) throws JsonParseException, JsonMappingException, IOException {

		MockModel mockModel = new MockModel();
		when(mockTypedDao.findByPersistentId(any(UUID.class))).thenReturn(mockModel);
		
		ObjectUpdateRequest objectUpdateRequest = createReqCmdJsonNode(testProperty, testValue);
		
		ObjectUpdateCommand subject = new ObjectUpdateCommand(null, objectUpdateRequest);
		ResponseABC respCmd = subject.exec();
		Assert.assertEquals(respCmd.toString(), false, respCmd.getStatus().equals(ResponseStatus.Fail));
		return mockModel;
	}

	private void assertIsErrorResponse(ResponseABC respCmd) throws IOException, JsonParseException, JsonMappingException {
		Assert.assertNotNull("Command response should not have been null", respCmd);
		Assert.assertTrue(respCmd.getStatus().equals(ResponseStatus.Fail));
		Assert.assertNotNull(respCmd.getStatusMessage());
	}
	
	private ObjectUpdateRequest createReq() throws JsonParseException, JsonMappingException, IOException {
		return createReqCmdJsonNode("testSetterString", "testString");
	}
	
	private ObjectUpdateRequest createReqCmdJsonNode(String testProperty, Object testValue) throws JsonParseException, JsonMappingException, IOException {
		Map <String, Object> properties = new HashMap<String, Object>();
		properties.put(testProperty, testValue);
		ObjectUpdateRequest req =  new ObjectUpdateRequest("com.gadgetworks.codeshelf.model.domain.MockModel", UUID.randomUUID(), properties);
		return req;
	}
	
	@SuppressWarnings("unused")
	private String toDoubleQuote(String simpleJSONSyntax) {
		return simpleJSONSyntax.replaceAll("'", "\"");
		
	}
}
