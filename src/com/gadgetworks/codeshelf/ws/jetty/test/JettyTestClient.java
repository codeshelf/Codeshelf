package com.gadgetworks.codeshelf.ws.jetty.test;

import java.net.URI;
import java.util.Arrays;

import com.gadgetworks.codeshelf.ws.jetty.client.JettyWebSocketClient;
import com.gadgetworks.codeshelf.ws.jetty.client.LogResponseProcessor;
import com.gadgetworks.codeshelf.ws.jetty.io.CompressedJsonMessage;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.EchoRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.LoginRequest;

public class JettyTestClient {

	// private static final Logger	LOGGER = LoggerFactory.getLogger(JettyTestClient.class);

    public static void main(String[] args) {
		System.setProperty("console.appender","org.apache.log4j.ConsoleAppender");
		
		// init keystore and trust store
		System.setProperty("javax.net.ssl.keyStore", "conf/localhost.jks");
		System.setProperty("javax.net.ssl.keyStorePassword", "1qazse4");
		System.setProperty("javax.net.ssl.trustStore", "conf/localhost.jks");
		System.setProperty("javax.net.ssl.trustStorePassword", "1qazse4");
		System.setProperty("javax.net.ssl.trustStoreType", "JKS");
		// System.setProperty("javax.net.debug", "ssl");
		
		try {
    		// create WS client
        	MessageProcessor responseProcessor = new LogResponseProcessor();
        	JettyWebSocketClient client = new JettyWebSocketClient(URI.create("wss://localhost:8444/"),responseProcessor,null);
        	client.connect();
        	//Message that shouldn't compress
        	char[] charBuf = new char[CompressedJsonMessage.JSON_COMPRESS_MAXIMUM - 1];
        	Arrays.fill(charBuf, '{');
    		EchoRequest genericRequest = new EchoRequest(new String(charBuf));
    		client.sendMessage(genericRequest);
    		
        	// create a login request and send it to the server
    		LoginRequest loginRequest = new LoginRequest("a@example.com","testme");
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