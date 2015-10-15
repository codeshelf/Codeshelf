package com.codeshelf.behavior;

import static com.codeshelf.model.dao.GenericDaoABC.createIntervalRestriction;
import static com.codeshelf.model.dao.GenericDaoABC.createSubstringRestriction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.apache.commons.beanutils.PropertyUtilsBean;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.ProductivitySummaryList.StatusSummary;
import com.codeshelf.manager.Tenant;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.util.CompareNullChecker;
import com.codeshelf.util.UomNormalizer;
import com.google.common.base.Strings;

/**
 * Functionality that reports and manipulates the Orders model (ie. OrderHeader, OrderGroups and OrderDetails).
 * Orders is an abstraction over the work to be executed by the system and Selector personnel
 *
 *
 */
public class OrderBehavior implements IApiBehavior {

	@EqualsAndHashCode(of = { "orderId", "orderDetailId" })
	public static class OrderDetailView {
		@Getter
		@Setter
		private String			orderId;

		@Getter
		@Setter
		private String			orderDetailId;
		@Getter
		@Setter
		private String			uom;
		@Getter
		@Setter
		private String			itemId;
		@Getter
		@Setter
		private String			description;
		@Getter
		@Setter
		private int				planQuantity;
		@Getter
		private OrderStatusEnum	status;

		public void setStatusEnum(OrderStatusEnum statusEnum) {
			status = statusEnum;
		}

		public void setStatusString(String statusString) {
			status = OrderStatusEnum.valueOf(statusString);
		}

	}

	public static class ItemView {
		public String getId() {
			return String.format("%s:%s", getSku(), getUom());
		}

		@Getter
		@Setter
		private String	uom;
		@Getter
		@Setter
		private String	sku;
		@Getter
		@Setter
		private String	description;
		@Getter
		@Setter
		private long	planQuantity;

	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(OrderBehavior.class);

	public Collection<ItemView> itemsInQuantityOrder(Session session, UUID facilityUUID) {
		@SuppressWarnings("unchecked")
		List<ItemView> result = session.createQuery("select od.itemMaster.domainId as sku, od.uomMaster.domainId as uom, od.description as description, sum(od.quantity) as planQuantity"
				+ " from OrderDetail as od left join od.parent as oh where oh.parent.persistentId = :facilityId and od.active = true "
				+ " GROUP BY od.itemMaster.domainId, od.uomMaster.domainId, od.description " + " ORDER BY planQuantity DESC")
			.setParameter("facilityId", facilityUUID)
			.setMaxResults(10)
			.setResultTransformer(new AliasToBeanResultTransformer(ItemView.class))
			.list();
		return result;
	}

	public List<Object[]> findOrderHeaderReferences(Facility facility, String orderIdSubstring, Interval dueDateInterval) {
		Criteria criteria = OrderHeader.staticGetDao()
				.createCriteria()
				.setProjection(Projections.projectionList()
					.add(Projections.property("domainId").as("orderId")))
				.add(Property.forName("parent").eq(facility))
				.add(Property.forName("active").eq(true));
				if (!Strings.isNullOrEmpty(orderIdSubstring)) {
					criteria.add(createSubstringRestriction("domainId", orderIdSubstring));
				}		
				if (dueDateInterval != null) {
					criteria.add(createIntervalRestriction("dueDate", dueDateInterval));
				}
		
		@SuppressWarnings("unchecked")
		List<Object[]> result = (List<Object[]>) criteria.list();
		return result;
	}
	
	public List<Map<String, Object>> findOrderHeadersForStatus(Facility facility,
		String[] propertyNames,
		OrderStatusEnum[] orderStatusEnums) {
		Criteria criteria = orderHeaderCriteria(facility).add(Property.forName("status").in(orderStatusEnums));
		@SuppressWarnings("unchecked")
		List<OrderHeader> results = (List<OrderHeader>) criteria.list();
		return toPropertiesView(results, propertyNames);

	}

