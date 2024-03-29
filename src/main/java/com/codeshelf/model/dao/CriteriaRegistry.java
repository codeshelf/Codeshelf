package com.codeshelf.model.dao;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import com.codeshelf.model.OrderTypeEnum;
import com.google.common.collect.Maps;


public class CriteriaRegistry {

	public static final String	ALL_BY_PARENT	= "allByParent";
	public static final String	ALL_ACTIVE_BY_PARENT	= "allActiveByParent";
	private static final int	HIGH_MAX_RECORDS	= 5000;
	private static final int	MED_MAX_RECORDS	= 2000;


	private Map<String, TypedCriteria> indexedCriteria;


	private CriteriaRegistry() {
		indexedCriteria = Maps.newHashMap();


		indexedCriteria.put("orderDetailByFacilityAndDomainId",
			new TypedCriteria("from OrderDetail where active = true and parent.parent.persistentId = :facilityId and domainId = :domainId",
					"facilityId", UUID.class,
					"domainId", String.class));

		indexedCriteria.put("workInstructionByCheAndAssignedTime",
			new TypedCriteria("from WorkInstruction where assignedChe.persistentId = :cheId and assigned = :assignedTimestamp",
					"cheId", UUID.class,
					"assignedTimestamp", Timestamp.class));

		// the "assignedTimestamp" is the previous midnight, so we want assigned time > that that, and < that + 24 hours
		// And, we are using completed, not assigned time from the database.
		indexedCriteria.put("workInstructionByCheAndDay",
			new TypedCriteria("from WorkInstruction where assignedChe.persistentId = :cheId and (completed = null OR (date(completed) = date(:assignedTimestamp)))",
				"cheId", UUID.class,
				"assignedTimestamp", Timestamp.class));



		indexedCriteria.put("workInstructionByCheAll",
			new TypedCriteria("from WorkInstruction where assignedChe.persistentId = :cheId",
					"cheId", UUID.class));

		indexedCriteria.put("workInstructionBySku",
			new TypedCriteria("from WorkInstruction where itemMaster.domainId = :sku", "sku", String.class));

		indexedCriteria.put("workInstructionsByDetail",
			new TypedCriteria("from WorkInstruction where orderDetail.persistentId = :orderDetail",
				"orderDetail", UUID.class));

		indexedCriteria.put("workInstructionsByHeader",
			new TypedCriteria("from WorkInstruction where orderDetail.parent.persistentId = :orderHeader",
				"orderHeader", UUID.class));

		indexedCriteria.put("workInstructionsByFacility",
			new TypedCriteria("from WorkInstruction where parent.persistentId = :facilityId and status != 'COMPLETE'",
				"facilityId", UUID.class));

		indexedCriteria.put("cheByFacility",
			new TypedCriteria("from Che where parent.parent.persistentId = :facilityId",
				"facilityId", UUID.class));
		
		indexedCriteria.put("siteControllersByFacility",
			new TypedCriteria("from SiteController where parent.parent.persistentId = :facilityId",
				"facilityId", UUID.class));

		indexedCriteria.put("locationAliasesByFacility",
			new TypedCriteria("from LocationAlias where parent.persistentId = :facilityId and active = true",
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
			new TypedCriteria("from ContainerUse where currentChe.persistentId = :cheId  and active = true",
				"cheId", UUID.class));

		indexedCriteria.put("baysByZeroPosition",
			new TypedCriteria("from Bay where parent.persistentId = :parentId AND anchorPosZ = 0",
				"parentId", UUID.class));

		indexedCriteria.put("itemsByFacilityAndLocation",
			new TypedCriteria("from Item where parent.parent.persistentId = :facilityId and active = true and ((storedLocation.persistentId = :locationId) or (storedLocation.parent is not null and ((storedLocation.parent.persistentId = :locationId) or (storedLocation.parent.parent is not null and (storedLocation.parent.parent.persistentId = :locationId))or (storedLocation.parent.parent.parent is not null and (storedLocation.parent.parent.parent.persistentId = :locationId)))))",
				"facilityId", UUID.class,
				"locationId", UUID.class));

		TypedCriteria  itemsByFacilityAndSku = new TypedCriteria("from Item where parent.parent.persistentId = :facilityId and active = true and parent.domainId = :sku",
			"facilityId", UUID.class,
			"sku", String.class);
		itemsByFacilityAndSku.setMaxRecords(MED_MAX_RECORDS);
		indexedCriteria.put("itemsByFacilityAndSku", itemsByFacilityAndSku);

		TypedCriteria  itemsByFacilityAndGtin = new TypedCriteria("from Item where parent.parent.persistentId = :facilityId and active = true and parent.persistentId = :itemMaster",
			"facilityId", UUID.class,
			"itemMaster", UUID.class).addEqualsRestriction("uomMaster.persistentId", "uomMaster", UUID.class);
		itemsByFacilityAndGtin.setMaxRecords(MED_MAX_RECORDS);
		indexedCriteria.put("itemsByFacilityAndGtin", itemsByFacilityAndGtin);

		indexedCriteria.put("tiersByFacility",
			new TypedCriteria("from Tier where parent.parent.parent.persistentId = :facilityId and active = true",
				"facilityId", UUID.class));

		indexedCriteria.put("slotsByFacility",
			new TypedCriteria("from Slot where parent.parent.parent.parent.persistentId = :facilityId and active = true",
				"facilityId", UUID.class));

		indexedCriteria.put("tiersByAisle",
			new TypedCriteria("from Tier where parent.parent.persistentId = :aisleId and active = true",
				"aisleId", UUID.class));

		TypedCriteria  orderDetailsByFacility = new TypedCriteria("from OrderDetail where parent.parent.persistentId = :facilityId and active = true",
			"facilityId", UUID.class);
		orderDetailsByFacility.setMaxRecords(HIGH_MAX_RECORDS);
		indexedCriteria.put("orderDetailsByFacility", orderDetailsByFacility);

		// earlier experiment with shipper and customer did not work. Just too many.
		// order number or itemId. Would be nice to do gtin, but that would require a complicated join as gtin is a function of itemMaster and uom.
		TypedCriteria orderDetailsByFacilityAndPartialQuery = new TypedCriteria("from OrderDetail where parent.parent.persistentId = :facilityId "
				+ " and active = true"
				+ " and (parent.domainId      LIKE :partialQuery "
				+ " or itemMaster.domainId  LIKE :partialQuery)",
			"facilityId", UUID.class,
			"partialQuery", String.class);
		orderDetailsByFacilityAndPartialQuery.setMaxRecords(HIGH_MAX_RECORDS);
		indexedCriteria.put("orderDetailsByFacilityAndPartialQuery", orderDetailsByFacilityAndPartialQuery);

		TypedCriteria itemMastersByParentAndPartialQuery = new TypedCriteria("from ItemMaster where parent.persistentId = :parentId "
				+ " and active = true"
				+ " and (	lower(domainId) 	LIKE lower(:partialQuery)"
				+ "      or	lower(description)	LIKE lower(:partialQuery))",
			"parentId", UUID.class,
			"partialQuery", String.class);
		itemMastersByParentAndPartialQuery.setMaxRecords(HIGH_MAX_RECORDS);
		indexedCriteria.put("itemMastersByParentAndPartialQuery", itemMastersByParentAndPartialQuery);

		indexedCriteria.put("orderDetailsByHeader",
			new TypedCriteria("from OrderDetail where parent.persistentId = :theId and active = true",
				"theId", UUID.class)); //the UI dynamically sets the "parent" with theId

		indexedCriteria.put("orderHeadersByFacilityAndPartialDomainId",
			new TypedCriteria("from OrderHeader where active = true and parent.persistentId = :facilityId and domainId LIKE :partialDomainId",
				"facilityId", UUID.class,
				"partialDomainId", String.class));
		
		indexedCriteria.put("orderHeadersByFacilityAndType",
			new TypedCriteria("from OrderHeader where active = true and parent.persistentId = :facilityId and orderType = :orderType",
				"facilityId", UUID.class,
				"orderType", OrderTypeEnum.class));

		indexedCriteria.put("orderGroupsByOrderHeaderId",
			new TypedCriteria("from OrderGroup as og join fetch og.orderHeaders oh where og.active = true and og.parent.persistentId = :parentId and oh.domainId LIKE :partialDomainId",
				"parentId", UUID.class, //the UI dynamically sets the "parent" with theId
				"partialDomainId", String.class));

		indexedCriteria.put("orderHeadersByGroupAndType",
			new TypedCriteria("from OrderHeader where active = true and orderGroup.persistentId = :theId and orderType = :orderType",
				"theId", UUID.class, //the UI dynamically sets the "parent" with theId
				"orderType", OrderTypeEnum.class));
		
		TypedCriteria  gtinsByFacility = new TypedCriteria("from Gtin where parent.parent.persistentId = :facilityId",
			"facilityId", UUID.class);
			gtinsByFacility.setMaxRecords(MED_MAX_RECORDS);
		indexedCriteria.put("gtinsByFacility", gtinsByFacility);

		TypedCriteria gtinsByFacilityAndPartialQuery = new TypedCriteria("from Gtin where parent.parent.persistentId = :facilityId "
				+ " and (	lower(domainId) 			LIKE lower(:partialQuery)"
				+ "      or lower(parent.description) 	LIKE lower(:partialQuery)"
				+ "      or lower(parent.domainId) 		LIKE lower(:partialQuery))",
			"facilityId", UUID.class,
			"partialQuery", String.class);
		gtinsByFacilityAndPartialQuery.setMaxRecords(HIGH_MAX_RECORDS);
		indexedCriteria.put("gtinsByFacilityAndPartialQuery", gtinsByFacilityAndPartialQuery);

		indexedCriteria.put("orderLocationByFacility",
			new TypedCriteria("from OrderLocation where active = true and parent.parent.persistentId = :facilityId",
				"facilityId", UUID.class)); // could check that the location is active.
		
		indexedCriteria.put("orderLocationByFacilityAndLocationAll",
			new TypedCriteria("from OrderLocation where location.persistentId = :locationId and parent.parent.persistentId = :facilityId",
				"facilityId", UUID.class,
				"locationId", UUID.class));
		
		indexedCriteria.put("orderLocationByFacilityAndLocationActive",
			new TypedCriteria("from OrderLocation where location.persistentId = :locationId and parent.parent.persistentId = :facilityId and active = true ",
				"facilityId", UUID.class,
				"locationId", UUID.class)); // could check that the location is active.
	}

	public TypedCriteria findByName(String name, Class<?> selectClass) {
		if (name.equals(ALL_BY_PARENT)) {
			String query = String.format("from %s where parent.persistentId = :parentId", selectClass.getSimpleName());
			return new TypedCriteria(query, "parentId", UUID.class);
		} else if (name.equals(ALL_ACTIVE_BY_PARENT)){
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
