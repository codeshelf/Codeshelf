package com.gadgetworks.codeshelf.model.dao;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;


public class CriteriaRegistry {
	
	private Map<String, TypedCriteria> indexedCriteria;
	

	private CriteriaRegistry() {
		indexedCriteria = Maps.newHashMap();
		
		indexedCriteria.put("workInstructionByCheAndAssignedTime", 
			new TypedCriteria("from WorkInstruction where assignedChe.persistentId = :che_persistentId and assigned = :assigned", 
					"che_persistentId", UUID.class,
					"assigned", Timestamp.class));
	
		indexedCriteria.put("workInstructionByItem",
			new TypedCriteria("from WorkInstruction where itemMaster = :theId", "theId", UUID.class));
	
		indexedCriteria.put("allWorkInstructions",
			new TypedCriteria("from WorkInstruction where parent.persistentId = :theId and statusEnum != 'COMPLETE'",
				"theId", UUID.class));

		indexedCriteria.put("allAisles",
			new TypedCriteria("from where Aisle parent.persistentId = :theId and active = true", "theId", UUID.class));

	}

	public TypedCriteria findByName(String name) {
		return indexedCriteria.get(name);
	}
	
	//Josh Bloch singleton pattern
	public static CriteriaRegistry getInstance() {
	    return CriteriaRegistryHolder.instance;
	}

	private static final class CriteriaRegistryHolder {
		static final CriteriaRegistry instance = new CriteriaRegistry();    
	}
	
	
}
