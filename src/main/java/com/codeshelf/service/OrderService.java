package com.codeshelf.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.type.StandardBasicTypes;

import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.dao.CriteriaRegistry;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.service.ProductivitySummaryList.StatusSummary;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.sun.jersey.api.NotFoundException;

/**
 * Functionality that reports and manipulates the Orders model (ie. OrderHeader, OrderGroups and OrderDetails).
 * Orders is an abstraction over the work to be executed by the system and Selector personnel
 * 
 *
 */
public class OrderService implements IApiService {
	
	private TenantPersistenceService	persistenceService;

	@Inject
	public OrderService(TenantPersistenceService persistenceService) {
		this.persistenceService = persistenceService;
	}

	public StatusSummary statusSummary(String aggregate, String filterName) {
		if (aggregate.equals("Case")) {
			 return caseSummary(filterName);
		} else if (aggregate.equals("OrderHeader")){
			return orderSummary(filterName);
		} else if (aggregate.equals("OrderDetail")) {
			return orderDetailSummary(filterName);
		}
		return null;
	}
		
	private StatusSummary  orderSummary(String filterName) {
		
		String hqlWhereString = generateFilters().get(filterName);
		Session session = persistenceService.getSession();
		Query query = session.createQuery("select oh from OrderHeader oh where " + hqlWhereString);
		@SuppressWarnings("unchecked")
		List<OrderHeader> orderHeaders = (List<OrderHeader>) query.list();
		StatusSummary summary = new StatusSummary();
		for (OrderHeader orderHeader : orderHeaders) {
			summary.add(1, orderHeader.getStatus());
		}
		return summary;
	}
	
	private StatusSummary  orderDetailSummary(String filterName) {
		String hqlWhereString = generateFilters().get(filterName);
		Session session = persistenceService.getSession();
		Query query = session.createQuery("select od from OrderDetail od join od.parent oh where od.active = true and " + hqlWhereString);
		query.setCacheable(true);
		@SuppressWarnings("unchecked")
		List<OrderDetail> orderDetails = (List<OrderDetail>) query.list();
		StatusSummary summary = new StatusSummary();
		for (OrderDetail orderDetail : orderDetails) {
			summary.add(1, orderDetail.getStatus());
		}
		return summary;
	}
	
	/**
	 * A very specific example of summarizing by a specific field
	 */
	private StatusSummary caseSummary(String filterName) {
		//TODO temp copy
		String fromClause = "select od from OrderDetail od join od.parent oh join od.uomMaster uom where uom.domainId in ('CS') and ";
		String hqlWhereString = generateFilters().get(filterName);
		Session session = persistenceService.getSession();
		Query query = session.createQuery(fromClause + hqlWhereString);
		@SuppressWarnings("unchecked")
		List<OrderDetail> orderDetails = (List<OrderDetail>) query.list();

		StatusSummary summary = new StatusSummary();
		for (OrderDetail orderDetail: orderDetails) {
			summary.add(orderDetail.getQuantity(), orderDetail.getStatus());
		}
		return summary;
	}
	
	@SuppressWarnings("unchecked")
	public ProductivitySummaryList getProductivitySummary(UUID facilityId, boolean skipSQL) throws Exception {
		Facility facility = Facility.DAO.findByPersistentId(facilityId);
		if (facility == null) {
			throw new NotFoundException("Facility " + facilityId + " does not exist");
		}
		Session session = persistenceService.getSession();
		List<Object[]> picksPerHour = null;
		if (!skipSQL) {
			String schema = System.getProperty("db.schemaname", "codeshelf");
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
		ProductivitySummaryList productivitySummary = new ProductivitySummaryList(facility, picksPerHour);
		return productivitySummary;
	}

	public ProductivityCheSummaryList getCheByGroupSummary(UUID facilityId) throws Exception {
		List<WorkInstruction> instructions = WorkInstruction.DAO.findByFilterAndClass(CriteriaRegistry.ALL_BY_PARENT,
			ImmutableMap.<String, Object> of("parentId", facilityId),
			WorkInstruction.class);
		ProductivityCheSummaryList summary = new ProductivityCheSummaryList(facilityId, instructions);
		return summary;
	}

	public List<WorkInstruction> getGroupShortInstructions(UUID facilityId, String groupNameIn) throws NotFoundException {
		//Get Facility
		Facility facility = Facility.DAO.findByPersistentId(facilityId);
		if (facility == null) {
			throw new NotFoundException("Facility " + facilityId + " does not exist");
		}
		//If group name provided, confirm that such group exists
		boolean allGroups = groupNameIn == null, undefined = OrderGroup.UNDEFINED.equalsIgnoreCase(groupNameIn);
		if (!(allGroups || undefined)) {
			OrderGroup group = OrderGroup.DAO.findByDomainId(facility, groupNameIn);
			if (group == null) {
				throw new NotFoundException("Group " + groupNameIn + " had not been created");
			}
		}
		//Get all instructions and filter those matching the requirements
		List<WorkInstruction> instructions = WorkInstruction.DAO.findByFilterAndClass(CriteriaRegistry.ALL_BY_PARENT,
			ImmutableMap.<String, Object> of("parentId", facilityId),
			WorkInstruction.class);
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
	public Set<String> getFilterNames() {
		return generateFilters().keySet();
		
	}
	
	/**
	 * Early implementation that just uses shippers as an example and prepends an All.
	 * Very specific to OrderHeaders to start
	 */
	private Map<String, String> generateFilters() {
		LinkedHashMap<String, String> filters = new LinkedHashMap<>();
		filters.put("All", "oh.active = true");
		filters.putAll(generateShipperFilters());
		return Collections.<String, String>unmodifiableMap(filters);
	}

	/**
	 * Quick way to pregenerate filters for each existing shipper
	 */
	private Map<String, String> generateShipperFilters() {
		Map<String, String> shipperFilters = new HashMap<>();
		Session session = persistenceService.getSession();
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
