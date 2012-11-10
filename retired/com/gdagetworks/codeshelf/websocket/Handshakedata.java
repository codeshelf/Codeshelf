package com.gadgetworks.codeshelf.web.websocket;

import java.util.Iterator;

public interface Handshakedata {
	String getHttpStatusMessage();

	String getResourceDescriptor();

	Iterator<String> iterateHttpFields();

	String getFieldValue(String name);

	boolean hasFieldValue(String name);

	byte[] getContent();
	
	//  boolean isComplete();
}
