package com.gadgetworks.codeshelf.ws.jetty;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

public class MockSession implements Session {

	String id;
	
	@Override
	public void addMessageHandler(MessageHandler arg0) throws IllegalStateException {
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public void close(CloseReason arg0) throws IOException {
	}

	@Override
	public Async getAsyncRemote() {
		return null;
	}

	@Override
	public Basic getBasicRemote() {
		return null;
	}

	@Override
	public WebSocketContainer getContainer() {
		return null;
	}

	@Override
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public int getMaxBinaryMessageBufferSize() {
		return 0;
	}

	@Override
	public long getMaxIdleTimeout() {
		return 0;
	}

	@Override
	public int getMaxTextMessageBufferSize() {
		return 0;
	}

	@Override
	public Set<MessageHandler> getMessageHandlers() {
		return null;
	}

	@Override
	public List<Extension> getNegotiatedExtensions() {
		return null;
	}

	@Override
	public String getNegotiatedSubprotocol() {
		return null;
	}

	@Override
	public Set<Session> getOpenSessions() {
		return null;
	}

	@Override
	public Map<String, String> getPathParameters() {
		return null;
	}

	@Override
	public String getProtocolVersion() {
		return null;
	}

	@Override
	public String getQueryString() {
		return null;
	}

	@Override
	public Map<String, List<String>> getRequestParameterMap() {
		return null;
	}

	@Override
	public URI getRequestURI() {
		return null;
	}

	@Override
	public Principal getUserPrincipal() {
		return null;
	}

	@Override
	public Map<String, Object> getUserProperties() {
		return null;
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public void removeMessageHandler(MessageHandler arg0) {
	}

	@Override
	public void setMaxBinaryMessageBufferSize(int arg0) {
	}

	@Override
	public void setMaxIdleTimeout(long arg0) {
	}

	@Override
	public void setMaxTextMessageBufferSize(int arg0) {
	}

}
