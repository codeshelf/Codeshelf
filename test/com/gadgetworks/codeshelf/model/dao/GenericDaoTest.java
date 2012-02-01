package com.gadgetworks.codeshelf.model.dao;

import static org.junit.Assert.*;

import org.junit.Test;

import com.gadgetworks.codeshelf.model.persist.PersistABC;

public class GenericDaoTest {

	class PersistABCStub extends PersistABC {
		// Stub object needed to test abstract class.
		// Ensures the Liskov Substitution Principle.
	}

	@Test
	public void testPushNonPersistentUpdates() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitCacheMap() {
		GenericDao<PersistABCStub> generic = new GenericDao<PersistABCStub>(PersistABCStub.class);
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