	public List<Map<String, Object>> findOrderHeadersForOrderId(Facility facility, String[] propertyNames, String orderId) {

		SimpleExpression orderIdProperty = null;
		if (orderId != null && orderId.indexOf('*') >= 0) {
			orderIdProperty = Property.forName("domainId").like(orderId.replace('*', '%'));
		} else {
			orderIdProperty = Property.forName("domainId").eq(orderId);
		}

		Criteria criteria = orderHeaderCriteria(facility).add(orderIdProperty);
		@SuppressWarnings("unchecked")
		//long start = System.currentTimeMillis();
		List<OrderHeader> joined = (List<OrderHeader>) criteria.list();
		Set<OrderHeader> results = new HashSet<>(joined);
		//long stop = System.currentTimeMillis();
		//System.out.println("Fetch " + results.size() + " " + (start-stop));
		return toPropertiesView(results, propertyNames);
	}

	private List<Map<String, Object>> toPropertiesView(Collection<?> results, String[] propertyNames) {
		PropertyUtilsBean propertyUtils = new PropertyUtilsBean();
		ArrayList<Map<String, Object>> viewResults = new ArrayList<Map<String, Object>>();
		//long start = System.currentTimeMillis();

		for (Object object : results) {
			Map<String, Object> propertiesMap = new HashMap<>();
			for (String propertyName : propertyNames) {
				try {
					Object resultObject = propertyUtils.getProperty(object, propertyName);
					propertiesMap.put(propertyName, resultObject);
				} catch (NoSuchMethodException e) {
					// Minor problem. UI hierarchical view asks for same data field name for all object types in the view. Not really an error in most cases
					LOGGER.debug("no property " + propertyName + " on object: " + object);
				} catch (Exception e) {
					LOGGER.warn("unexpected exception for property " + propertyName + " object: " + object, e);
				}
			}
			viewResults.add(propertiesMap);
		}

		//long stop = System.currentTimeMillis();
		//System.out.println("TRANSFORM " +(stop-start));
		return viewResults;

	}

	private Criteria orderHeaderCriteria(Facility facility) {
		Criteria criteria = OrderHeader.staticGetDao()
			.createCriteria()
			.setFetchMode("orderDetails", FetchMode.JOIN)
			.setFetchMode("containerUse", FetchMode.JOIN)
			.setFetchMode("containerUse.parent", FetchMode.JOIN)
			.add(Property.forName("parent").eq(facility));
		return criteria;

	}

	@SuppressWarnings("unchecked")
	public List<OrderDetailView> getOrderDetailsForOrderId(Facility facility, String orderDomainId) {
		return (List<OrderDetailView>) OrderDetail.staticGetDao()
			.createCriteria()
			.createAlias("itemMaster", "im")
			.createAlias("uomMaster", "um")
			.createAlias("parent", "order")
			.add(Property.forName("order.domainId").eq(orderDomainId))
			.add(Property.forName("order.parent").eq(facility))
			.add(Property.forName("active").eq(true))
			.setProjection(Projections.projectionList()
				.add(Projections.property("domainId").as("orderDetailId"))
				.add(Projections.property("description").as("description"))
				.add(Projections.property("quantity").as("planQuantity"))
				.add(Projections.property("status").as("statusEnum"))
				.add(Projections.property("im.domainId"), "itemId")
				.add(Projections.property("um.domainId").as("uom"))
				.add(Projections.property("order.domainId").as("orderId")))

			.setResultTransformer(new AliasToBeanResultTransformer(OrderDetailView.class))
			.list();
	}

