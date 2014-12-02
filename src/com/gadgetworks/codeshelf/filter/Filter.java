package com.gadgetworks.codeshelf.filter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

public class Filter extends Listener {

	@Setter
	ITypedDao<IDomainObject> dao;
	
	@Getter @Setter
	String criteriaName;
	
	@Getter @Setter
	Map<String,Object> params;
	
	public Filter(Class<IDomainObject> classObject, String id) {
		super(classObject, id);
	}
	
	public List<IDomainObject> refreshMatchList() {
		List<IDomainObject> objectMatchList = dao.findByFilterAndClass(criteriaName,params,dao.getDaoClass());
		List<UUID> objectIds = new LinkedList<UUID>();
		for (IDomainObject object : objectMatchList) {
			objectIds.add(object.getPersistentId());
		}
		this.setMatchList(objectIds);
		return objectMatchList;
	}
	
	@Override
	public ResponseABC processObjectAdd(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId) {
		ResponseABC result = null;
		if(domainClass.getSimpleName().equals(this.getPersistenceClass().getSimpleName())) {
			// TODO:???
			if (!this.matchList.contains(domainPersistentId)) {
				this.matchList.add(domainPersistentId);
			}
			result = super.processObjectAdd(domainClass, domainPersistentId);

			refreshMatchList();
		} 
		return result;
	}
}
