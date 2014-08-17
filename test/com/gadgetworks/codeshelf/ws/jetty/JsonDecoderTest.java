package com.gadgetworks.codeshelf.ws.jetty;

import javax.websocket.DecodeException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonDecoder;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.LoginRequest;

public class JsonDecoderTest  {

	private static final Logger	LOGGER = LoggerFactory.getLogger(JsonDecoderTest.class);

	static {
		Util.initLogging();
	}
	
	/**
	 * Test simple decoding without exception
	 */
	@Test
	public void testLoginRequestDecoding() throws DecodeException {
		JsonDecoder decoder = new JsonDecoder();
		String rawMessage = "{'LoginRequest':{'organizationId':'DEMO1','userId':'a@example.com','password':'testme','messageId':'cid_4'}}".replace('\'', '"');
		MessageABC messageABC = decoder.decode(rawMessage);
		Assert.assertTrue(messageABC instanceof LoginRequest);
	}
	
	@Test
	public void testPointSerialization() throws DecodeException {
		Point point = new Point();
		ObjectMapper mapper = new ObjectMapper();
		String jsonString = "";
		try {
			jsonString = mapper.writeValueAsString(point);
			// System.out.println(jsonString);
		} catch (Exception e) {
			LOGGER.error("Failed to seriaize point", e);
			Assert.fail("Failed to seriaize point");
		}
		int pos = jsonString.indexOf("className");
		Assert.assertTrue(pos>=0);
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
			segment.setPosTypeEnum(PositionTypeEnum.METERS_FROM_PARENT);
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
		String rawMessage = "{'ObjectMethodRequest':{'messageId':'da76ec80-225c-11e4-8ce6-48d705ccef0f','className':'Organization','persistentId':'da76ec81-225c-11e4-8ce6-48d705ccef0f','methodName':'createFacility','methodArgs':[{'name':'domainId','value':'F1','classType':'java.lang.String'},{'name':'description','value':'First Facility','classType':'java.lang.String'},{'name':'anchorPoint','value':{'className':'Point','posTypeEnum':'METERS_FROM_PARENT','x':0.0,'y':0.0,'z':0.0},'classType':'com.gadgetworks.codeshelf.model.domain.Point'}]}}".replace('\'', '"');;
		MessageABC message = decoder.decode(rawMessage);
		Assert.assertTrue(message instanceof ObjectMethodRequest);
		ObjectMethodRequest objectMethodRequest = (ObjectMethodRequest) message;
		List<ArgsClass> args = objectMethodRequest.getMethodArgs();
		Object obj = args.get(2).getValue();
		Assert.assertTrue(obj instanceof Point);	
	}
	*/

}