	public List<OrderDetailView> findOrderDetailsForStatus(Session session, UUID facilityUUID, OrderStatusEnum orderStatusEnum) {
		@SuppressWarnings("unchecked")
		List<OrderDetailView> result = session.createQuery(
		//	"select od "
		"select oh.domainId as orderId, od.domainId as orderDetailId, od.itemMaster.domainId as sku, od.uomMaster.domainId as uom, od.description as description, od.quantity as planQuantity, od.status as statusEnum"
				+ " from OrderDetail od left join od.parent as oh where od.parent.parent.persistentId = :facilityId and od.active = true "
				+ " and od.status = :status")
			.setParameter("facilityId", facilityUUID)
			.setParameter("status", orderStatusEnum)
			.setResultTransformer(new AliasToBeanResultTransformer(OrderDetailView.class))
			.list();
		return result;
	}

	public int archiveAllOrders(String facilityUUID) {
		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();
		TenantPersistenceService persistence = TenantPersistenceService.getInstance(); // convenience
		String schema = tenant.getSchemaName();
		Session session = persistence.getSession();
		UUID uuid = UUID.fromString(facilityUUID);

		// The parent.parent traversal generates a cross join that results in invalid psql syntax
		//
		//		int odResult = session.createQuery("update OrderDetail od set od.active = false where od.parent.parent.persistentId = :facilityUUID")
		//				.setParameter("facilityUUID", uuid)
		//				.executeUpdate();

		@SuppressWarnings("unused")
		int odResult = session.createSQLQuery(String.format("update %s.order_detail od set active = false FROM %s.order_header oh WHERE od.parent_persistentid = oh.persistentid AND CAST(oh.parent_persistentid AS VARCHAR(50)) =  :facilityUUIDString",
			schema,
			schema))
			.setParameter("facilityUUIDString", uuid.toString())
			.executeUpdate();

		int ohResult = session.createQuery("update OrderHeader oh set oh.active = false where oh.parent.persistentId = :facilityUUID")
			.setParameter("facilityUUID", uuid)
			.executeUpdate();

		@SuppressWarnings("unused")
		int ogResult = session.createQuery("update OrderGroup og set og.active = false where og.parent.persistentId = :facilityUUID")
			.setParameter("facilityUUID", uuid)
			.executeUpdate();

		return ohResult;
	}

	public StatusSummary statusSummary(Session session, UUID facilityUUID, String aggregate, String filterName) {
		if (UomNormalizer.normalizedEquals("CASE", aggregate)) {
			return uomSummary(session, facilityUUID, filterName, UomNormalizer.variants(UomNormalizer.CASE));
		} else if (UomNormalizer.normalizedEquals("EACH", aggregate)) {
			return uomSummary(session, facilityUUID, filterName, UomNormalizer.variants(UomNormalizer.EACH));
		} else if (aggregate.equals("OrderHeader")) {
			return orderSummary(session, facilityUUID, filterName);
		} else if (aggregate.equals("OrderDetail")) {
			return orderDetailSummary(session, facilityUUID, filterName);
		}
		return null;
	}

	private StatusSummary orderSummary(Session session, UUID facilityUUID, String filterName) {

		String hqlWhereString = generateFilters(session).get(filterName);
		Query query = session.createQuery("select count(oh.status) as total, oh.status as status from OrderHeader oh where oh.parent.persistentId = :facilityUUID and "
				+ hqlWhereString + " group by oh.status")
			.setParameter("facilityUUID", facilityUUID)
			.setCacheable(true);
		@SuppressWarnings("unchecked")
		List<Object[]> tuples = (List<Object[]>) query.list();

		StatusSummary summary = new StatusSummary();
		for (Object[] tuple : tuples) {
			summary.add(new Integer(tuple[0].toString()), (OrderStatusEnum) tuple[1]);
		}
		return summary;
	}

	private StatusSummary orderDetailSummary(Session session, UUID facilityUUID, String filterName) {
		String hqlWhereString = generateFilters(session).get(filterName);
		Query query = session.createQuery("select count(od.status) as total,  od.status as status from OrderDetail od join od.parent oh where oh.parent.persistentId = :facilityUUID and od.active = true and "
				+ hqlWhereString + " GROUP BY od.status")
			.setParameter("facilityUUID", facilityUUID)
			.setCacheable(true);

		@SuppressWarnings("unchecked")
		List<Object[]> tuples = query.list();
		final StatusSummary summary = new StatusSummary();
		for (Object[] tuple : tuples) {
			summary.add(new Integer(tuple[0].toString()), (OrderStatusEnum) tuple[1]);
		}
		return summary;
	}

