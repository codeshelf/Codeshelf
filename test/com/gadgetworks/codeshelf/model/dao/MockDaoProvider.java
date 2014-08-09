package com.gadgetworks.codeshelf.model.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;

public class MockDaoProvider implements IDaoProvider {

	Map<Class<? extends IDomainObject>,ITypedDao<? extends IDomainObject>> daos = new HashMap<Class<? extends IDomainObject>,ITypedDao<? extends IDomainObject>>();
	
	public MockDaoProvider() {
		ITypedDao<Organization> organizationDao = new MockDao<Organization>();
		daos.put(Organization.class, organizationDao);

		ITypedDao<User> userDao = new MockDao<User>();
		daos.put(User.class, userDao);
		
		/*
		ITypedDao<User> userDao = new MockDao<User>();
		ITypedDao<Che> cheDao = new MockDao<Che>();
		ITypedDao<WorkInstruction> workInstructionDao = new MockDao<WorkInstruction>();
		ITypedDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		ITypedDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		*/
	}
	
	@Override
	public <T extends IDomainObject> ITypedDao<T> getDaoInstance(Class<T> inDomainObjectClass) {
		ITypedDao<? extends IDomainObject> dao = this.daos.get(inDomainObjectClass);
		return (ITypedDao<T>)dao;
	}

	@Override
	public <T extends IDomainObject> List<ITypedDao<T>> getAllDaos() {
		return null;
	}

}
