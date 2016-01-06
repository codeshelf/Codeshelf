package com.codeshelf.behavior;

import static com.codeshelf.model.dao.GenericDaoABC.countCriteria;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.transform.BasicTransformerAdapter;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.responses.EventDisplay;
import com.codeshelf.api.responses.PickRate;
import com.codeshelf.api.responses.ResultDisplay;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.PageQuery;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.DomainObjectABC;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.model.domain.WorkerEvent.EventType;
import com.codeshelf.persistence.DialectUUIDType;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.ws.protocol.message.NotificationMessage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.inject.Inject;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class NotificationBehavior implements IApiBehavior{
	
	public static class HistogramResult {
		
		@Getter
		private Date startTime;
		@Getter
		private Date endTime;
		@Getter
		private String binInterval; //ISO
		@Getter
		private List<BinValue> bins;
		
		public HistogramResult(Interval window, Period bin, List<BinValue> values) {
			this.startTime = window.getStart().toDate();
			this.endTime = window.getEnd().toDate();
			this.binInterval = bin.toString(ISOPeriodFormat.standard());
			this.bins = values;
		}

	}
	
	public static class BinValue {

		@Getter
		private Date start;
		
		@JsonIgnore
		private Period interval;
		
		@Getter
		private BigInteger value;

		public BinValue(DateTime binStart, Period interval, BigInteger value) {
			this.start = binStart.toDate();
			this.interval = interval;
			this.value = value;
		}

		public static BinValue missing(DateTime binStart, Period interval) {
			//TODO indicate missing with missing field?
			return new BinValue(binStart,  interval, BigInteger.ZERO);
		}
	}


	private static final Logger			LOGGER				= LoggerFactory.getLogger(NotificationBehavior.class);
	
	private static final EnumSet<WorkerEvent.EventType>	SAVE_ONLY	= EnumSet.of(EventType.SKIP_ITEM_SCAN,
																		EventType.SHORT,
																		EventType.COMPLETE,
																		EventType.DETAIL_WI_MISMATCHED,
																		EventType.LOW);
	
	private final WorkerHourlyMetricBehavior	workerHourlyMetricBehavior = new WorkerHourlyMetricBehavior();
	
	@Inject
	public NotificationBehavior() {
	}
	
	public WorkerEvent saveEvent(WorkerEvent event) {
		WorkerEvent.staticGetDao().store(event);
		return event;
	}

	public void saveFinishedWI(WorkInstruction wi) {
		EventType type = null;
		if (wi.getStatus().equals(WorkInstructionStatusEnum.COMPLETE)) {
			type = EventType.COMPLETE;
		} else if(wi.getStatus().equals(WorkInstructionStatusEnum.SHORT))  {
			type = EventType.SHORT;
		} else {
			return;
		}
		if (!SAVE_ONLY.contains(type)) {
			return;
		}
	
		LOGGER.info("Saving WorkerEvent {} from {} for {}", type, wi.getAssignedChe(), wi.getPickerId());
		WorkerEvent event = new WorkerEvent();
		Che device = wi.getAssignedChe();
		event.setDeviceGuid(device.getDeviceGuidStr());
		event.setParent(device.getFacility());
		event.setDevicePersistentId(device.getPersistentId().toString());
		
		event.setPurpose(wi.getPurpose().name());
		event.setCreated(wi.getCompleted());
		event.setEventType(type);
		event.setWorkerId(wi.getPickerId());
		event.setWorkInstruction(wi);
		event.setLocation(wi.getPickInstruction());
		OrderDetail orderDetail = wi.getOrderDetail();
		if (orderDetail != null) {
			event.setOrderDetailId(orderDetail.getPersistentId());
		}
		event.generateDomainId();
		WorkerEvent existingEventWithSameDomain = WorkerEvent.staticGetDao().findByDomainId(device.getFacility(), event.getDomainId());
		if (existingEventWithSameDomain == null){
			WorkerEvent.staticGetDao().store(event);
			//Save Complete or Short event into WorkerHourlyMetric
			workerHourlyMetricBehavior.recordEvent(wi.getFacility(), wi.getPickerId(), type);			
		} else {
			LOGGER.warn("Event " + event.getDomainId() + " already exists. Not creating another one.");
		}
	}
	
	public void saveEvent(NotificationMessage message) {
		if (!SAVE_ONLY.contains(message.getEventType())) {
			return;
		}
		boolean save_completed=false;
		try {
			TenantPersistenceService.getInstance().beginTransaction();
			LOGGER.info("Saving WorkerEvent {} from {} for {}", message.getEventType(), message.getNetGuidStr(), message.getWorkerId());
			WorkerEvent event = new WorkerEvent();

			Class<?> deviceClass = message.getDeviceClass();
			DomainObjectABC device = null;
			if (deviceClass == Che.class) {
				device = Che.staticGetDao().findByPersistentId(message.getDevicePersistentId());
			} else {
				throw new IllegalArgumentException("unable to process notifications from " + deviceClass + " devices");
			}
			if (device == null) {
				throw new IllegalArgumentException(String.format("unable to find %s device %s (%s)", deviceClass, message.getDevicePersistentId(), message.getNetGuidStr()));
			}
			event.setDeviceGuid(new NetGuid(message.getNetGuidStr()).toString());
			event.setParent(device.getFacility());
			event.setDevicePersistentId(device.getPersistentId().toString());
			
			event.setCreated(new Timestamp(message.getTimestamp()));
			event.setEventType(message.getEventType());
			event.setWorkerId(message.getWorkerId());
			
			UUID workInstructionId = message.getWorkInstructionId();
			if (workInstructionId != null) {
				WorkInstruction wi = WorkInstruction.staticGetDao().findByPersistentId(workInstructionId);
				// It is possible, though bad and unlikely, that server has deleted a work instruction by the time site controller sends message to server about it.
				// This dataflow is seen in DataArchiving.testPurgeActiveJobs()
				if (wi == null) {
					throw new NullPointerException(String.format("Work instruction does not exist for this persistentId: %s", workInstructionId.toString()));
					// would get NPE just below in wi.getOrderDetail();
				}
				event.setWorkInstruction(wi);
				event.setLocation(wi.getPickInstruction());
				OrderDetail orderDetail = wi.getOrderDetail();
				if (orderDetail != null) {
					event.setOrderDetailId(orderDetail.getPersistentId());
				}
			}
			
			event.generateDomainId();
			WorkerEvent.staticGetDao().store(event);
			TenantPersistenceService.getInstance().commitTransaction();
			save_completed = true;
		} finally {
			if(!save_completed) {
				TenantPersistenceService.getInstance().rollbackTransaction();
			}
		}
	}

	public List<PickRate> getPickRate(Set<String> purposes, Interval createdTime){
		return getPickRate(ImmutableSet.of(WorkerEvent.EventType.COMPLETE, WorkerEvent.EventType.SHORT),
			               ImmutableSet.of("workerId"),
			               purposes,
			               createdTime);
	}
	@SuppressWarnings("unchecked")
	public List<PickRate> getPickRate(Set<WorkerEvent.EventType> types, Set<String> groupPropertyNames, Set<String> purposes, Interval createdTime){
		Preconditions.checkArgument(groupPropertyNames.size() > 0, "must group pick rates by at least one property");
		List<String> selectProperties = new ArrayList<>();
		for (String groupPropertyName : groupPropertyNames) {
			String selectClause = String.format("%s as %s", groupPropertyName, groupPropertyName);
			selectProperties.add(selectClause);
		}
		String selectClause = Joiner.on(",").join(selectProperties);
		String groupByClause = Joiner.on(",").join(groupPropertyNames);
		Session session = TenantPersistenceService.getInstance().getSession();
		Query query = session.createQuery("SELECT "
				+                    selectClause
				+ "                 ,HOUR(created) as hour"
				+ "                 ,count(*) as picks"
				//+ "                 ,sum(workInstruction.actualQuantity) as quantity" TODO should denormalize into workerevent
				+ "           FROM WorkerEvent"
				+ "          WHERE eventType IN (:includedEventTypes)"
				+              ((purposes != null && purposes.size() > 0) ? " AND purpose IN (:purposes)" : "") 
				+ "            AND created BETWEEN :startDateTime AND :endDateTime"
				+ "       GROUP BY "
				+                  groupByClause
				+ "                ,HOUR(created)"
				+ "       ORDER BY HOUR(created)"
				);
		query.setParameterList("includedEventTypes", types);
		Timestamp startTimestamp = new Timestamp(createdTime.getStartMillis());
		Timestamp endTimestamp = new Timestamp(createdTime.getEndMillis());
		query.setParameter("startDateTime", startTimestamp); //use setParameter instead of set timestamp so that it goes through the UTC conversion before hitting db
		query.setParameter("endDateTime", endTimestamp);
		if (purposes != null && purposes.size() > 0) {
			query.setParameterList("purposes", purposes);
		}
		query.setResultTransformer(new AliasToBeanResultTransformer(PickRate.class));
		List<PickRate> results = query.list();
 		return results;
	}
	
	
	@SuppressWarnings("unchecked")
	public List<BinValue> facilityPickRateHistogram(Facility facility, final Interval completedInterval, final Period binWidth) throws IOException {
		String sqlWhereClause = 
				  " parent_persistentid = :facilityId"
				+ " and created between :startDateTime and :endDateTime"
                + " ";
		URL url = Resources.getResource(this.getClass(), "./facilityPickRate.sql");
		String sqlTemplate = Resources.toString(url, Charsets.UTF_8);
		String sqlQuery = String.format(sqlTemplate, sqlWhereClause);
		Session session = TenantPersistenceService.getInstance().getSession();
		SQLQuery query = session.createSQLQuery(sqlQuery);
		query.setParameter("facilityId", facility.getPersistentId(),  new DialectUUIDType());
		query.setParameter("startDateTime", new Timestamp(completedInterval.getStart().getMillis()));
		query.setParameter("endDateTime", new Timestamp(completedInterval.getEnd().getMillis()));
		query.setParameter("binWidth", binWidth.toStandardSeconds().getSeconds());
		query.setResultTransformer(new BasicTransformerAdapter() {
			private static final long serialVersionUID = 1L;

			@Override
			public Object transformTuple(Object[] tuple, String[] aliases) {
				
				int binNumber = (Integer) tuple[0];
				BigInteger binValue = (BigInteger) tuple[1];
				return new BinValue(completedInterval.getStart().plus(binWidth.multipliedBy(binNumber)), binWidth, binValue);
			}
		});
		
		
		List<BinValue> withMissingValues = new ArrayList<>();
		LinkedList<BinValue> dbValues = new LinkedList<>(query.list());
		DateTime binStart = completedInterval.getStart();
		while(binStart.isBefore(completedInterval.getEnd())) {
			if(!dbValues.isEmpty() 
 				&& binStart.getMillis() == dbValues.peek().getStart().getTime()) {
				withMissingValues.add(dbValues.poll());
			} else {
				withMissingValues.add(BinValue.missing(binStart, binWidth));
			}
			binStart = binStart.plus(binWidth);
		}
		return withMissingValues;	
	}

	@ToString
	public static class WorkerEventTypeGroup {
		@Getter @Setter
		WorkerEvent.EventType eventType;
		
		@Getter @Setter
		long count;
	}

	@SuppressWarnings("unchecked")
	public List<WorkerEventTypeGroup> groupWorkerEventsByType(Facility facility, Interval createdInterval, boolean resolved) {
        Criteria criteria = WorkerEvent.staticGetDao().createCriteria();
        criteria.setProjection(Projections.projectionList()
        		.add(Projections.groupProperty("eventType"), "eventType")
        		.add(Projections.rowCount(), "count"))
        	.add(Restrictions.eq("parent", facility))
        	.add(GenericDaoABC.createIntervalRestriction("created", createdInterval));
        	if (resolved){
        		criteria.add(Restrictions.isNotNull("resolution"));
        	} else {
        		criteria.add(Restrictions.isNull("resolution"));
        	}

        	criteria.setResultTransformer(new AliasToBeanResultTransformer(WorkerEventTypeGroup.class));
        	return criteria.list();
    }

	public List<String> getDistinct(Facility facility, String name) {
		Set<WorkerEvent.EventType> types = ImmutableSet.of(EventType.COMPLETE, EventType.SHORT);
		@SuppressWarnings("unchecked")
		List<String> result = (List<String>) WorkerEvent.staticGetDao().createCriteria()
			.add(Property.forName("parent").eq(facility))
			.add(Property.forName("eventType").in(types))
			.setProjection(Projections.distinct(Property.forName(name)))
			.list();
		return result;
	}

	public ResultDisplay<EventDisplay> getEventsForCheId(UUIDParam uuidParam, Integer limit) {
		limit = MoreObjects.firstNonNull(limit, 15);
		
		
		Criteria criteria = WorkerEvent.staticGetDao()
		.createCriteria()
		.add(Property.forName("devicePersistentId").eq(uuidParam.toString()));
		long total = countCriteria(criteria);
		
		criteria
		.addOrder(Order.desc("created"))
		.setMaxResults(limit);
		@SuppressWarnings("unchecked")
		List<WorkerEvent> entities = criteria.list();

		return new ResultDisplay<>(total, mapToEventDisplay(entities));
	}
	
	@SuppressWarnings("unchecked")
	public List<EventDisplay> getOrderEventsForOrderId(Facility facility, String orderDomainId) {
		DetachedCriteria detailUUIDs = DetachedCriteria.forClass(OrderDetail.class)
				.createAlias("parent", "order")
				.add(Property.forName("order.domainId").eq(orderDomainId))
				.add(Property.forName("order.parent").eq(facility))
				.add(Property.forName("active").eq(true))
				.setProjection(Property.forName("persistentId"));
		
		List<WorkerEvent> entities =  WorkerEvent.staticGetDao()
				.createCriteria()
				.add(Property.forName("orderDetailId").in(detailUUIDs))
				.addOrder(Order.asc("created"))
				.list();
		return mapToEventDisplay(entities);
	}


	public ResultDisplay<EventDisplay> getEventsForWorkerId(PageQuery query) throws JsonProcessingException {
		
		Criteria criteria = WorkerEvent.staticGetDao().createCriteria();
		criteria = query.toFilterCriteria(criteria);
		long total = countCriteria(criteria);
		criteria = query.toLimitedCriteria(criteria);
		@SuppressWarnings("unchecked")
		List<WorkerEvent> entities = criteria.list();
		String nextToken =query.getNextQueryToken();
		return new ResultDisplay<>(total, mapToEventDisplay(entities), nextToken);
	}
	

	private List<EventDisplay> mapToEventDisplay(List<WorkerEvent> workerEvents) {
		//Lazily convert worker event to event for display
		List<EventDisplay> results = Lists.transform(workerEvents, new Function<WorkerEvent, EventDisplay>() {
			@Override
			public EventDisplay apply(WorkerEvent event) {
				EventDisplay eventDisplay = EventDisplay.createEventDisplay(event);
				return eventDisplay;
			}
		});
		return results;

	}

}