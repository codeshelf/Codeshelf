package com.gadgetworks.codeshelf.model.dao;

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.gadgetworks.codeshelf.model.persist.PersistABC;

public class GenericDaoTest {

	public class PersistABCStub extends PersistABC {
		// Stub object needed to test abstract class.
		// Ensures the Liskov Substitution Principle.
		
		public PersistABC getParent() {
			return null;
		}

		public void setParent(PersistABC inParent) {
			
		}
	}
	
	@Test
	public void testPushNonPersistentUpdates() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsObjectPersisted() {
		fail("Not yet implemented");
	}

	@Test
	public void testLoadByPersistentId() {
		fail("Not yet implemented");
	}

	@Test
	public void testFindById() {
		fail("Not yet implemented");
	}

	@Test
	public void testStore() {
		fail("Not yet implemented");
	}

	@Test
	public void testDelete() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetAll() {
		fail("Not yet implemented");
	}

}
