package com.codeshelf.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import lombok.Getter;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.WorkInstructionCsvBean;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.ContainerUse;
import com.codeshelf.model.domain.DomainObjectABC;
import com.codeshelf.model.domain.ExportMessage;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.ImportReceipt;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Resolution;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.persistence.TenantPersistenceService;

/**
 * This class assists with object reporting and object purging
 * @author jonranstrom
 *
 */
public class DomainObjectManager {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(DomainObjectManager.class);

	@Getter
	private Facility			facility;

	public DomainObjectManager(Facility inFacility) {
		facility = inFacility;
	}

	public class FacilityPickParameters {
		@Getter
		private int	picksLastOneHour			= 0;
		@Getter
		private int	picksLastTwentyFourHours	= 0;

		FacilityPickParameters() {
		}
	}

	public static Timestamp getDaysOldTimeStamp(int daysOldToCount) {
		// Get our reference timestamp relative to now.
		// One minute after the days counter to make unit tests that set things 2 days old return those on a 2 days old criteria.
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, (daysOldToCount * -1));
		// add a minute is useful for purging test. For activity last one day, the minute add is kind of wrong. 
		cal.add(Calendar.MINUTE, 1);
		long desiredTimeLong = cal.getTimeInMillis();
		return new Timestamp(desiredTimeLong);
	}

	private Timestamp getHoursOldTimeStamp(int hoursOldToCount) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR, (hoursOldToCount * -1));
		// Don't add or subtract a minute for this one. Just do what it says.
		long desiredTimeLong = cal.getTimeInMillis();
		return new Timestamp(desiredTimeLong);
	}

	private String getArchivableString(String objectName, Criteria totalQuery, Criteria archiveQuery, ITypedDao<?> inDao) {
		int totalCount = inDao.countByCriteriaQuery(totalQuery);
		int archiveCount = inDao.countByCriteriaQuery(archiveQuery);
		return String.format(" %-19s: %4d archivable of %6d total", objectName, archiveCount, totalCount);
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

		// used for logging, and the thing returned
		ArrayList<String> reportables = new ArrayList<String>();

		String headerString = String.format("***Archivable Objects Summary. Objects older than %d days.***", daysOldToCount);
		reportables.add(headerString);

		// Work Instructions
		Criteria totalWisCrit = WorkInstruction.staticGetDao().createCriteria();
		totalWisCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		Criteria archiveableWisCrit = getWiPurgeCriterion(desiredTime, facilityUUID);
		reportables.add(getArchivableString("WorkInstruction", totalWisCrit, archiveableWisCrit, WorkInstruction.staticGetDao()));

		// Orders
		Criteria totalOrdersCrit = OrderHeader.staticGetDao().createCriteria();
		totalOrdersCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		Criteria archiveableOrderCrit = getOrderPurgeCriterion(desiredTime, facilityUUID);
		reportables.add(getArchivableString("Order", totalOrdersCrit, archiveableOrderCrit, OrderHeader.staticGetDao()));

		// Order Details
		Criteria totalDetailsCrit = OrderDetail.staticGetDao().createCriteria();
		totalDetailsCrit.createAlias("parent", "p");
		totalDetailsCrit.add(Restrictions.eq("p.parent.persistentId", facilityUUID));

		Criteria archiveableDetailsCrit = OrderDetail.staticGetDao().createCriteria();
		archiveableDetailsCrit.createAlias("parent", "p");
		archiveableDetailsCrit.add(Restrictions.eq("p.parent.persistentId", facilityUUID));
		archiveableDetailsCrit.add(Restrictions.lt("p.dueDate", desiredTime));

		reportables.add(getArchivableString("OrderDetail", totalDetailsCrit, archiveableDetailsCrit, OrderDetail.staticGetDao()));

		// Replenish Order Details
		Criteria totalReplenishDetailsCrit = OrderDetail.staticGetDao().createCriteria();
		totalReplenishDetailsCrit.createAlias("parent", "p");
		totalReplenishDetailsCrit.add(Restrictions.eq("p.orderType", OrderTypeEnum.REPLENISH));
		Criteria archiveableReplenishDetailsCrit = getReplenishDetailsPurgeCriterion(desiredTime, facilityUUID);
		reportables.add(getArchivableString("ReplenOrderDetail",
			totalReplenishDetailsCrit,
			archiveableReplenishDetailsCrit,
			OrderDetail.staticGetDao()));

		// ContainerUse
		Criteria totalUsesCrit = ContainerUse.staticGetDao().createCriteria();
		totalUsesCrit.createAlias("parent", "p");
		totalUsesCrit.add(Restrictions.eq("p.parent.persistentId", facilityUUID));

		Criteria archiveableUsesCrit = ContainerUse.staticGetDao().createCriteria();
		archiveableUsesCrit.createAlias("parent", "p");
		archiveableUsesCrit.createAlias("orderHeader", "oh");
		archiveableUsesCrit.add(Restrictions.eq("p.parent.persistentId", facilityUUID));
		archiveableUsesCrit.add(Restrictions.isNotNull("orderHeader"));
		archiveableUsesCrit.add(Restrictions.lt("oh.dueDate", desiredTime));

		reportables.add(getArchivableString("ContainerUse", totalUsesCrit, archiveableUsesCrit, ContainerUse.staticGetDao()));

		// ImportReceipts
		Criteria totalReceiptCrit = ImportReceipt.staticGetDao().createCriteria();
		totalReceiptCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		Criteria archiveableReceiptCrit = getImportReceiptPurgeCriterion(desiredTime, facilityUUID);
		reportables.add(getArchivableString("ImportReceipt", totalReceiptCrit, archiveableReceiptCrit, ImportReceipt.staticGetDao()));

		// ExportMessages
		Criteria totalMessageCrit = ExportMessage.staticGetDao().createCriteria();
		totalMessageCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		Criteria archiveableMessageCrit = getExportMessagePurgeCriterion(desiredTime, facilityUUID);
		reportables.add(getArchivableString("ExportMessage", totalMessageCrit, archiveableMessageCrit, ExportMessage.staticGetDao()));

		// WorkInstructionBeans
		Criteria totalBeanCrit = WorkInstructionCsvBean.staticGetDao().createCriteria();
		totalBeanCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		Criteria archiveableBeanCrit = getWiBeanPurgeCriterion(desiredTime, facilityUUID);
		reportables.add(getArchivableString("WorkInstructionBean",
			totalBeanCrit,
			archiveableBeanCrit,
			WorkInstructionCsvBean.staticGetDao()));

		// WorkerEvents
		Criteria totalEventCrit = WorkerEvent.staticGetDao().createCriteria();
		totalEventCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		Criteria archiveableEventCrit = getWorkerEventPurgeCriterion(desiredTime, facilityUUID);
		reportables.add(getArchivableString("WorkerEvent", totalEventCrit, archiveableEventCrit, WorkerEvent.staticGetDao()));

		// Objects where it is not easy to know how many will be be deleted until all of the above purges are done

		String otherGroupString = String.format("*Additional object totals (Some of these may archive)*");
		reportables.add(otherGroupString);

		// Containers
		Criteria totalCntrsCrit = Container.staticGetDao().createCriteria();
		totalCntrsCrit.createAlias("parent", "p");
		totalCntrsCrit.add(Restrictions.eq("p.persistentId", facilityUUID));
		int totalCntrCount = Container.staticGetDao().countByCriteriaQuery(totalCntrsCrit);
		String cntrString = String.format(" Container          : %6d total", totalCntrCount);
		reportables.add(cntrString);

		// OrderGroup
		Criteria totalGroupsCrit = OrderGroup.staticGetDao().createCriteria();
		totalGroupsCrit.createAlias("parent", "p");
		totalGroupsCrit.add(Restrictions.eq("p.persistentId", facilityUUID));
		int totalGroupCount = OrderGroup.staticGetDao().findByCriteriaQuery(totalGroupsCrit).size();
		String groupString = String.format(" OrderGroup         : %6d total", totalGroupCount);
		reportables.add(groupString);

		String toLogStr = "";
		for (String s : reportables) {
			// from v25, lets log as a single string
			// LOGGER.info(s);
			toLogStr += String.format("%s%n", s);
		}
		LOGGER.info(toLogStr);
		return reportables;

	}

	/**
	 * The common criterion for Order purge. 
	 */
	private Criteria getOrderPurgeCriterion(Timestamp desiredTime, UUID facilityUUID) {
		Criteria orderCrit = OrderHeader.staticGetDao().createCriteria();
		orderCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		orderCrit.add(Restrictions.lt("dueDate", desiredTime));
		return orderCrit;
	}

	/**
	 * This returns the full list of UUIDs of OrderHeaders whose dueDate is older than daysOld before now.
	 */
	public List<UUID> getOrderUuidsToPurge(int daysOld) {
		Timestamp desiredTime = getDaysOldTimeStamp(daysOld);
		UUID facilityUUID = getFacility().getPersistentId();
		Criteria orderCrit = getOrderPurgeCriterion(desiredTime, facilityUUID);
		List<UUID> uuidList = OrderHeader.staticGetDao().getUUIDListByCriteriaQuery(orderCrit);
		return uuidList;
	}

	/**
	 * The common criterion for replenish details purge. 
	 */
	private Criteria getReplenishDetailsPurgeCriterion(Timestamp desiredTime, UUID facilityUUID) {
		Criteria detailsCrit = OrderDetail.staticGetDao().createCriteria();
		detailsCrit.createAlias("parent", "p");
		detailsCrit.add(Restrictions.eq("p.parent.persistentId", facilityUUID));
		detailsCrit.add(Restrictions.eq("p.orderType", OrderTypeEnum.REPLENISH));
		detailsCrit.add(Restrictions.lt("updated", desiredTime));
		return detailsCrit;
	}

	/**
	 * This returns the full list of UUIDs of OrderDetails from Replenish orders whose 'updated' value is older than daysOld before now.
	 */
	public List<UUID> getReplenishDetailsUuidsToPurge(int daysOld) {
		Timestamp desiredTime = getDaysOldTimeStamp(daysOld);
		UUID facilityUUID = getFacility().getPersistentId();
		Criteria detailsCrit = getReplenishDetailsPurgeCriterion(desiredTime, facilityUUID);
		List<UUID> uuidList = OrderDetail.staticGetDao().getUUIDListByCriteriaQuery(detailsCrit);
		return uuidList;
	}

	/**
	 * The common criterion for WorkerEvent purge. 
	 */
	private Criteria getWorkerEventPurgeCriterion(Timestamp desiredTime, UUID facilityUUID) {
		Criteria eventCrit = WorkerEvent.staticGetDao().createCriteria();
		eventCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		eventCrit.add(Restrictions.lt("created", desiredTime));
		return eventCrit;
	}

	/**
	 * This returns the full list of UUIDs of WorkerEvents whose event date is older than daysOld before now.
	 */
	public List<UUID> getWorkerEventUuidsToPurge(int daysOld) {
		Timestamp desiredTime = getDaysOldTimeStamp(daysOld);
		UUID facilityUUID = getFacility().getPersistentId();
		Criteria eventCrit = getWorkerEventPurgeCriterion(desiredTime, facilityUUID);
		List<UUID> uuidList = WorkerEvent.staticGetDao().getUUIDListByCriteriaQuery(eventCrit);
		return uuidList;
	}

	/**
	 * The common criterion for WorkInstructionCsvBean purge. 
	 */
	private Criteria getWiBeanPurgeCriterion(Timestamp desiredTime, UUID facilityUUID) {
		Criteria eventCrit = WorkInstructionCsvBean.staticGetDao().createCriteria();
		eventCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		eventCrit.add(Restrictions.lt("updated", desiredTime));
		return eventCrit;
	}
	
	/**
	 * This returns the full list of UUIDs of WorkInstructionCsvBean whose update date is older than daysOld before now.
	 */
	public List<UUID> getWorkInstructionCsvBeanUuidsToPurge(int daysOld) {
		Timestamp desiredTime = getDaysOldTimeStamp(daysOld);
		UUID facilityUUID = getFacility().getPersistentId();
		Criteria eventCrit = getWiBeanPurgeCriterion(desiredTime, facilityUUID);
		List<UUID> uuidList = WorkInstructionCsvBean.staticGetDao().getUUIDListByCriteriaQuery(eventCrit);
		return uuidList;
	}

	/**
	 * Common criterion for ExportMessage. Only complexity here is that prior to v24, export message objects did not have a created time.
	 * Therefore, add null to this criterion
	 */
	private Criteria getExportMessagePurgeCriterion(Timestamp desiredTime, UUID facilityUUID) {
		Criteria msgCrit = ExportMessage.staticGetDao().createCriteria();
		
		/*  This works */
		// warning .eq("created", null) does not work. Use isNull()
		Disjunction or = Restrictions.disjunction();
		or.add(Restrictions.isNull("created"));
		or.add(Restrictions.lt("created", desiredTime));
		msgCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		msgCrit.add(or);
		// */
		
		/* This also works
		// Expression :  (c1 OR c2) AND (c3) 
		Criterion c1 = Restrictions.isNull("created");
		Criterion c2 = Restrictions.lt("created", desiredTime);
		Criterion c3 = Restrictions.eq("parent.persistentId", facilityUUID);
		Criterion c4 = Restrictions.and(Restrictions.or(c1, c2), c3);
		msgCrit.add(c4);
		*/
		
		return msgCrit;
	}

	/**
	 * This returns the full list of UUIDs of ExportMessage record whose created date is older than daysOld before now.
	 */
	public List<UUID> getExportMessageUuidsToPurge(int daysOld) {
		Timestamp desiredTime = getDaysOldTimeStamp(daysOld);
		UUID facilityUUID = getFacility().getPersistentId();
		Criteria msgCrit = getExportMessagePurgeCriterion(desiredTime, facilityUUID);
		List<UUID> uuidList = ExportMessage.staticGetDao().getUUIDListByCriteriaQuery(msgCrit);
		return uuidList;
	}

	/**
	 * The common criterion for WorkInstructionCsvBean purge. 
	 */
	private Criteria getImportReceiptPurgeCriterion(Timestamp desiredTime, UUID facilityUUID) {
		Criteria receiptCrit = ImportReceipt.staticGetDao().createCriteria();
		receiptCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		receiptCrit.add(Restrictions.lt("received", desiredTime));
		return receiptCrit;
	}

	/**
	 * This returns the full list of UUIDs of ImportReceipt whose receive date is older than daysOld before now.
	 */
	public List<UUID> getImportReceiptUuidsToPurge(int daysOld) {
		Timestamp desiredTime = getDaysOldTimeStamp(daysOld);
		UUID facilityUUID = getFacility().getPersistentId();
		Criteria receiptCrit = getImportReceiptPurgeCriterion(desiredTime, facilityUUID);
		List<UUID> uuidList = ImportReceipt.staticGetDao().getUUIDListByCriteriaQuery(receiptCrit);
		return uuidList;
	}

	/**
	 * The common criterion for WorkInstructionCsvBean purge. 
	 */
	private Criteria getWiPurgeCriterion(Timestamp desiredTime, UUID facilityUUID) {
		Criteria wiCrit = WorkInstruction.staticGetDao().createCriteria();
		wiCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		wiCrit.add(Restrictions.lt("created", desiredTime));
		return wiCrit;
	}

	/**
	 * This returns the full list of UUIDs of workInstructions whose created date is older than daysOld before now.
	 */
	public List<UUID> getWorkInstructionUuidsToPurge(int daysOld) {
		Timestamp desiredTime = getDaysOldTimeStamp(daysOld);
		UUID facilityUUID = getFacility().getPersistentId();
		Criteria wiCrit = getWiPurgeCriterion(desiredTime, facilityUUID);
		List<UUID> uuidList = WorkInstruction.staticGetDao().getUUIDListByCriteriaQuery(wiCrit);
		return uuidList;
	}

	/**
	 * This returns the full list of UUIDs of Containers to delete.
	 * Does not directly respect the daysOld parameter. There is no good timestamp on the container.
	 * Indirect because containerUse and Wis will be deleted first by timestamp. Container only deletes after those that reference it are gone.
	 */
	public List<UUID> getCntrUuidsToPurge(int daysOld) {
		// Time this routine as this might be a bit slow
		long startMillis = System.currentTimeMillis();

		// The main concern is that we should not purge the container if there are any remaining ContainerUses
		// Containers have an active flag. So, if inactive, probably worth purging anyway.

		Session session = TenantPersistenceService.getInstance().getSession();
		Query query = session.createQuery(
			"SELECT c.persistentId\n" + 
			"FROM Container c\n" + 
			"	LEFT JOIN c.uses cu\n" + 
			"	LEFT JOIN c.workInstructions wi\n" + 
			"WHERE c.parent = :facility AND cu.persistentId IS NULL AND wi.persistentId IS NULL\n");
		query.setParameter("facility", getFacility());
		@SuppressWarnings("unchecked")
		List<UUID> results = query.list();
		long endMillis = System.currentTimeMillis();
		if (endMillis - startMillis > 1000){
			LOGGER.info("Took {}ms to determine that {} containers should be deleted", endMillis - startMillis , results.size());
		}
		return results;
	}

	/**
	 * Purge these containers all in the current transaction.
	 */
	public int purgeSomeCntrs(List<UUID> cntrUuids) {
		final int MAX_CNTR_PURGE = 500;
		int wantToPurge = cntrUuids.size();
		int willPurge = Math.min(wantToPurge, MAX_CNTR_PURGE);
		if (wantToPurge > MAX_CNTR_PURGE) {
			LOGGER.error("Limiting container delete batch size to {}. Called for {}.", MAX_CNTR_PURGE, wantToPurge);
		}
		int deletedCount = 0;
		List<Container> containers = Container.staticGetDao().findByPersistentIdList(cntrUuids);
		for (Container cntr : containers) {
			// just protection against bad call
			if (deletedCount > willPurge)
				break;
			try {
				if (cntr != null) {
					Container.staticGetDao().delete(cntr);
					// This could fail! Make a new ContainerUse or WorkInstruction for a container after the uuid list was created.
					// Too expensive to check again, and will not happen much. Let the database error catch it.
					deletedCount++;
				}
			} catch (DaoException e) {
				LOGGER.error("purgeSomeCntrs", e);
			}
		}
		return deletedCount;
	}

	/**
	 * Purge these WorkerEvents all in the current transaction.
	 * And any Resolution objects the events point to.
	 */
	public int purgeSomeWorkerEvents(List<UUID> workerEventUuids) {
		final int MAX_EVENT_PURGE = 500;
		int wantToPurge = workerEventUuids.size();
		int willPurge = Math.min(wantToPurge, MAX_EVENT_PURGE);
		if (wantToPurge > MAX_EVENT_PURGE) {
			LOGGER.error("Limiting workerEvent delete batch size to {}. Called for {}.", MAX_EVENT_PURGE, wantToPurge);
		}
		int deletedCount = 0;
		for (UUID eventUuid : workerEventUuids) {
			// just protection against bad call
			if (deletedCount > willPurge)
				break;
			try {
				WorkerEvent event = WorkerEvent.staticGetDao().findByPersistentId(eventUuid);
				if (event != null) {
					// Worker events are a little special. They may or may not have a resolution. If so, delete the resolution, no matter the date on the resolution.
					Resolution resolution = event.getResolution();
					if (resolution != null) {
						event.setResolution(null); // necessary?
						Resolution.staticGetDao().delete(resolution);
					}
					WorkerEvent.staticGetDao().delete(event);
					deletedCount++;
				}
			} catch (DaoException e) {
				LOGGER.error("purgeSomeWorkerEvents", e);
			}
		}
		return deletedCount;
	}

	/**
	 * Purge these work instructions all in the current transaction.
	 * Work instructions must be delinked from owning detail and che as they have lazy loaded lists
	 */
	public int purgeSomeWorkInstructions(List<UUID> wiUuids) {
		final int MAX_WI_PURGE = 500;
		int wantToPurge = wiUuids.size();
		int willPurge = Math.min(wantToPurge, MAX_WI_PURGE);
		if (wantToPurge > MAX_WI_PURGE) {
			LOGGER.error("Limiting work instruction delete batch size to {}. Called for {}.", MAX_WI_PURGE, wantToPurge);
		}
		int deletedCount = 0;
		for (UUID wiUuid : wiUuids) {
			// just protection against bad call
			if (deletedCount > willPurge)
				break;
			try {
				WorkInstruction wi = WorkInstruction.staticGetDao().findByPersistentId(wiUuid);
				if (wi != null) {
					// Work instructions are in lists for various parents. Make sure they are removed.
					OrderDetail detail = wi.getOrderDetail();
					if (detail != null)
						detail.removeWorkInstruction(wi);
					Che che = wi.getAssignedChe();
					if (che != null)
						che.removeWorkInstruction(wi);

					WorkInstruction.staticGetDao().delete(wi);
					deletedCount++;
				}
			} catch (DaoException e) {
				LOGGER.error("purgeSomeWorkInstructions", e);
			}
		}
		return deletedCount;
	}

	/**
	 * Purge these export message records all in the current transaction.
	 * No complexities, so may call through to the simple batch purge
	 */
	public int purgeSomeExportMessages(List<UUID> receiptUuids) {
		final int MAX_MESSAGE_PURGE = 500;
		return simplePurge(receiptUuids, MAX_MESSAGE_PURGE, ExportMessage.staticGetDao());
	}

	/**
	 * Purge these import receipts all in the current transaction.
	 * No complexities, so may call through to the simple batch purge
	 */
	public int purgeSomeImportReceipts(List<UUID> receiptUuids) {
		final int MAX_RECEIPT_PURGE = 500;
		return simplePurge(receiptUuids, MAX_RECEIPT_PURGE, ImportReceipt.staticGetDao());
	}

	/**
	 * Purge these work instruction beans all in the current transaction.
	 * No complexities, so may call through to the simple batch purge
	 */
	public int purgeSomeWiCsvBeans(List<UUID> wiBeanUuids) {
		final int MAX_WIBEAN_PURGE = 500;
		return simplePurge(wiBeanUuids, MAX_WIBEAN_PURGE, WorkInstructionCsvBean.staticGetDao());
	}

	/**
	 * Purge objects that have no complex relationships requiring fancier code
	 */
	private int simplePurge(List<UUID> objectUuids, int programMaxBatch, ITypedDao<?> theDao) {
		int wantToPurge = objectUuids.size();
		int willPurge = Math.min(wantToPurge, programMaxBatch);
		if (wantToPurge > programMaxBatch) {
			LOGGER.error("Limiting purge delete batch size to {}. Called for {}.", programMaxBatch, wantToPurge);
		}
		int deletedCount = 0;
		for (UUID objectUuid : objectUuids) {
			// just protection against bad call
			if (deletedCount > willPurge)
				break;
			try {
				DomainObjectABC object = (DomainObjectABC) theDao.findByPersistentId(objectUuid);
				if (object != null) {
					theDao.delete(object);
					deletedCount++;
				}
			} catch (DaoException e) {
				LOGGER.error("simplePurge", e);
			}
		}
		return deletedCount;
	}

	private final List<WorkInstruction> findByDetailPersistentIdList(List<UUID> inIdList) {
		if (inIdList != null && inIdList.isEmpty()) {
			return Collections.<WorkInstruction> emptyList(); //empty WHERE X IN () causes syntax issue in postgres
		} else {
			Criteria criteria = WorkInstruction.staticGetDao().createCriteria();
			criteria.add(Restrictions.in("orderDetail.persistentId", inIdList)); // empty .in() guard present
			@SuppressWarnings("unchecked")
			List<WorkInstruction> wiResultsList = (List<WorkInstruction>) criteria.list();
			return wiResultsList;
		}
	}

	public int purgeSomeReplenishDetails(List<UUID> detailUuids) {
		final int MAX_DETAIL_PURGE = 500;
		int wantToPurge = detailUuids.size();
		int willPurge = Math.min(wantToPurge, MAX_DETAIL_PURGE);
		if (wantToPurge > MAX_DETAIL_PURGE) {
			LOGGER.error("Limiting replenishDetail delete batch size to {}. Called for {}.", MAX_DETAIL_PURGE, wantToPurge);
		}
		List<UUID> uuidsToPurge = detailUuids.subList(0, willPurge);
		LOGGER.debug("Phase 1 of Replenish detail purge: retrieve details and wis to purge.");
		List<OrderDetail> details = OrderDetail.staticGetDao().findByPersistentIdList(uuidsToPurge);
		List<WorkInstruction> wis = findByDetailPersistentIdList(uuidsToPurge);

		LOGGER.debug("Phase 2 of Replenish detail purge: delete the assembled work instructions, which delinks from details and che.");
		safelyDeleteWorkInstructionList(wis);

		LOGGER.debug("Phase 3 of Replenish detail purge: delete the details");
		int deletedDetailCount = safelyDeleteDetailsList(details);

		return deletedDetailCount;
	}

	/**
	 * Purge these orders, and related objects, all in the current transaction.
	 * This imposes a max at one time limit of 100. We expect small values per transaction in production
	 */
	public int purgeSomeOrders(List<UUID> orderUuids) {
		final int MAX_ORDER_PURGE = 100;
		int wantToPurge = orderUuids.size();
		int willPurge = Math.min(wantToPurge, MAX_ORDER_PURGE);
		if (wantToPurge > MAX_ORDER_PURGE) {
			LOGGER.error("Limiting order delete batch size to {}. Called for {}.", MAX_ORDER_PURGE, wantToPurge);
		}

		//  back to simple order.delete() 
		// Trying to speed up by not relying quite so much on the hibernate delete cascade.
		// Result: not much improvement in time. But much nicer logging about the process. (changed to debug logging now)
		LOGGER.debug("Phase 2 of order purge: assemble list of details for these orders");
		List<OrderDetail> details = OrderDetail.staticGetDao().findByParentPersistentIdList(orderUuids);

		LOGGER.debug("Phase 3 of order purge: assemble work instructions from the details");
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.in("orderDetail", details));
		List<WorkInstruction> wis = WorkInstruction.staticGetDao().findByFilter(filterParams);
		
		LOGGER.debug("Phase 4 of order purge: delete the assembled work instructions, which delinks from details and che.");
		safelyDeleteWorkInstructionList(wis);

		LOGGER.debug("Phase 5 of order purge: delete the details");
		safelyDeleteDetailsList(details);

		LOGGER.debug("Phase 6 of order purge: delete the orders which delinks from container");

		int deletedCount = 0;
		for (UUID orderUuid : orderUuids) {
			// just protection against bad call
			if (deletedCount > willPurge)
				break;
			try {
				OrderHeader order = OrderHeader.staticGetDao().findByPersistentId(orderUuid);
				if (order != null) {
					order.delete();
					deletedCount++;
				}
			} catch (DaoException e) {
				LOGGER.error("purgeOrders", e);
			}
		}
		return deletedCount;
	}

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

	private int safelyDeleteDetailsList(List<OrderDetail> detailsList) {
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
		return deletedCount;
	}

	private int floorDays(int daysOldToCount) {
		return Math.max(daysOldToCount, 1);
	}

	/**
	 * This function unrelated to data purge. Used in a test function
	 * Get up to maxNeeded orders that are active and not complete
	 */
	public List<OrderHeader> getSomeUncompletedOrders(int maxNeeded) {
		UUID facilityUUID = getFacility().getPersistentId();
		Criteria orderCrit = OrderHeader.staticGetDao().createCriteria();
		orderCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		orderCrit.add(Restrictions.ne("status", OrderStatusEnum.COMPLETE));
		orderCrit.add(Restrictions.eq("active", true));
		orderCrit.setMaxResults(maxNeeded);
		return OrderHeader.staticGetDao().findByCriteriaQuery(orderCrit);
	}

	/**
	 * This function is used for the PickActivityHealthCheck. Query is somewhat similar to the purge queries.
	 */
	public FacilityPickParameters getFacilityPickParameters() {
		FacilityPickParameters params = new FacilityPickParameters();

		Timestamp dayOldTime = getDaysOldTimeStamp(1);
		Timestamp hourOldTime = getHoursOldTimeStamp(1);
		UUID facilityUUID = getFacility().getPersistentId();

		Criteria wiDayCrit = WorkInstruction.staticGetDao().createCriteria();
		wiDayCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		// Actual, with a complete time, should exclude housekeeping. Includes picks, putwall puts, and other things.
		wiDayCrit.add(Restrictions.eq("type", WorkInstructionTypeEnum.ACTUAL));
		wiDayCrit.add(Restrictions.gt("completed", dayOldTime));
		int completedThisDay = WorkInstruction.staticGetDao().countByCriteriaQuery(wiDayCrit);
		params.picksLastTwentyFourHours = completedThisDay;

		Criteria wiHourCrit = WorkInstruction.staticGetDao().createCriteria();
		wiHourCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		// Actual, with a complete time, should exclude housekeeping. Includes picks, putwall puts, and other things.
		wiHourCrit.add(Restrictions.eq("type", WorkInstructionTypeEnum.ACTUAL));
		wiHourCrit.add(Restrictions.gt("completed", hourOldTime));
		int completedThisHour = WorkInstruction.staticGetDao().countByCriteriaQuery(wiHourCrit);
		params.picksLastOneHour = completedThisHour;

		return params;
	}
}