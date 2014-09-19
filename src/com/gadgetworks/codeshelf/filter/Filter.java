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
	
	@Getter @Setter
	Map<String,Object> params;
	
	public Filter(Class<IDomainObject> classObject) {
		super(classObject);
	}
	
	public List<IDomainObject> refreshMatchList() {
		List<IDomainObject> objectMatchList = dao.findByFilter(params);
		List<UUID> objectIds = new LinkedList<UUID>();
		for (IDomainObject object : objectMatchList) {
			objectIds.add(object.getPersistentId());
		}
		this.setMatchList(objectIds);
		return objectMatchList;
	}
	
	@Override
	public ResponseABC processObjectAdd(IDomainObject inDomainObject) {
		refreshMatchList();
		return super.processObjectAdd(inDomainObject);
	}
}
