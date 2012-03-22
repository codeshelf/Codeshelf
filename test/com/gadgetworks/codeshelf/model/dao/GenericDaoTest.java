package com.gadgetworks.codeshelf.model.dao;

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.gadgetworks.codeshelf.model.persist.PersistABC;

public class GenericDaoTest {

	class PersistABCStub extends PersistABC {
		// Stub object needed to test abstract class.
		// Ensures the Liskov Substitution Principle.
	}
	
	class DaoRegistryStub implements IDaoRegistry {

		@Override
		public void addDao(IDao inDao) {
		}

		@Override
		public List<IDao> getDaoList() {
			return null;
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
