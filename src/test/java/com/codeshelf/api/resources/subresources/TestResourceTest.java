package com.codeshelf.api.resources.subresources;

import java.lang.reflect.InvocationTargetException;

import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.api.ParameterUtils;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.service.TestBehavior;
import com.sun.jersey.api.representation.Form;

public class TestResourceTest {

	@Test
	public void testCallFunction() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Facility mockFacility = Mockito.mock(Facility.class);
		Form form = new Form();
		form.add("ordersPerChe", "3");
		form.add("ches", "CHE1 CHE2 CHE3");
		
		String testAnswer = "An Answer";
		
		TestBehavior mockTestBehavior = Mockito.mock(TestBehavior.class);
		
		Mockito.when(mockTestBehavior.setupManyCartsWithOrders(Mockito.eq(mockFacility), Mockito.eq(ParameterUtils.toMapOfFirstValues(form)))).thenReturn(testAnswer);
		TestResource subject = new TestResource();
		subject.setFacility(mockFacility);
		subject.setTestBehavior(mockTestBehavior);
		Response response = subject.callFunction("setupManyCartsWithOrders", form);
		Assert.assertEquals(testAnswer, response.getEntity());
	}

	@Test
	public void testBadFunctionName() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		TestResource subject = new TestResource();
		Form form = new Form();
		try {
			@SuppressWarnings("unused")
			Response response = subject.callFunction("notFound", form);
			Assert.fail("Should have thrown exception");
		}
		catch(NoSuchMethodException e) {
			
		}
		
	}
	
}
