package com.codeshelf.ws.jetty.test;

import java.util.Arrays;

import com.codeshelf.ws.jetty.client.CsClientEndpoint;
import com.codeshelf.ws.jetty.client.LogResponseProcessor;
import com.codeshelf.ws.jetty.io.CompressedJsonMessage;
import com.codeshelf.ws.jetty.protocol.message.IMessageProcessor;
import com.codeshelf.ws.jetty.protocol.request.EchoRequest;
import com.codeshelf.ws.jetty.protocol.request.LoginRequest;

public class JettyTestClient {

	// private static final Logger	LOGGER = LoggerFactory.getLogger(JettyTestClient.class);

    public static void main(String[] args) {
		//System.setProperty("console.appender","org.apache.log4j.ConsoleAppender");
		
		try {
    		// create WS client
        	IMessageProcessor responseProcessor = new LogResponseProcessor();
        	
        	CsClientEndpoint.setMessageProcessor(responseProcessor);
        	CsClientEndpoint.setEventListener(null); //optional
        	CsClientEndpoint client = new CsClientEndpoint();
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