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

	@Override
	public Object getFieldValueByName(String inFieldName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFieldValueByName(String inFieldName, Object inFieldValue) {
		// TODO Auto-generated method stub
		
	}
	
	public void testMethod(String testParam) throws Exception {
		
	}

}
