package com.gadgetworks.codeshelf.ws.jetty;

import java.io.IOException;

import javax.websocket.DecodeException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gadgetworks.codeshelf.application.Configuration;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionCount;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonDecoder;
import com.gadgetworks.codeshelf.ws.jetty.io.ObjectMixIn;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.LoginRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ComputeWorkResponse;

public class JsonDecoderTest  {

	private static final Logger	LOGGER = LoggerFactory.getLogger(JsonDecoderTest.class);

	static {
		Configuration.loadConfig("test");
	}
	
	/**
	 * Test simple decoding without exception
	 */
	@Test
	public void testLoginRequestDecoding() throws DecodeException {
		JsonDecoder decoder = new JsonDecoder();
		String rawMessage = "{'LoginRequest':{'userId':'a@example.com','password':'testme','messageId':'cid_4'}}".replace('\'', '"');
		MessageABC messageABC = decoder.decode(rawMessage);
		Assert.assertTrue(messageABC instanceof LoginRequest);
	}
	
	@Test
	public void testJsonPojoSerDeser() throws DecodeException, JsonParseException, JsonMappingException, IOException {
		JsonPojo pojo = new JsonPojo();
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.addMixInAnnotations(Object.class, ObjectMixIn.class);
		mapper.registerSubtypes(JsonPojo.class);
		
		String jsonString = "";
		try {
			jsonString = mapper.writeValueAsString(pojo);
			System.out.println(jsonString);
		} catch (Exception e) {
			LOGGER.error("Failed to serialize JsonPojo", e);
			Assert.fail("Failed to serialize JsonPojo");
		}		
		try {
			Object object = mapper.readValue(jsonString, Object.class);
			System.out.println(object);
			Assert.assertTrue(object instanceof JsonPojo);
		}
		catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testPointSerDeser() throws DecodeException, JsonParseException, JsonMappingException, IOException {
		Point point = new Point(PositionTypeEnum.GPS,123d,232d,0d);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.addMixInAnnotations(Object.class, ObjectMixIn.class);
		mapper.registerSubtypes(Point.class);
		mapper.registerSubtypes(PathSegment.class);
		
		String jsonString = "";
		try {
			jsonString = mapper.writeValueAsString(point);
			System.out.println(jsonString);
		} catch (Exception e) {
			LOGGER.error("Failed to seriaize point", e);
			Assert.fail("Failed to seriaize point");
		}		
		try {
			Object object = mapper.readValue(jsonString, Object.class);
			System.out.println(object);
			Assert.assertTrue(object instanceof Point);
		}
		catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}	
	
	@Test
	public void testPointPathArraySerialization() throws DecodeException {
		PathSegment[] segments = createPathSegment(3);
		ObjectMapper mapper = new ObjectMapper();
		String jsonString = "";
		try {
			jsonString = mapper.writeValueAsString(segments);
			System.out.println(jsonString);
		} catch (Exception e) {
			LOGGER.error("Failed to seriaize point", e);
			Assert.fail("Failed to seriaize point");
		}
		int pos = jsonString.indexOf("className");
		Assert.assertTrue(pos>=0);
	}

	private PathSegment[] createPathSegment(int numberOfSegments) {
		PathSegment[] segments = new PathSegment[numberOfSegments];
		for (int i = 0; i < numberOfSegments; i++) {
			double di = i;
			PathSegment segment = new PathSegment();
			segment.setDomainId("P."+i);
			segment.setSegmentOrder(i);
			segment.setPosType(PositionTypeEnum.METERS_FROM_PARENT);
			segment.setStartPosX(di);
			segment.setStartPosY(di);
			segment.setStartPosZ(di);
			segment.setEndPosX(di);
			segment.setEndPosY(di);
			segment.setEndPosZ(di);			
			segments[i]=segment;
		}
		return segments;
	}
	
	@Test
	public void testComputeWorkResponseDeserialization() throws IOException {
		ComputeWorkResponse computeWorkResp = new ComputeWorkResponse();
		computeWorkResp.addWorkInstructionCount("Container1", new WorkInstructionCount(0, 5, 0, 5, 6));
		computeWorkResp.addWorkInstructionCount("Container2", new WorkInstructionCount(12, 45565, 0, 0, 0));
		computeWorkResp.addWorkInstructionCount("01234567890123456789", new WorkInstructionCount(255,
			66,
			Integer.MAX_VALUE,
			Integer.MIN_VALUE,
			-1));
		computeWorkResp.setTotalGoodWorkInstructions(300);

		//Serialize and Deserialize
		ObjectMapper mapper = new ObjectMapper();
		String serialized = mapper.writeValueAsString(computeWorkResp);
		ComputeWorkResponse deserializedWorkResp = mapper.readValue(serialized, ComputeWorkResponse.class);
		Assert.assertEquals(deserializedWorkResp.getMessageId(), computeWorkResp.getMessageId());
		Assert.assertEquals(deserializedWorkResp.getNetworkGuid(), computeWorkResp.getNetworkGuid());
		Assert.assertEquals(deserializedWorkResp.getRequestId(), computeWorkResp.getRequestId());
		Assert.assertEquals(deserializedWorkResp.getStatusMessage(), computeWorkResp.getStatusMessage());
		Assert.assertEquals(deserializedWorkResp.getContainerToWorkInstructionCountMap(),
			computeWorkResp.getContainerToWorkInstructionCountMap());
		Assert.assertEquals(deserializedWorkResp.getStatus(), computeWorkResp.getStatus());
		Assert.assertEquals(deserializedWorkResp.getTotalGoodWorkInstructions(), computeWorkResp.getTotalGoodWorkInstructions());

	}

	/*
	@Test
	public void testObjectMethodRequest() throws DecodeException {
		ObjectMethodRequest request = new ObjectMethodRequest();
		request.setClassName("Organization");
		request.setPersistentId(new UUID().toString());
		request.setMethodName("createFacility");
		
		List<ArgsClass> methodArgs = new LinkedList<ArgsClass>();
		ArgsClass ac1 = new ArgsClass("domainId", "F1", String.class.getName());
		methodArgs.add(ac1);
		ArgsClass ac2 = new ArgsClass("description", "First Facility", String.class.getName());
		methodArgs.add(ac2);
		ArgsClass ac3 = new ArgsClass("anchorPoint", Point.getZeroPoint(), Point.class.getName());
		methodArgs.add(ac3);
		request.setMethodArgs(methodArgs);
		
		JsonEncoder enc = new JsonEncoder();
		String rawMessage = null;
		try {
			rawMessage = enc.encode(request);
			System.out.println(rawMessage.replace('"','\''));
		} catch (EncodeException e) {
			Assert.fail("Failed to encode message");
		}
		JsonDecoder dec = new JsonDecoder();
		MessageABC decodedRequest = dec.decode(rawMessage);
		Assert.assertTrue(decodedRequest instanceof ObjectMethodRequest);
		ObjectMethodRequest objectMethodRequest = (ObjectMethodRequest) decodedRequest;
		Object obj = objectMethodRequest.getMethodArgs().get(2).getValue();
		Assert.assertTrue(obj instanceof Point);
	}
	*/
	
	/*
	@Test
	public void testObjectMethodRequestDecoding() throws DecodeException {
		JsonDecoder decoder = new JsonDecoder();		
		//String rawMessage = "{'ObjectMethodRequest':{'messageId':'da76ec80-225c-11e4-8ce6-48d705ccef0f','className':'Organization','persistentId':'da76ec81-225c-11e4-8ce6-48d705ccef0f','methodName':'createFacility','methodArgs':[{'name':'domainId','value':'F1','classType':'java.lang.String'},{'name':'description','value':'First Facility','classType':'java.lang.String'},{'name':'anchorPoint','value':{'posTypeEnum':'METERS_FROM_PARENT','x':0.0,'y':0.0,'z':0.0},'classType':'com.gadgetworks.codeshelf.model.domain.Point'}]}}".replace('\'', '"');;
		String rawMessage = "{'ObjectMethodRequest':{'messageId':'da76ec80-225c-11e4-8ce6-48d705ccef0f','className':'Organization','persistentId':'da76ec81-225c-11e4-8ce6-48d705ccef0f','methodName':'createFacility','methodArgs':[{'name':'domainId','value':'F1','classType':'java.lang.String'},{'name':'description','value':'First Facility','classType':'java.lang.String'},{'name':'anchorPoint','value':{'className':'Point','posType':'METERS_FROM_PARENT','x':0.0,'y':0.0,'z':0.0},'classType':'com.gadgetworks.codeshelf.model.domain.Point'}]}}".replace('\'', '"');;
		MessageABC message = decoder.decode(rawMessage);
		Assert.assertTrue(message instanceof ObjectMethodRequest);
		ObjectMethodRequest objectMethodRequest = (ObjectMethodRequest) message;
		List<ArgsClass> args = objectMethodRequest.getMethodArgs();
		Object obj = args.get(2).getValue();
		Assert.assertTrue(obj instanceof Point);	
	}
	*/

}
