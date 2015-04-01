package com.codeshelf.ws.server;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class WebSocketConfigurator extends ServerEndpointConfig.Configurator {

	@Override
	public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
		// does nothing: super.modifyHandshake(sec, request, response);
		
		// This works to make cookie available to endpoint, but I am not sure it is 
		// threadsafe, since the SEC is *not* per session.
		// Token in URL/query parameter might be a better approach for jetty websockets.
		
        //HttpSession httpSession = (HttpSession)request.getHttpSession();
        //Map<String, List<String>> headers = request.getHeaders();
        //sec.getUserProperties().put("headers",headers);
	}

}