	/**
	 * A very specific example of summarizing by a specific field
	 */
	private StatusSummary uomSummary(Session session, UUID facilityUUID, String filterName, List<String> uoms) {
		String fromClause = "select count(od.status) as total,  od.status as status from OrderDetail od join od.parent oh join od.uomMaster uom where oh.parent.persistentId = :facilityUUID and upper(uom.domainId) in (:uoms) and ";
		String hqlWhereString = generateFilters(session).get(filterName);
		Query query = session.createQuery(fromClause + hqlWhereString + " group by od.status")
			.setParameter("facilityUUID", facilityUUID)
			.setParameterList("uoms", uoms)
			.setCacheable(true);
		@SuppressWarnings("unchecked")
		List<Object[]> tuples = query.list();
		final StatusSummary summary = new StatusSummary();
		for (Object[] tuple : tuples) {
			summary.add(new Integer(tuple[0].toString()), (OrderStatusEnum) tuple[1]);
		}
		return summary;
	}

	/*

	public List<WorkInstruction> getGroupShortInstructions(UUID facilityId, String groupNameIn) throws NotFoundException {
		//Get Facility
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);
		if (facility == null) {
			throw new NotFoundException("Facility " + facilityId + " does not exist");
		}
		//If group name provided, confirm that such group exists
		boolean allGroups = groupNameIn == null, undefined = OrderGroup.UNDEFINED.equalsIgnoreCase(groupNameIn);
		if (!(allGroups || undefined)) {
			OrderGroup group = OrderGroup.staticGetDao().findByDomainId(facility, groupNameIn);
			if (group == null) {
				throw new NotFoundException("Group " + groupNameIn + " had not been created");
			}
		}
		//Get all instructions and filter those matching the requirements
		List<WorkInstruction> instructions = WorkInstruction.staticGetDao().findByFilter(CriteriaRegistry.ALL_BY_PARENT,
			ImmutableMap.<String, Object> of("parentId", facilityId));
		List<WorkInstruction> filtered = new ArrayList<>();
		for (WorkInstruction instruction : instructions) {
			if (instruction.isHousekeeping() || instruction.getStatus() != WorkInstructionStatusEnum.SHORT) {
				continue;
			}
			OrderDetail detail = instruction.getOrderDetail();
			if (detail == null) {
				continue;
			}
			OrderHeader header = detail.getParent();
			String groupName = header.getOrderGroup() == null ? OrderGroup.UNDEFINED : header.getOrderGroup().getDomainId();
			if (allGroups || groupName.equals(groupNameIn)) {
				filtered.add(instruction);
			}
		}
		return filtered;
	}
	*/

	/***
	 * This is initial work to get supported filters. Right now it is just a name, but is expected to become a set of objects.
	 * This MAY converge with filters in codeshelf.filter somehow.
	 */
	public Set<String> getFilterNames(Session session) {
		return generateFilters(session).keySet();

	}

	/**
	 * Early implementation that just uses shippers as an example and prepends an All.
	 * Very specific to OrderHeaders to start
	 */
	private Map<String, String> generateFilters(Session session) {
		LinkedHashMap<String, String> filters = new LinkedHashMap<>();
		filters.put("All", "oh.active = true");
		filters.putAll(generateShipperFilters(session));
		return Collections.<String, String> unmodifiableMap(filters);
	}

	/**
	 * Quick way to pregenerate filters for each existing shipper
	 */
	private Map<String, String> generateShipperFilters(Session session) {
		Map<String, String> shipperFilters = new HashMap<>();
		Query query = session.createQuery("select distinct oh.shipperId from OrderHeader oh where active = true");
		query.setCacheable(true);
		@SuppressWarnings("unchecked")
		List<String> shipperIds = (List<String>) query.list();
		for (String shipperId : shipperIds) {
			shipperFilters.put(shipperId, String.format("oh.active = true and oh.shipperId = '%s'", shipperId));
		}
		return shipperFilters;
	}

