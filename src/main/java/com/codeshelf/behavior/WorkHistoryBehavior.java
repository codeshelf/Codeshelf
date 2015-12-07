package com.codeshelf.behavior;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;

import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.responses.EventDisplay;
import com.codeshelf.api.responses.ResultDisplay;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.WorkerEvent;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class WorkHistoryBehavior {

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


	public ResultDisplay<EventDisplay> getEventsForWorkerId(Facility facility, String badgeId, Integer limit) {
		limit = MoreObjects.firstNonNull(limit, 15);
		
		
		Criteria criteria = WorkerEvent.staticGetDao()
		.createCriteria()
		.add(Property.forName("facility").eq(facility))
		.add(Property.forName("workerId").eq(badgeId));
		long total = countCriteria(criteria);
		
		criteria
		.addOrder(Order.desc("created"))
		.setMaxResults(limit);
		@SuppressWarnings("unchecked")
		List<WorkerEvent> entities = criteria.list();
		return new ResultDisplay<>(total, mapToEventDisplay(entities));
	}
	
	//side effect of changing the query temporarily since cloning isn't really supported
	private long countCriteria(Criteria criteria) {
		//Turn into a count query
		Criteria countCriteria = criteria.setProjection(Projections.rowCount());
		Long total = (Long) countCriteria.uniqueResult();

		//Turn back into entity query
		criteria.setProjection(null);
		criteria.setResultTransformer(Criteria.ROOT_ENTITY);
		return total;
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
