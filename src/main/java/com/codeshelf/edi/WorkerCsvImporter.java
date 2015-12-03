/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvImporter.java,v 1.30 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.io.Reader;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.event.EventProducer;
import com.codeshelf.event.EventSeverity;
import com.codeshelf.event.EventTag;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.validation.InputValidationException;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public class WorkerCsvImporter extends CsvImporter<WorkerCsvBean> implements ICsvWorkerImporter {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(WorkerCsvImporter.class);

	@Inject
	public WorkerCsvImporter(final EventProducer inProducer) {
		super(inProducer);
	}

	// --------------------------------------------------------------------------
	/* 
	 *
	 */
	public final boolean importWorkersFromCsvStream(Reader inCsvReader, Facility inFacility, Timestamp inProcessTime) {
		boolean result = true;
		List<WorkerCsvBean> workerBeanList = toCsvBean(inCsvReader, WorkerCsvBean.class);

		if (workerBeanList.size() > 0) {

			LOGGER.debug("Begin location alias map import.");

			// Iterate over the location alias import beans.
			for (WorkerCsvBean workerBean : workerBeanList) {
				try {
					Worker worker = workerCsvBeanImport(workerBean, inFacility, inProcessTime);
					if (worker != null) {
						produceRecordSuccessEvent(workerBean);
					} else {
						result &= false;
					}
				} catch (InputValidationException e) {
					result &= false;
					produceRecordViolationEvent(EventSeverity.WARN, e, workerBean);
					LOGGER.warn("Unable to process record: " + workerBean, e);
				} catch (Exception e) {
					result &= false;
					produceRecordViolationEvent(EventSeverity.ERROR, e, workerBean);
					LOGGER.error("Unable to process record: " + workerBean, e);
				}
			}

			archiveCheckWorkers(inFacility, inProcessTime);

			LOGGER.debug("End location alias import.");
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  deactivate any previous workers not referenced in this file
	 */
	private void archiveCheckWorkers(final Facility inFacility, final Timestamp inProcessTime) {
		LOGGER.debug("Archive unreferenced workers data");
		int archiveCount = 0;
		
		// Our mission is to inactivate any active workers at this facility that were not represented in this file
		// But leave other facility workers alone.
		UUID facilityUUID = inFacility.getPersistentId();
		Criteria criteria = Worker.staticGetDao().createCriteria();
		criteria.add(Restrictions.eq("parent.persistentId", facilityUUID));
		criteria.add(Restrictions.eq("active", true));

		List<Worker> workers = Worker.staticGetDao().findByCriteriaQuery(criteria);
		for (Worker worker : workers) {
			if (!inProcessTime.equals(worker.getUpdated())) {
				archiveCount++;
				try {
					worker.setActive(false);
					Worker.staticGetDao().store(worker);
				}
				catch (DaoException e) {
					LOGGER.error("archiveCheckWorkers", e);
				}
			}
		}

		if (archiveCount > 0) {
			LOGGER.info("Archived {} workers for this facility that were not in this workers import file", archiveCount);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 * @throws ViolationException 
	 */
	private Worker workerCsvBeanImport(final WorkerCsvBean inCsvBean, final Facility inFacility, final Timestamp inEdiProcessTime) throws InputValidationException,
		DaoException {

		LOGGER.info(inCsvBean.toString());
		String errorMsg = inCsvBean.validateBean();
		if (errorMsg != null) {
			produceRecordViolationEvent(inCsvBean, errorMsg);
			return null;
		}
		boolean isNewWorker = true;

		// Get or create the item at the specified location.
		String badgeId = inCsvBean.getBadgeId();
		Worker result = Worker.findTenantWorker(badgeId);

		if (result == null) {
			// create a new worker
			result = new Worker();
			result.setBadgeId(badgeId); // sets domainId
			isNewWorker = true;
		}

		Facility newFacility = inFacility;
		String newFirst = inCsvBean.getFirstName();
		String newLast = inCsvBean.getLastName();
		String newHr = inCsvBean.getHumanResourcesId();
		String newGroup = inCsvBean.getWorkGroupName();

		boolean changed = false;
		if (!isNewWorker) {
			changed = changed || !newFirst.equals(result.getFirstName());
			changed = changed || !newLast.equals(result.getLastName());
			changed = changed || !newHr.equals(result.getHrId());
			changed = changed || !newGroup.equals(result.getGroupName());

			if (!newFacility.equals(inFacility)) {
				changed = true; // change of facility. Fairly major?
				LOGGER.info("Worker {} changing facility", badgeId);
			}

			if (!result.getActive()) {
				changed = true; // was inactive. Now active. Also log-worthy
				LOGGER.info("Worker {} was inactive. Becoming active.", badgeId);
			}
		}

		result.setParent(newFacility);
		result.setFirstName(newFirst);
		// If last name field is missing or blank, force the badgeId there
		if (newLast != null && !newLast.isEmpty())
			result.setLastName(newLast);
		else
			result.setLastName(badgeId);
		result.setGroupName(newGroup);
		result.setHrId(newHr);

		result.setActive(true);
		result.setUpdated(inEdiProcessTime);

		if (changed) {
			LOGGER.info("Worker updated from import. New state: {} ", result);
		}

		try {
			if (result.getBadgeField() == null || result.getDomainId() == null)
				LOGGER.error("storing bad worker. How?");
			Worker.staticGetDao().store(result);
		} catch (DaoException e) {
			LOGGER.warn("Worker bean import", e);
		}
		return result;
	}

	@Override
	protected Set<EventTag> getEventTagsForImporter() {
		return EnumSet.of(EventTag.IMPORT, EventTag.WORKER);
	}

}
