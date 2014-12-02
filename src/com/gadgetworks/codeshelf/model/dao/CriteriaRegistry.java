package com.gadgetworks.codeshelf.model.dao;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.google.common.collect.Maps;


public class CriteriaRegistry {
	
	private Map<String, TypedCriteria> indexedCriteria;
	

	private CriteriaRegistry() {
		indexedCriteria = Maps.newHashMap();
		
		indexedCriteria.put("workInstructionByCheAndAssignedTime", 
			new TypedCriteria("from WorkInstruction where assignedChe.persistentId = :cheId and assigned = :assignedTimestamp", 
					"cheId", UUID.class,
					"assignedTimestamp", Timestamp.class));
	
		indexedCriteria.put("workInstructionBySku",
			new TypedCriteria("from WorkInstruction where itemMaster.domainId = :sku", "sku", String.class));
	
		indexedCriteria.put("workInstructionsByFacility",
			new TypedCriteria("from WorkInstruction where parent.persistentId = :facilityId and status != 'COMPLETE'",
				"facilityId", UUID.class));

		indexedCriteria.put("cheByFacility",
			new TypedCriteria("from Che where parent.parent.persistentId = :facilityId",
				"facilityId", UUID.class));
		
		indexedCriteria.put("pathSegmentsByFacility",
			new TypedCriteria("from PathSegment where parent.parent.persistentId = :facilityId",
				"facilityId", UUID.class));

		indexedCriteria.put("pathSegmentsByPath",
			new TypedCriteria("from PathSegment where parent.persistentId = :theId",
				"theId", UUID.class));
		
		indexedCriteria.put("ledControllersByFacility",
			new TypedCriteria("from LedController where parent.parent.persistentId = :facilityId",
				"facilityId", UUID.class));

		indexedCriteria.put("itemsByFacility",
			new TypedCriteria("from Item where parent.parent.persistentId = :facilityId and active = true",
				"facilityId", UUID.class));

		indexedCriteria.put("baysByFacility",
			new TypedCriteria("from Bay where parent.parent.persistentId = :facilityId and active = true",
				"facilityId", UUID.class));

		indexedCriteria.put("containerUsesByFacility",
			new TypedCriteria("from ContainerUse where parent.parent.persistentId = :facilityId and active = true",
				"facilityId", UUID.class));

		indexedCriteria.put("containerUsesByChe",
			new TypedCriteria("from ContainerUse where 	current_che_persistentid = :cheId  and active = true",
				"cheId", UUID.class));

		indexedCriteria.put("baysByZeroPosition",
			new TypedCriteria("from Bay where parent.persistentId = :parentId AND anchorPosZ = 0",
				"parentId", UUID.class));

		indexedCriteria.put("itemsByFacilityAndLocation",
			new TypedCriteria("from Item where parent.parent.persistentId = :facilityId and active = true and ((storedLocation.persistentId = :locationId) or (storedLocation.parent is not null and ((storedLocation.parent.persistentId = :theLocationId) or (storedLocation.parent.parent is not null and (storedLocation.parent.parent.persistentId = :locationId))))",
				"facilityId", UUID.class,
				"locationId", UUID.class));

		indexedCriteria.put("itemsByFacilityAndSku",
			new TypedCriteria("from Item where parent.parent.persistentId = :facilityId and active = true and parent.domainId = :sku",
				"facilityId", UUID.class,
				"sku", String.class));

		indexedCriteria.put("tiersByFacility",
			new TypedCriteria("from Tier where parent.parent.parent.persistentId = :facilityId and active = true",
				"facilityId", UUID.class));
		
		indexedCriteria.put("tiersByAisle",
			new TypedCriteria("from Tier where parent.parent.persistentId = :aisleId and active = true",
				"aisleId", UUID.class));
		
		indexedCriteria.put("orderDetailsByFacility",
			new TypedCriteria("from OrderDetail where parent.parent.persistentId = :facilityId and status <> 'COMPLETE'",
				"facilityId", UUID.class));

		indexedCriteria.put("orderDetailsByHeader",
			new TypedCriteria("from OrderDetail where parent.persistentId = :theId and status <> 'COMPLETE'",
				"theId", UUID.class)); //the UI dynamically sets the "parent" with theId

		
		indexedCriteria.put("orderHeadersByFacilityAndType",
			new TypedCriteria("from OrderHeader where status <> 'COMPLETE' and active = true and parent.persistentId = :facilityId and orderType = :orderType",
				"facilityId", UUID.class,
				"orderType", OrderTypeEnum.class));
		
		indexedCriteria.put("orderHeadersByGroupAndType",
			new TypedCriteria("from OrderHeader where status <> 'COMPLETE' and active = true and orderGroup.persistentId = :theId and orderType = :orderType",
				"theId", UUID.class, //the UI dynamically sets the "parent" with theId
				"orderType", OrderTypeEnum.class));
		
	}

	public TypedCriteria findByName(String name, Class<?> selectClass) {
		if (name.equals("allByParent")) {
			String query = String.format("from %s where parent.persistentId = :parentId", selectClass.getSimpleName());
			return new TypedCriteria(query, "parentId", UUID.class);
		} else if (name.equals("allActiveByParent")){
			String query = String.format("from %s where parent.persistentId = :parentId and active = true", selectClass.getSimpleName());
			return new TypedCriteria(query, "parentId", UUID.class);
		} else {
			return indexedCriteria.get(name);
		}
	}
	
	//Josh Bloch singleton pattern
	public static CriteriaRegistry getInstance() {
	    return CriteriaRegistryHolder.instance;
	}

	private static final class CriteriaRegistryHolder {
		static final CriteriaRegistry instance = new CriteriaRegistry();    
	}
	
	
}
