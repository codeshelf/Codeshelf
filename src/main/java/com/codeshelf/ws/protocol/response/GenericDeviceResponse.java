package com.codeshelf.ws.protocol.response;

/**
 * Why? So a command may choose to do a generic response, without instantiating an ABC class
 * Then the WARN "Failed to handle response" in SiteControllerMessageProcessor is meaningful
 */
public class GenericDeviceResponse extends DeviceResponseABC{
	public GenericDeviceResponse() {
	}
}
