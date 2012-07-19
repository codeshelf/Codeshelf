package com.gadgetworks.codeshelf.model.persist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.domain.DomainObjectABC;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;

public class PersistABCTest {
	
	class PersistABCStub extends DomainObjectABC {
		
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
	
	class PersistABCOtherStub extends DomainObjectABC {

		public PersistABCOtherStub() {
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
	public void testGetId() {
		PersistABCStub persist = new PersistABCStub();
		assertEquals(IDomainObject.ID_COLUMN_NAME, "Id");
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
