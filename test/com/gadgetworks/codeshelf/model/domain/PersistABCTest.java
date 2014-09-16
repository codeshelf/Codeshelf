package com.gadgetworks.codeshelf.model.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import com.eaio.uuid.UUIDGen;
import com.gadgetworks.codeshelf.model.dao.Database;
import com.gadgetworks.codeshelf.model.dao.H2SchemaManager;
import com.gadgetworks.codeshelf.model.dao.IDatabase;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;

public class PersistABCTest {

	private static ISchemaManager	mSchemaManager;
	private static IDatabase		mDatabase;

	@BeforeClass
	public final static void setup() {

		try {

			Class.forName("org.h2.Driver");
			mSchemaManager = new H2SchemaManager("codeshelf", "codeshelf", "codeshelf", "CODESHELF", "localhost", "");
			mDatabase = new Database(mSchemaManager);

			mDatabase.start();
		} catch (ClassNotFoundException e) {
		}
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
	}

	@Test
	public void testGetId() {
		new PersistABCStub();
		assertEquals(IDomainObject.ID_PROPERTY, "domainId");
	}

	@Test
	public void testHashCode() {
		PersistABCStub persist = new PersistABCStub();
		assertEquals(persist.hashCode(), 0);
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
