package com.codeshelf.ws.jetty.protocol.command;

import org.junit.Test;

import com.codeshelf.ws.jetty.protocol.request.ObjectMethodRequest;

public class APITest {

	@Test
	public void testUpsertItem() {
		ObjectMethodRequest methodRequest = new ObjectMethodRequest();
		methodRequest.setClassName("Facility");

	}
	
}