	public int deleteAll(Facility facility) {
		//This needs to obey the relationship cascades defined in hibernate
		List<OrderHeader> orderHeaders = OrderHeader.staticGetDao().findByParent(facility);
		int result = orderHeaders.size();
		for (OrderHeader orderHeader : orderHeaders) {
			orderHeader.delete();
		}
		// new v20
		deleteDuplicateItemMasters(facility);
		return result;
	}

	/**
	 * Bad order processing can result in various duplicates. Deleting Orders cascades to get most of it, but misses any duplicated item masters
	 */
	private void deleteDuplicateItemMasters(Facility facility) {
		List<ItemMaster> masters = ItemMaster.staticGetDao().findByParent(facility);
		Collections.sort(masters, new SkuComparator());

		String previousId = null;
		ArrayList<ItemMaster> listOfMastersWithSameId = new ArrayList<ItemMaster>();

		for (ItemMaster master : masters) {
			String thisId = master.getItemId();
			// chicken crash proofing
			if (thisId == null) {
			} else if (thisId.equals(previousId)) {
				// add on to uniqueIdList
				listOfMastersWithSameId.add(master);
			} else {
				// Start on the next ID. But if we hae 2 or more of the last ID, pass that list off for analysis for deletion.
				if (listOfMastersWithSameId.size() > 1) {
					LOGGER.warn("finding duplicate masters with this ID: {}", previousId);
					deleteUnneededDuplicatesFrom(listOfMastersWithSameId);
				}
				listOfMastersWithSameId.clear();
				listOfMastersWithSameId.add(master);
				previousId = thisId;
			}
		}
	}

	/**
	 * This is a list of two or more masters with the same ID.
	 * The goal is to keep all that have references from gtin or item. If none do, arbitrarily keep one
	 */
	private void deleteUnneededDuplicatesFrom(List<ItemMaster> mastersWithSameId) {
		ArrayList<ItemMaster> referencedMasters = new ArrayList<ItemMaster>();
		ArrayList<ItemMaster> unReferencedMasters = new ArrayList<ItemMaster>();

		ItemMaster firstMaster = mastersWithSameId.get(0);
		for (ItemMaster master : mastersWithSameId) {
			if (master.getGtins().size() > 0 || master.getItems().size() > 0) {
				referencedMasters.add(master);
			} else {
				unReferencedMasters.add(master);
			}
		}
		// If we have any referenced masters, we can delete all of the unreferenced ones
		// If all unreferenced, keep one of them
		LOGGER.info("masters of {}: {} have references, and {} do not.",
			firstMaster.getItemId(),
			referencedMasters.size(),
			unReferencedMasters.size());
		boolean skipFirst = referencedMasters.size() == 0;
		for (ItemMaster deletemaster : unReferencedMasters) {
			if (skipFirst) {
				skipFirst = false;
			} else {
				LOGGER.warn("Deleting duplicate master {}", deletemaster);
				ItemMaster.staticGetDao().delete(deletemaster);				
			}
		}
	}

	private class SkuComparator implements Comparator<ItemMaster> {
		// Sort the masters by item ID so that iterator finds them grouped.

		@Override
		public int compare(ItemMaster master1, ItemMaster master2) {

			int value = CompareNullChecker.compareNulls(master1, master2);
			if (value != 0)
				return value;
			String id1 = master1.getItemId();
			String id2 = master2.getItemId();
			value = CompareNullChecker.compareNulls(id1, id2);
			if (value != 0)
				return value;
			value = id1.compareTo(id2);
			if (value != 0)
				return value;

			return master1.getPersistentId().compareTo(master2.getPersistentId()); // just to give a determinant sort
		}
	}

}
