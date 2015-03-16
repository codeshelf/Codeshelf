package com.codeshelf.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.type.StandardBasicTypes;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.dao.CriteriaRegistry;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.platform.persistence.ITenantPersistenceService;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.service.ProductivitySummaryList.StatusSummary;
import com.google.common.collect.ImmutableMap;
import com.sun.jersey.api.NotFoundException;

/**
 * Functionality that reports and manipulates the Orders model (ie. OrderHeader, OrderGroups and OrderDetails).
 * Orders is an abstraction over the work to be executed by the system and Selector personnel
 *
 *
 */
public class OrderService implements IApiService {

	@EqualsAndHashCode(of={"orderId", "orderDetailId"})
	public static class OrderDetailView {
		@Getter @Setter
		private String orderId;

		@Getter @Setter
		private String orderDetailId;
		@Getter @Setter
		private String uom;
		@Getter @Setter
		private String sku;
		@Getter @Setter
		private String description;
		@Getter @Setter
		private int planQuantity;
		@Getter
		private OrderStatusEnum status;
		
		public void setStatusEnum(OrderStatusEnum statusEnum) {
			status = statusEnum;
		}
		
		public void setStatusString(String statusString) {
			status = OrderStatusEnum.valueOf(statusString);
		}

	}

	public Collection<OrderDetailView> orderDetailsNoLocation(Tenant tenant, Session session, UUID facilityUUID) {
		HashSet<OrderDetailView> allOrderDetails = new HashSet<OrderDetailView>();
		allOrderDetails.addAll(orderDetailNoPreferredLocation(tenant, session, facilityUUID));
		allOrderDetails.retainAll(orderDetailsNoInventoryLocation(tenant, session, facilityUUID));
		return allOrderDetails;
	}
	
	private List<OrderDetailView> orderDetailNoPreferredLocation(Tenant tenant, Session session, UUID facilityUUID) {
		@SuppressWarnings("unchecked")
		List<OrderDetailView>  result = session.createQuery(
			"select oh.domainId as orderId, od.domainId as orderDetailId, od.itemMaster.domainId as sku, od.uomMaster.domainId as uom, od.description as description, od.quantity as planQuantity, od.status as statusEnum"
				+ " from OrderDetail as od left join od.parent as oh where oh.parent.persistentId = :facilityId and od.active = true and COALESCE(od.preferredLocation, '') = ''")
				.setParameter("facilityId", facilityUUID)
				.setResultTransformer(new AliasToBeanResultTransformer(OrderDetailView.class))
				.list();
		return result;
	}
	
	private List<OrderDetailView> orderDetailsNoInventoryLocation(Tenant tenant, Session session, UUID facilityUUID) {
		String schema = tenant.getSchemaName();
		@SuppressWarnings("unchecked")
		List<OrderDetailView>  result = session.createSQLQuery(String.format(
			"select oh.domainid \"orderId\", od.domainId as \"orderDetailId\", im.domainId as \"sku\", um.domainId as \"uom\", od.description as \"description\", od.quantity as \"planQuantity\", od.status as \"statusString\""
		    + " from %s.order_detail as od"
			+ " left join %s.item as item on od.item_master_persistentid = item.parent_persistentid AND od.uom_master_persistentid = item.uom_master_persistentid"
			+ " left join %s.order_header as oh on od.parent_persistentid = oh.persistentid"
			+ " left join %s.item_master as im on od.item_master_persistentid = im.persistentid"
			+ " left join %s.uom_master as um on od.uom_master_persistentid = um.persistentid"
			+ " where CAST(oh.parent_persistentid AS varchar(50)) = :facilityId AND stored_location_persistentid IS NULL"
			, schema, schema, schema, schema, schema ))
				.setParameter("facilityId", facilityUUID.toString())
				.setResultTransformer(new AliasToBeanResultTransformer(OrderDetailView.class))
				.list();
		return result;
	}

	public List<OrderDetailView> orderDetailsByStatus(Session session, UUID facilityUUID, OrderStatusEnum orderStatusEnum) {
		@SuppressWarnings("unchecked")
		List<OrderDetailView>  result = session.createQuery(
			//	"select od "
			"select oh.domainId as orderId, od.domainId as orderDetailId, od.itemMaster.domainId as sku, od.uomMaster.domainId as uom, od.description as description, od.quantity as planQuantity, od.status as statusEnum"
				+ " from OrderDetail od left join od.parent as oh where od.parent.parent.persistentId = :facilityId and od.active = true "
				+ " and od.status = :status"
				)
				.setParameter("facilityId", facilityUUID)
				.setParameter("status", orderStatusEnum)
				.setResultTransformer(new AliasToBeanResultTransformer(OrderDetailView.class))
				.list();
		return result;
	}

	public int archiveAllOrders(String facilityUUID) {
		ITenantPersistenceService persistence = TenantPersistenceService.getInstance(); // convenience
		String schema = persistence.getDefaultSchema().getSchemaName();
		Session session = persistence.getSession();
		UUID uuid = UUID.fromString(facilityUUID);

		// The parent.parent traversal generates a cross join that results in invalid psql syntax
//
//		int odResult = session.createQuery("update OrderDetail od set od.active = false where od.parent.parent.persistentId = :facilityUUID")
//				.setParameter("facilityUUID", uuid)
//				.executeUpdate();

		@SuppressWarnings("unused")
		int odResult = session.createSQLQuery(String.format("update %s.order_detail od set active = false FROM %s.order_header oh WHERE od.parent_persistentid = oh.persistentid AND CAST(oh.parent_persistentid AS VARCHAR(50)) =  :facilityUUIDString", schema, schema))
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
		if (aggregate.equals("Case")) {
			 return caseSummary(session, facilityUUID, filterName);
		} else if (aggregate.equals("OrderHeader")){
			return orderSummary(session, facilityUUID, filterName);
		} else if (aggregate.equals("OrderDetail")) {
			return orderDetailSummary(session, facilityUUID, filterName);
		}
		return null;
	}

