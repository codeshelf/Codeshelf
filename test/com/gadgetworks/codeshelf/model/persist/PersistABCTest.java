package com.gadgetworks.codeshelf.model.persist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class PersistABCTest {
	
	class PersistABCStub extends PersistABC {
		// Stub object needed to test abstract class.
		// Ensures the Liskov Substitution Principle.
	}
	
	class PersistABCOtherStub extends PersistABC {
		// Stub object needed to test abstract class.
		// Ensures the Liskov Substitution Principle.
	}

	@Test
	public void testHashCode() {
		PersistABCStub persist = new PersistABCStub();
		assertEquals(persist.hashCode(), 0);
		Long testId = 9999L;
		persist.setPersistentId(testId);
		assertEquals(persist.hashCode(), testId.hashCode());
	}

	@Test
	public void testEquals() {
		PersistABCStub persist1 = new PersistABCStub();
		persist1.setPersistentId(1L);
		PersistABCStub persist2 = new PersistABCStub();
		persist2.setPersistentId(2L);
		PersistABCOtherStub persist3 = new PersistABCOtherStub();
		persist3.setPersistentId(3L);
		assertTrue(persist1.equals(persist1));
		assertFalse(persist1.equals(persist2));
		assertFalse(persist1.equals(persist3));
		assertFalse(persist1.equals(String.class));
	}

}
