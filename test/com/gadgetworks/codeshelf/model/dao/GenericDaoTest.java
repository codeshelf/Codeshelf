package com.gadgetworks.codeshelf.model.dao;

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.gadgetworks.codeshelf.model.domain.DomainObjectABC;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;

public class GenericDaoTest {

	public class PersistABCStub extends DomainObjectABC {
		// Stub object needed to test abstract class.
		// Ensures the Liskov Substitution Principle.
		
		
		public PersistABCStub() {
		}

		public DomainObjectABC getParent() {
			return null;
		}

		public void setParent(DomainObjectABC inParent) {
			
		}

		@Override
		public void setParent(IDomainObject inParent) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public List<IDomainObject> getChildren() {
			// TODO Auto-generated method stub
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