	private StatusSummary  orderSummary(Session session, UUID facilityUUID, String filterName) {

		String hqlWhereString = generateFilters(session).get(filterName);
		Query query = session.createQuery("select count(oh.status) as total, oh.status as status from OrderHeader oh where oh.parent.persistentId = :facilityUUID and " + hqlWhereString + " group by oh.status")
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

	private StatusSummary  orderDetailSummary(Session session, UUID facilityUUID, String filterName) {
		String hqlWhereString = generateFilters(session).get(filterName);
		Query query = session.createQuery("select count(od.status) as total,  od.status as status from OrderDetail od join od.parent oh where oh.parent.persistentId = :facilityUUID and od.active = true and " + hqlWhereString + " GROUP BY od.status")
			.setParameter("facilityUUID", facilityUUID)
			.setCacheable(true);

		@SuppressWarnings("unchecked")
		List<Object[]> tuples = query.list();
		final StatusSummary summary = new StatusSummary();
		for (Object[] tuple : tuples) {
			summary.add(new Integer(tuple[0].toString()), (OrderStatusEnum)tuple[1]);
		}
		return summary;
	}

	/**
	 * A very specific example of summarizing by a specific field
	 */
	private StatusSummary caseSummary(Session session, UUID facilityUUID, String filterName) {
		//TODO temp copy
		String fromClause = "select count(od.status) as total,  od.status as status from OrderDetail od join od.parent oh join od.uomMaster uom where oh.parent.persistentId = :facilityUUID and uom.domainId in ('CS') and ";
		String hqlWhereString = generateFilters(session).get(filterName);
		Query query = session.createQuery(fromClause + hqlWhereString + " group by od.status")
				.setParameter("facilityUUID", facilityUUID)
				.setCacheable(true);
		@SuppressWarnings("unchecked")
		List<Object[]> tuples = query.list();
		final StatusSummary summary = new StatusSummary();
		for (Object[] tuple : tuples) {
			summary.add(new Integer(tuple[0].toString()), (OrderStatusEnum)tuple[1]);
		}
		return summary;
	}

	@SuppressWarnings("unchecked")
	public ProductivitySummaryList getProductivitySummary(Tenant tenant, UUID facilityId, boolean skipSQL) throws Exception {
		Facility facility = null;
		List<Object[]> picksPerHour = null;
		ProductivitySummaryList productivitySummary = null;
		try {
			Session session = TenantPersistenceService.getInstance().getSessionWithTransaction(tenant);
			facility = Facility.staticGetDao().findByPersistentId(facilityId);
			if (facility == null) {
				throw new NotFoundException("Facility " + facilityId + " does not exist");
			}

			if (!skipSQL) {
				String schema = tenant.getSchemaName();

				String queryStr = String.format("" + "SELECT dur.order_group AS group,\n" + "		trim(to_char(\n"
						+ "		 3600 / (EXTRACT('epoch' FROM avg(dur.duration)) + 1) ,\n"
						+ "		'9999999999999999999D9')) AS picksPerHour\n" + "FROM \n" + "	(\n" + "		SELECT group_and_sort_code,\n"
						+ "			COALESCE(g.domainid, 'undefined') AS order_group,\n"
						+ "			i.completed - lag(i.completed) over (ORDER BY i.completed) as duration\n"
						+ "		FROM %s.work_instruction i\n"
						+ "			INNER JOIN %s.order_detail d ON i.order_detail_persistentid = d.persistentid\n"
						+ "			INNER JOIN %s.order_header h ON d.parent_persistentid = h.persistentid\n"
						+ "			LEFT JOIN %s.order_group g ON h.order_group_persistentid = g.persistentid\n"
						+ "		WHERE  i.item_id != 'Housekeeping'\n" + "	) dur\n" + "WHERE dur.group_and_sort_code != '0001'\n"
						+ "GROUP BY dur.order_group\n" + "ORDER BY dur.order_group", schema, schema, schema, schema);
				SQLQuery getPicksPerHourQuery = session.createSQLQuery(queryStr)
					.addScalar("group", StandardBasicTypes.STRING)
					.addScalar("picksPerHour", StandardBasicTypes.DOUBLE);
				getPicksPerHourQuery.setCacheable(true);
				picksPerHour = getPicksPerHourQuery.list();
			}
			productivitySummary = new ProductivitySummaryList(facility, picksPerHour, session);
		} finally {
			TenantPersistenceService.getInstance().commitTransaction(tenant);
		}
		return productivitySummary;
	}

	public ProductivityCheSummaryList getCheByGroupSummary(UUID facilityId) throws Exception {
		List<WorkInstruction> instructions = WorkInstruction.staticGetDao().findByFilter(CriteriaRegistry.ALL_BY_PARENT,
			ImmutableMap.<String, Object> of("parentId", facilityId));
		ProductivityCheSummaryList summary = new ProductivityCheSummaryList(facilityId, instructions);
		return summary;
	}

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
		return Collections.<String, String>unmodifiableMap(filters);
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



}
