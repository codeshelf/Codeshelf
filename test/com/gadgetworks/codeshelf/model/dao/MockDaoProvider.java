package com.gadgetworks.codeshelf.model.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.ContainerKind;
import com.gadgetworks.codeshelf.model.domain.DropboxService;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.model.domain.Vertex;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.model.domain.Vertex.VertexDao;

public class MockDaoProvider implements IDaoProvider {

	Map<Class<? extends IDomainObject>,ITypedDao<? extends IDomainObject>> daos = new HashMap<Class<? extends IDomainObject>,ITypedDao<? extends IDomainObject>>();
	
	public MockDaoProvider() {
		ITypedDao<Organization> organizationDao = new MockDao<Organization>();
		daos.put(Organization.class, organizationDao);

		ITypedDao<Facility> facilityDao = new MockDao<Facility>();
		daos.put(Facility.class, facilityDao);

		ITypedDao<CodeshelfNetwork> networkDao = new MockDao<CodeshelfNetwork>();
		daos.put(CodeshelfNetwork.class, networkDao);

		ITypedDao<User> userDao = new MockDao<User>();
		daos.put(User.class, userDao);
		
		ITypedDao<Che> cheDao = new MockDao<Che>();
		daos.put(Che.class, cheDao);

		ITypedDao<WorkInstruction> workInstructionDao = new MockDao<WorkInstruction>();
		daos.put(WorkInstruction.class, workInstructionDao);

		ITypedDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		daos.put(OrderHeader.class, orderHeaderDao);

		ITypedDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		daos.put(OrderDetail.class, orderDetailDao);

		ITypedDao<DropboxService> dropboxDao = new MockDao<DropboxService>();
		daos.put(DropboxService.class, dropboxDao);
		
		ITypedDao<ContainerKind> containerKindDao = new MockDao<ContainerKind>();
		daos.put(ContainerKind.class, containerKindDao);

		ITypedDao<Vertex> vertexDao = new MockDao<Vertex>();
		daos.put(Vertex.class, vertexDao);
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
