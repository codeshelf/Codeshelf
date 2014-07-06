package com.gadgetworks.codeshelf.ws.jetty.test;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.ws.jetty.client.JettyWebSocketClient;
import com.gadgetworks.codeshelf.ws.jetty.client.LogResponseProcessor;
import com.gadgetworks.codeshelf.ws.jetty.client.ResponseProcessor;
import com.gadgetworks.codeshelf.ws.jetty.request.EchoRequest;
import com.gadgetworks.codeshelf.ws.jetty.request.LoginRequest;

public class JettyTestClient {

	// private static final Logger	LOGGER = LoggerFactory.getLogger(JettyTestClient.class);

    public static void main(String[] args) {
		System.setProperty("console.appender","org.apache.log4j.ConsoleAppender");
		Util util = new Util();
		
		try {
    		// create WS client
        	JettyWebSocketClient client = new JettyWebSocketClient("ws://localhost:8444/");
        	
        	// create response processor and register it with WS client
        	ResponseProcessor responseProcessor = new LogResponseProcessor();
        	client.setResponseProcessor(responseProcessor);

    		EchoRequest genericRequest = new EchoRequest("Hello!");
    		client.sendRequest(genericRequest);
    		
        	// create a login request and send it to the server
    		LoginRequest loginRequest = new LoginRequest("DEMO1","a@example.com","testme");
    		client.sendRequest(loginRequest);
    		
    		Thread.sleep(10000);
    	}
    	catch (Exception e) {
    		// LOGGER.error("Failed to process test interactions.",e);
    		e.printStackTrace();
    	}
    }
}