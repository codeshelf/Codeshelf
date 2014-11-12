package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.UUID;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;

/**
 * 
 * This class primarily exists to have an IDomainObject  in the correct package for unit testing.
 * 
 *
 */
public class MockModel implements IDomainObject {

	public static ITypedDao<MockModel>	DAO;

	private String testSetterString;
	private int testSetterInt;
	private double testSetterDouble;
	private boolean testSetterBoolean;
	
	@Override
	public String getDefaultDomainIdPrefix() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDomainId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDomainId(String inId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getClassName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UUID getPersistentId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPersistentId(UUID inPersistentId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Timestamp getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setVersion(Timestamp inVersion) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T extends IDomainObject> ITypedDao<T> getDao() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void testMethod(String testParam) throws Exception {
		
	}
	
	public void setTestSetterString(String value) {
		this.testSetterString = value;
	}

	public String getTestSetterString() {
		return this.testSetterString;
		
	}

	
	public int getTestSetterInt() {
		return testSetterInt;
	}

	public void setTestSetterInt(int testSetterInt) {
		this.testSetterInt = testSetterInt;
	}

	public double getTestSetterDouble() {
		return testSetterDouble;
	}

	public void setTestSetterDouble(double testSetterDouble) {
		this.testSetterDouble = testSetterDouble;
	}

	public boolean getTestSetterBoolean() {
		return testSetterBoolean;
	}

	public void setTestSetterBoolean(boolean testSetterBoolean) {
		this.testSetterBoolean = testSetterBoolean;
	}
	
	@Override
	public final Facility getFacility() {
		return null;
	}

	@Override
	public Organization getOrganization() {
		return null;
	}


}
