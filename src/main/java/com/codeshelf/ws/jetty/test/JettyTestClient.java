package com.codeshelf.ws.jetty.test;

import java.net.URI;
import java.util.Arrays;

import javax.websocket.ContainerProvider;

import com.codeshelf.ws.jetty.client.JettyWebSocketClient;
import com.codeshelf.ws.jetty.client.LogResponseProcessor;
import com.codeshelf.ws.jetty.io.CompressedJsonMessage;
import com.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.codeshelf.ws.jetty.protocol.request.EchoRequest;
import com.codeshelf.ws.jetty.protocol.request.LoginRequest;

public class JettyTestClient {

	// private static final Logger	LOGGER = LoggerFactory.getLogger(JettyTestClient.class);

    public static void main(String[] args) {
		System.setProperty("console.appender","org.apache.log4j.ConsoleAppender");
		
		try {
    		// create WS client
        	MessageProcessor responseProcessor = new LogResponseProcessor();
        	JettyWebSocketClient client = new JettyWebSocketClient(ContainerProvider.getWebSocketContainer(), URI.create("ws://localhost:"+Integer.getInteger("api.port")+"/ws/"),responseProcessor,null);
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