package com.codeshelf.ws.jetty.protocol.command;

import org.junit.Test;

import com.codeshelf.testframework.MinimalTest;
import com.codeshelf.ws.jetty.protocol.request.ObjectMethodRequest;

public class APITest extends MinimalTest {

	@Test
	public void testUpsertItem() {
		ObjectMethodRequest methodRequest = new ObjectMethodRequest();
		methodRequest.setClassName("Facility");

	}
	
}
