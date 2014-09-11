package com.gadgetworks.codeshelf.ws.jetty.test;

import java.util.Arrays;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.ws.jetty.client.JettyWebSocketClient;
import com.gadgetworks.codeshelf.ws.jetty.client.LogResponseProcessor;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonEncoder;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.EchoRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.LoginRequest;

public class JettyTestClient {

	// private static final Logger	LOGGER = LoggerFactory.getLogger(JettyTestClient.class);

    public static void main(String[] args) {
		System.setProperty("console.appender","org.apache.log4j.ConsoleAppender");
		Util util = new Util();
		
		// init keystore and trust store
		System.setProperty("javax.net.ssl.keyStore", "/etc/codeshelf.keystore");
		System.setProperty("javax.net.ssl.keyStorePassword", "x2HPbC2avltYQR");
		System.setProperty("javax.net.ssl.trustStore", "/etc/codeshelf.keystore");
		System.setProperty("javax.net.ssl.trustStorePassword", "x2HPbC2avltYQR");
		System.setProperty("javax.net.ssl.trustStoreType", "JKS");
		// System.setProperty("javax.net.debug", "ssl");
		
		try {
    		// create WS client
        	MessageProcessor responseProcessor = new LogResponseProcessor();
        	JettyWebSocketClient client = new JettyWebSocketClient("wss://localhost:8444/",responseProcessor,null);
        	client.connect();
        	//Message that shouldn't compress
        	char[] charBuf = new char[JsonEncoder.JSON_COMPRESS_MAXIMUM - 1];
        	Arrays.fill(charBuf, '{');
    		EchoRequest genericRequest = new EchoRequest(new String(charBuf));
    		client.sendMessage(genericRequest);
    		
        	// create a login request and send it to the server
    		LoginRequest loginRequest = new LoginRequest("DEMO1","a@example.com","testme");
    		client.sendMessage(loginRequest);
    		
    		Thread.sleep(10000);
    		
    		client.disconnect();
    	}
    	catch (Exception e) {
    		// LOGGER.error("Failed to process test interactions.",e);
    		e.printStackTrace();
    	}
    }
}