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
	String clause;
	
//	@Getter @Setter
//	List<Criterion> params;
	
	@Getter @Setter
	Map<String,Object> params;
	
	public Filter(Class<IDomainObject> classObject) {
		super(classObject);
	}
	
	public List<IDomainObject> refreshMatchList() {
		List<IDomainObject> objectMatchList = dao.findByFilterAndClass(clause,params,dao.getDaoClass());
		List<UUID> objectIds = new LinkedList<UUID>();
		for (IDomainObject object : objectMatchList) {
			objectIds.add(object.getPersistentId());
		}
		this.setMatchList(objectIds);
		return objectMatchList;
	}
	
	@Override
	public ResponseABC processObjectAdd(IDomainObject inDomainObject) {
		ResponseABC result = null;
		if(inDomainObject.getClassName().equals(this.getPersistenceClass().getSimpleName())) {
			// TODO:???
			if (!this.matchList.contains(inDomainObject.getPersistentId())) {
				this.matchList.add(inDomainObject.getPersistentId());
			}
			result = super.processObjectAdd(inDomainObject);

			//TODO: this might need to actually happen for filter to work right... 
			//refreshMatchList();
		} 
		return result;
	}
}
