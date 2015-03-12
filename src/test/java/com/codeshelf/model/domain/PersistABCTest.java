package com.codeshelf.model.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.testframework.MinimalTest;
import com.eaio.uuid.UUIDGen;

public class PersistABCTest extends MinimalTest {
	
	@BeforeClass
	public final static void setup() {
	}

	class PersistABCStub extends DomainObjectABC {
		
		public PersistABCStub() {
		}

		public DomainObjectABC getParent() {
			return null;
		}

		public void setParent(DomainObjectABC inParent) {
			
		}

		@Override
		public String getDefaultDomainIdPrefix() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T extends IDomainObject> ITypedDao<T> getDao() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Facility getFacility() {
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
		public String getDefaultDomainIdPrefix() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T extends IDomainObject> ITypedDao<T> getDao() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Facility getFacility() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Test
	public void testGetId() {
		new PersistABCStub();
		assertEquals(IDomainObject.ID_PROPERTY, "domainId");
	}

	@Test
	public void testHashCode() {
		PersistABCStub persist = new PersistABCStub();
		
		//ABC sets default random UUID on init
		//assertEquals(persist.hashCode(), 0); 
		
		UUID testId = new UUID(UUIDGen.newTime(), UUIDGen.getClockSeqAndNode());
		persist.setPersistentId(testId);
		assertEquals(persist.hashCode(), testId.hashCode());
	}

	@Test
	public void testEquals() {
		PersistABCStub persist1 = new PersistABCStub();
		persist1.setPersistentId(new UUID(UUIDGen.newTime(), UUIDGen.getClockSeqAndNode()));
		PersistABCStub persist2 = new PersistABCStub();
		persist2.setPersistentId(new UUID(UUIDGen.newTime(), UUIDGen.getClockSeqAndNode()));
		PersistABCOtherStub persist3 = new PersistABCOtherStub();
		persist3.setPersistentId(new UUID(UUIDGen.newTime(), UUIDGen.getClockSeqAndNode()));
		assertTrue(persist1.equals(persist1));
		assertFalse(persist1.equals(persist2));
		assertFalse(persist1.equals(persist3));
		assertFalse(persist1.equals(String.class));
	}

}
