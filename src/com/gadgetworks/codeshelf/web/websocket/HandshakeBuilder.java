package com.gadgetworks.codeshelf.web.websocket;

public interface HandshakeBuilder extends Handshakedata {

	void setContent(byte[] content);

	void setResourceDescriptor(String resourcedescriptor);

	void setHttpStatusMessage(String message);

	void put(String name, String value);

}