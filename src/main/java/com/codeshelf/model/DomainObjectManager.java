package com.codeshelf.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.ContainerUse;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.WorkService;
import com.google.common.collect.ImmutableList;

/**
 * This class assists with object reporting and object purging
 * @author jonranstrom
 *
 */
public class DomainObjectManager {

	private static final Logger	LOGGER					= LoggerFactory.getLogger(WorkService.class);
	
	@Getter
	private Facility facility;
	
	public DomainObjectManager(Facility inFacility){
		facility = inFacility;
	}

	private Timestamp getDaysOldTimeStamp(int daysOldToCount) {
		// Get our reference timestamp relative to now.
		// One minute after the days counter to make unit tests that set things 2 days old return those on a 2 days old criteria.
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, (daysOldToCount * -1));
		cal.add(Calendar.MINUTE, 1);
		long desiredTimeLong = cal.getTimeInMillis();
		return new Timestamp(desiredTimeLong);
	}


	/**
	 * The goal is to report on objects that might be archived.
	 * This requires that we be in a transaction in context
	 * @return 
	 */
	public List<String> reportAchiveables(int daysOldToCount) {
		daysOldToCount = floorDays(daysOldToCount);
		// Get our reference timestamp relative to now.
		Timestamp desiredTime = getDaysOldTimeStamp(daysOldToCount);

		UUID facilityUUID = getFacility().getPersistentId();

		// Although an internal variable now, this is probably what would be returned to the UI
		ArrayList<String> reportables = new ArrayList<String>();

		String headerString = String.format("***Archivable Objects Summary. Objects older than %d days.***", daysOldToCount);
		reportables.add(headerString);

		// Work Instructions
		Criteria totalWisCrit = WorkInstruction.staticGetDao().createCriteria();
		totalWisCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		int totalWiCount = WorkInstruction.staticGetDao().countByCriteriaQuery(totalWisCrit);

		Criteria archiveableWisCrit = WorkInstruction.staticGetDao().createCriteria();
		archiveableWisCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		archiveableWisCrit.add(Restrictions.lt("created", desiredTime));
		int archiveableWiCount = WorkInstruction.staticGetDao().countByCriteriaQuery(archiveableWisCrit);
		String wiString = String.format(" WorkInstructions: %d archivable of %d total", archiveableWiCount, totalWiCount);
		reportables.add(wiString);

		String orderGroupString = String.format("*Objects that archive with orders... (Note, work instructions for those orders will also)*");
		reportables.add(orderGroupString);

		// Orders
		Criteria totalOrdersCrit = OrderHeader.staticGetDao().createCriteria();
		totalOrdersCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		int totalOrderCount = OrderHeader.staticGetDao().countByCriteriaQuery(totalOrdersCrit);

		Criteria archiveableOrderCrit = OrderHeader.staticGetDao().createCriteria();
		archiveableOrderCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		archiveableOrderCrit.add(Restrictions.lt("dueDate", desiredTime));
		int archiveableOrderCount = OrderHeader.staticGetDao().countByCriteriaQuery(archiveableOrderCrit);

		String orderString = String.format(" Orders: %d archivable of %d total", archiveableOrderCount, totalOrderCount);
		reportables.add(orderString);

		// Order Details
		Criteria totalDetailsCrit = OrderDetail.staticGetDao().createCriteria();
		totalDetailsCrit.createAlias("parent", "p");
		totalDetailsCrit.add(Restrictions.eq("p.parent.persistentId", facilityUUID));
		int totalDetailCount = OrderDetail.staticGetDao().countByCriteriaQuery(totalDetailsCrit);
		// int totalDetailCount = OrderDetail.staticGetDao().findByCriteriaQuery(crit).size();

		Criteria archiveableDetailsCrit = OrderDetail.staticGetDao().createCriteria();
		archiveableDetailsCrit.createAlias("parent", "p");
		archiveableDetailsCrit.add(Restrictions.eq("p.parent.persistentId", facilityUUID));
		archiveableDetailsCrit.add(Restrictions.lt("p.dueDate", desiredTime));
		int archiveableDetailCount = OrderDetail.staticGetDao().countByCriteriaQuery(archiveableDetailsCrit);
		// int archiveableDetailCount = OrderDetail.staticGetDao().findByCriteriaQuery(crit2).size();

		String detailString = String.format(" Details: %d archivable of %d total", archiveableDetailCount, totalDetailCount);
		reportables.add(detailString);

		// ContainerUse
		Criteria totalUsesCrit = ContainerUse.staticGetDao().createCriteria();
		totalUsesCrit.createAlias("parent", "p");
		totalUsesCrit.add(Restrictions.eq("p.parent.persistentId", facilityUUID));
		int totalUseCount = ContainerUse.staticGetDao().countByCriteriaQuery(totalUsesCrit);
		// int totalUseCount = ContainerUse.staticGetDao().findByCriteriaQuery(crit3).size();

		Criteria archiveableUsesCrit = ContainerUse.staticGetDao().createCriteria();
		archiveableUsesCrit.createAlias("parent", "p");
		archiveableUsesCrit.createAlias("orderHeader", "oh");
		archiveableUsesCrit.add(Restrictions.eq("p.parent.persistentId", facilityUUID));
		archiveableUsesCrit.add(Restrictions.isNotNull("orderHeader"));
		archiveableUsesCrit.add(Restrictions.lt("oh.dueDate", desiredTime));
		int archiveableUseCount = ContainerUse.staticGetDao().countByCriteriaQuery(archiveableUsesCrit);
		// int archiveableUseCount = ContainerUse.staticGetDao().findByCriteriaQuery(crit4).size();

		String useString = String.format(" ContainerUses: %d archivable of %d total", archiveableUseCount, totalUseCount);
		reportables.add(useString);

		String otherGroupString = String.format("*Additional objects that may archive with orders... (Note, not easy to determine how many will be purged until other objects are gone)*");
		reportables.add(otherGroupString);

		// Containers
		Criteria totalCntrsCrit = Container.staticGetDao().createCriteria();
		totalCntrsCrit.createAlias("parent", "p");
		totalCntrsCrit.add(Restrictions.eq("p.persistentId", facilityUUID));
		int totalCntrCount = Container.staticGetDao().countByCriteriaQuery(totalCntrsCrit);
		// int totalCntrCount = Container.staticGetDao().findByCriteriaQuery(crit5).size();
		String cntrString = String.format(" Containers: %d total", totalCntrCount);
		reportables.add(cntrString);

		// OrderGroup
		Criteria totalGroupsCrit = OrderGroup.staticGetDao().createCriteria();
		totalGroupsCrit.createAlias("parent", "p");
		totalGroupsCrit.add(Restrictions.eq("p.persistentId", facilityUUID));
		int totalGroupCount = OrderGroup.staticGetDao().findByCriteriaQuery(totalGroupsCrit).size();
		String groupString = String.format(" OrderGroups: %d total", totalGroupCount);
		reportables.add(groupString);

		for (String s : reportables) {
			LOGGER.info(s);
		}
		return reportables;

	}

	/**
	 * delinks, then deletes
	 */
	private void safelyDeleteWorkInstructionList(List<WorkInstruction> wiList) {
		int deletedCount = 0;
		for (WorkInstruction wi : wiList) {

			OrderDetail detail = wi.getOrderDetail();
			if (detail != null)
				detail.removeWorkInstruction(wi);
			Che che = wi.getAssignedChe();
			if (che != null)
				che.removeWorkInstruction(wi);
			try {
				WorkInstruction.staticGetDao().delete(wi);
			} catch (DaoException e) {
				LOGGER.error("safelyDeleteWorkInstructionList", e);
			}
			deletedCount++;

			if (deletedCount % 100 == 0)
				LOGGER.info("deleted {} WorkInstructions ", deletedCount);

		}

		if (deletedCount % 100 != 0)
			LOGGER.info("deleted {} WorkInstructions ", deletedCount);
	}

	/**
	 * Assumes work instructions are already delinked. Normally call safelyDeleteWorkInstructionList first.
	 * This delinks from the order.
	 */
	private void safelyDeleteDetailsList(List<OrderDetail> detailsList) {
		int deletedCount = 0;
		for (OrderDetail detail : detailsList) {

			OrderHeader order = detail.getParent();
			if (order != null)
				order.removeOrderDetail(detail);

			try {
				OrderDetail.staticGetDao().delete(detail);
			} catch (DaoException e) {
				LOGGER.error("safelyDeleteDetailsList", e);
			}
			deletedCount++;

			if (deletedCount % 100 == 0)
				LOGGER.info("deleted {} OrderDetails ", deletedCount);

		}
		if (deletedCount % 100 != 0)
			LOGGER.info("deleted {} OrderDetails ", deletedCount);
	}

	/**
	 * This purge follows our parent child pattern
	 */
	private void purgeWorkInstructions(int daysOldToCount, int maxToPurgeAtOnce) {
		Timestamp desiredTime = getDaysOldTimeStamp(daysOldToCount);
		UUID facilityUUID = getFacility().getPersistentId();

		/*
		List<WorkInstruction> wiList = WorkInstruction.staticGetDao()
			.findByFilter(ImmutableList.<Criterion> of(Restrictions.eq("parent.persistentId", facilityUUID),
				Restrictions.lt("created", desiredTime)));
		*/

		Criteria archiveableWisCrit = WorkInstruction.staticGetDao().createCriteria();
		archiveableWisCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		archiveableWisCrit.add(Restrictions.lt("created", desiredTime));
		int wantToPurge = WorkInstruction.staticGetDao().countByCriteriaQuery(archiveableWisCrit);

		Criteria archiveableWisCrit2 = WorkInstruction.staticGetDao().createCriteria();
		archiveableWisCrit2.add(Restrictions.eq("parent.persistentId", facilityUUID));
		archiveableWisCrit2.add(Restrictions.lt("created", desiredTime));
		archiveableWisCrit2.setMaxResults(maxToPurgeAtOnce);
		List<WorkInstruction> wiList = WorkInstruction.staticGetDao().findByCriteriaQuery(archiveableWisCrit2);

		// int wantToPurge = wiList.size();
		int willPurge = Math.min(wantToPurge, maxToPurgeAtOnce);
		boolean partial = wantToPurge > willPurge;
		if (partial)
			LOGGER.info("purging only {}  of {} purgable work instructions", willPurge, wantToPurge);
		else
			LOGGER.info("purging {} work instructions", willPurge);

		safelyDeleteWorkInstructionList(wiList);
	}

	/**
	 * This purge assumes that order.delete() follows our parent child pattern. It is not done here.
	 */
	private void purgeOrders(int daysOldToCount, int maxToPurgeAtOnce) {
		Timestamp desiredTime = getDaysOldTimeStamp(daysOldToCount);
		UUID facilityUUID = getFacility().getPersistentId();

		LOGGER.info("Phase 1 of order purge: get the batch of orders to purge");

		Criteria archiveableOrderCrit = OrderHeader.staticGetDao().createCriteria();
		archiveableOrderCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		archiveableOrderCrit.add(Restrictions.lt("dueDate", desiredTime));
		int wantToPurge = OrderHeader.staticGetDao().countByCriteriaQuery(archiveableOrderCrit);

		Criteria archiveableOrderCrit2 = OrderHeader.staticGetDao().createCriteria();
		archiveableOrderCrit2.add(Restrictions.eq("parent.persistentId", facilityUUID));
		archiveableOrderCrit2.add(Restrictions.lt("dueDate", desiredTime));
		archiveableOrderCrit2.setMaxResults(maxToPurgeAtOnce);
		// List<OrderHeader> orders = OrderHeader.staticGetDao().findByCriteriaQuery(archiveableOrderCrit2);
		List<UUID> uuidList = OrderHeader.staticGetDao().getUUIDListByCriteriaQuery(archiveableOrderCrit2);

		// int wantToPurge = orders.size();
		int willPurge = Math.min(wantToPurge, maxToPurgeAtOnce);
		boolean partial = wantToPurge > willPurge;

		if (partial)
			LOGGER.info("purging only {}  of {} purgable orders and owned related objects", willPurge, wantToPurge);
		else
			LOGGER.info("purging {} orders and owned related objects", willPurge);

		// Trying to speed up by not relying quite so much on the hibernate delete cascade.
		// Result: not much improvement in time. But much nicer logging about the process.
		LOGGER.info("Phase 2 of order purge: assemble list of details for these orders");

		/*
		List<UUID> uuidList = new ArrayList<UUID>();
		for (OrderHeader order : orders) {
			uuidList.add(order.getPersistentId());
		}
		*/

		// 500 orders with 2900 details assembles in 2 seconds using findByParentPersistentIdList
		List<OrderDetail> details = OrderDetail.staticGetDao().findByParentPersistentIdList(uuidList);

		// My test case did not have enough work instruction to know if this helped or not.
		LOGGER.info("Phase 3 of order purge: assemble work instructions from the details");
		List<WorkInstruction> wis = new ArrayList<WorkInstruction>();
		for (OrderDetail detail : details) {
			List<WorkInstruction> oneOrderWis = detail.getWorkInstructions();
			wis.addAll(oneOrderWis);
		}

		LOGGER.info("Phase 4 of order purge: delete the assembled work instructions, which delinks from details and che.");
		safelyDeleteWorkInstructionList(wis);

		LOGGER.info("Phase 5 of order purge: delete the details");
		safelyDeleteDetailsList(details);

		LOGGER.info("Phase 6 of order purge: delete the orders which delinks from container");

		int deletedCount = 0;
		// for (OrderHeader order : orders) {
		for (UUID orderUuid : uuidList) {
			try {
				OrderHeader order = OrderHeader.staticGetDao().findByPersistentId(orderUuid);
				if (order != null) {
					order.delete();
					deletedCount++;
					if (deletedCount % 100 == 0)
						LOGGER.info("deleted {} Orders ", deletedCount);
				}
			} catch (DaoException e) {
				LOGGER.error("purgeOrders", e);
			}

		}
		if (deletedCount % 100 != 0)
			LOGGER.info("deleted {} Orders ", deletedCount);
		// Note: after you see this, there is the transaction commit. That seems to run in linear time:
		// 500 orders with 2900 details commits in 39 seconds
		// 1000 orders with 5800 details commits in 76 seconds
	}

	/**
	 * For many sites, we may get a new "container" for each order. If so, certainly the old containers should be purged
	 * But we would like to avoid purging permanent containers if possible.
		 */
	private void purgeContainers(int daysOldToCount, int maxToPurgeAtOnce) {
		UUID facilityUUID = getFacility().getPersistentId();
		// The main concern is that we should not purge the container if there are any remaining ContainerUses
		// Containers have an active flag. So, if inactive, probably worth purging anyway.

		// We cannot limit the returned containers because we have no easy way to find the subset that do not have containerUse pointing at them. 
		List<Container> cntrs = Container.staticGetDao()
			.findByFilter(ImmutableList.<Criterion> of(Restrictions.eq("parent.persistentId", facilityUUID)));

		Map<UUID, Container> referencedMap = new HashMap<UUID, Container>();

		LOGGER.info("examining {} Containers to consider purging", cntrs.size());
		LOGGER.info("examining ContainerUses linked to containers");
		// ContainerUse. Can not limit, or we would make incorrect decisions about which were deleteable.
		Criteria crit = ContainerUse.staticGetDao().createCriteria();
		crit.createAlias("parent", "p");
		crit.add(Restrictions.eq("p.parent.persistentId", facilityUUID));
		List<ContainerUse> uses = ContainerUse.staticGetDao().findByCriteriaQuery(crit);

		// Add each container that ContainerUse references to map
		for (ContainerUse cntrUse : uses) {
			Container referencedCntr = cntrUse.getParent();
			if (!referencedMap.containsKey(referencedCntr.getPersistentId())) {
				referencedMap.put(referencedCntr.getPersistentId(), referencedCntr);
			}
		}

		LOGGER.info("examining WorkInstructions linked to containers");
		Criteria crit2 = WorkInstruction.staticGetDao().createCriteria();
		crit2.add(Restrictions.eq("parent.persistentId", facilityUUID));
		List<WorkInstruction> wis = WorkInstruction.staticGetDao().findByCriteriaQuery(crit2);

		// Add each container that the work instruction references to map
		for (WorkInstruction wi : wis) {
			Container referencedCntr = wi.getContainer();
			if (!referencedMap.containsKey(referencedCntr.getPersistentId())) {
				referencedMap.put(referencedCntr.getPersistentId(), referencedCntr);
			}
		}

		int deletedCount = 0;
		for (Container cntr : cntrs) {
			boolean shouldDelete = false;
			if (!referencedMap.containsKey(cntr.getPersistentId())) // This container had no containerUses and no work instructions
				shouldDelete = true;

			if (shouldDelete) {
				try {
					Container.staticGetDao().delete(cntr);
				} catch (DaoException e) {
					LOGGER.error("purgeContainers", e);
				}
				deletedCount++;
				if (deletedCount >= maxToPurgeAtOnce)
					break;

				if (deletedCount % 100 == 0)
					LOGGER.info("deleted {} Containers ", deletedCount);
			}
		}
		LOGGER.info("deleted {} Containers ", deletedCount);

	}


	/**
	* The goal is to delete what reported on with the same parameters.
	* However, there are several sub-deletes, controlled by the className parameter
	* Logs an error on unsupported class name.
	* This requires that we be in a transaction in context
	*/
	public void purgeOldObjects(int daysOldToCount, Class<? extends IDomainObject> inCls, int maxToPurgeAtOnce) {
		if (inCls == null) {
			LOGGER.error("null class name in purgeOldObjects");
			return;
		}
		daysOldToCount = floorDays(daysOldToCount);

		boolean foundGoodClassName = false;
		// String nameToMatch = WorkInstruction.class.getName();
		if (WorkInstruction.class.isAssignableFrom(inCls)) {
			foundGoodClassName = true;
			purgeWorkInstructions(daysOldToCount, maxToPurgeAtOnce);
		}

		else if (OrderHeader.class.isAssignableFrom(inCls)) {
			foundGoodClassName = true;
			// only do 200 orders at a time. See DEV-1144
			maxToPurgeAtOnce = Math.min(200, maxToPurgeAtOnce);
			purgeOrders(daysOldToCount, maxToPurgeAtOnce);
		}

		else if (Container.class.isAssignableFrom(inCls)) {
			foundGoodClassName = true;
			purgeContainers(daysOldToCount, maxToPurgeAtOnce);
		}

		if (!foundGoodClassName) {
			LOGGER.error("unimplement class name: {} in purgeOldObjects", inCls);
		}
	}

	private int floorDays(int daysOldToCount) {
		return Math.max(daysOldToCount, 1);
	}
	
	/**
	 * This function unrelated to data purge. Used in a test function
	 * Get up maxNeeded orders that are active and not complete
	 */
	public List<OrderHeader> getSomeUncompletedOrders(int maxNeeded){
		UUID facilityUUID = getFacility().getPersistentId();
		Criteria orderCrit = OrderHeader.staticGetDao().createCriteria();
		orderCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		orderCrit.add(Restrictions.ne("status",OrderStatusEnum.COMPLETE));
		orderCrit.add(Restrictions.eq("active",true));
		return OrderHeader.staticGetDao().findByCriteriaQuery(orderCrit);
	
	}
}