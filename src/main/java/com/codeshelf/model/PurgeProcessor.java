package com.codeshelf.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.BatchProcessor;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.service.ExtensionPointEngine;

public class PurgeProcessor implements BatchProcessor {
	private static final Logger	LOGGER					= LoggerFactory.getLogger(PurgeProcessor.class);

	Facility					facility;
	DataPurgeParameters			purgeParams				= null;
	List<UUID>					ordersUuidsToPurge		= null;
	List<UUID>					containerUuidsToPurge	= null;
	List<UUID>					wiUuidsToPurge			= null;
	List<UUID>					workerEventUuidsToPurge	= null;

	enum PurgePhase {
		PurgePhaseInit,
		PurgePhaseSetup,
		PurgePhaseOrders,
		PurgePhaseContainers,
		PurgePhaseWis,
		PurgePhaseWorkerEvents,
		PurgePhaseDone
	}

	@Setter
	@Getter
	PurgePhase	purgePhase;

	@Setter
	@Getter
	int			ordersToPurge;
	@Setter
	@Getter
	int			workerEventsToPurge;
	@Getter
	int			ordersPurged		= 0;
	@Getter
	int			wisPurged			= 0;
	@Getter
	int			cntrsPurged			= 0;
	@Getter
	int			workerEventsPurged	= 0;

	public PurgeProcessor(Facility inFacility) {
		facility = inFacility;
		setPurgePhase(PurgePhase.PurgePhaseInit);
	}

	/**
	 *  This needs to return a count of how many "things" it is going to do.
	 *  And assemble and remember what doBatch is going to do. This is called in an appropriate transaction
	 *  Unfortunately, we cannot really know how many containers or wis will be purged until after the order purge is done.
	 *  This returns the sum of orders to purge, and workerEvents to purge
	 */
	@Override
	public int doSetup() throws Exception {
		setPurgePhase(PurgePhase.PurgePhaseSetup);
		facility = facility.reload();
		// get our purge parameters
		ExtensionPointEngine ss = ExtensionPointEngine.getInstance(facility);
		purgeParams = ss.getDataPurgeParameters();

		LOGGER.info("Starting data purge with these parameters: {}", purgeParams);

		buildWorkerEventList();
		buildOrdersList(); //must be called here instead of in doBatch() so we can return something about the size of the job

		setPurgePhase(PurgePhase.PurgePhaseOrders);
		return (getOrdersToPurge() + getWorkerEventsToPurge());
	}

	private void buildWiList() {
		if (wiUuidsToPurge != null)
			LOGGER.error("buildWiList should be called only once");
		DomainObjectManager doMananager = new DomainObjectManager(facility);
		wiUuidsToPurge = doMananager.getWorkInstructionUuidsToPurge(purgeParams.getPurgeAfterDaysValue());
		LOGGER.info("Starting work instruction purge. {} work instructions.", wiUuidsToPurge.size());
	}

	private int purgeWiBatch() {
		if (wiUuidsToPurge.isEmpty()) {
			LOGGER.error("unexpected state for purgeWiBatch");
			return 0;
		}
		List<UUID> wiBatch = subUuidListAndRemoveFromInList(wiUuidsToPurge, purgeParams.getWorkInstructionBatchValue());
		DomainObjectManager doMananager = new DomainObjectManager(facility);
		return doMananager.purgeSomeWis(wiBatch);
	}

	private void buildOrdersList() {
		if (ordersUuidsToPurge != null)
			LOGGER.error("buildOrdersList should be called only once");
		DomainObjectManager doMananager = new DomainObjectManager(facility);
		ordersUuidsToPurge = doMananager.getOrderUuidsToPurge(purgeParams.getPurgeAfterDaysValue());
		setOrdersToPurge(ordersUuidsToPurge.size()); // record the starting size of the job
		LOGGER.info("Starting orders and associated objects purge. {} orders", ordersUuidsToPurge.size());
	}
	
	private int purgeOrderBatch() {
		if (ordersUuidsToPurge.isEmpty()) {
			LOGGER.error("unexpected state for purgeOrderBatch");
			return 0;
		}
		List<UUID> orderBatch = subUuidListAndRemoveFromInList(ordersUuidsToPurge, purgeParams.getOrderBatchValue());
		DomainObjectManager doMananager = new DomainObjectManager(facility);
		return doMananager.purgeSomeOrders(orderBatch);
	}

	private void buildWorkerEventList() {
		// This could be combined into buildOrdersList with the current structure.
		// This is called in doSetup. We just keep the list around
		if (workerEventUuidsToPurge != null)
			LOGGER.error("buildWorkerEventList should be called only once");
		DomainObjectManager doMananager = new DomainObjectManager(facility);
		workerEventUuidsToPurge = doMananager.getWorkerEventUuidsToPurge(purgeParams.getPurgeAfterDaysValue());
		setWorkerEventsToPurge(workerEventUuidsToPurge.size()); // record this component of the starting size of the job
	}

	private int purgeWorkerEventBatch() {
		if (workerEventUuidsToPurge.isEmpty()) {
			LOGGER.error("unexpected state for purgeWorkerEventBatch");
			return 0;
		}
		List<UUID> eventBatch = subUuidListAndRemoveFromInList(workerEventUuidsToPurge, purgeParams.getWorkerEventBatchValue());
		DomainObjectManager doMananager = new DomainObjectManager(facility);
		return doMananager.purgeSomeCntrs(eventBatch);
	}

	private void buildCntrList() {
		if (containerUuidsToPurge != null)
			LOGGER.error("buildCntrList should be called only once");
		DomainObjectManager doMananager = new DomainObjectManager(facility);
		containerUuidsToPurge = doMananager.getCntrUuidsToPurge(purgeParams.getPurgeAfterDaysValue());
		LOGGER.info("Starting containers purge. {} containers", containerUuidsToPurge.size());
	}

	private int purgeCntrBatch() {
		if (containerUuidsToPurge.isEmpty()) {
			LOGGER.error("unexpected state for purgeCntrBatch");
			return 0;
		}
		List<UUID> cntrBatch = subUuidListAndRemoveFromInList(containerUuidsToPurge, purgeParams.getContainerBatchValue());
		DomainObjectManager doMananager = new DomainObjectManager(facility);
		return doMananager.purgeSomeCntrs(cntrBatch);
	}

	/**
	 *  Pass in a list. We want to get back the sublist, and remove items from the passed in list.
	 *  (Would be ok also to replace the passed in list if we returned a two parameter class.)
	 */
	private List<UUID> subUuidListAndRemoveFromInList(List<UUID> inList, int subCount) {
		int countNeeded = Math.min(subCount, inList.size());

		List<UUID> returnList = new ArrayList<UUID>(inList.subList(0, countNeeded));
		inList.removeAll(returnList);
		return returnList;
	}

	@Override
	public int doBatch(int batchCount) throws Exception {
		PurgePhase currentPhase = getPurgePhase();
		// in almost every case, need to reload facility, so do it centrally
		facility = facility.reload();

		if (currentPhase == PurgePhase.PurgePhaseOrders) {
			// we want to know if we are done with orders. One quite reliable way to know is if the orders list size decreased.
			// If non-zero, but it did not decrease, lets log an error and call it done so we are not stuck forever.
			int startSize = ordersUuidsToPurge.size();
			if (startSize > 0)
				ordersPurged += purgeOrderBatch();
			int endSize = ordersUuidsToPurge.size();
			if (endSize == startSize || endSize == 0) {
				setPurgePhase(PurgePhase.PurgePhaseWis);
				LOGGER.info("Total: purged {} orders", ordersPurged);
				if (endSize > 0) {
					LOGGER.error("orders purge did not progress. Bailing out. Leaving {} orders that should have been purged", endSize);
				}
			} else if (batchCount % 10 == 0) {
				LOGGER.info("incremental total: purged {} orders", ordersPurged);
			}
		} else if (currentPhase == PurgePhase.PurgePhaseWis) {
			if (wiUuidsToPurge == null)
				buildWiList();
			int startSize = wiUuidsToPurge.size();
			if (startSize > 0)
				wisPurged += purgeWiBatch();
			int endSize = wiUuidsToPurge.size();
			if (endSize == startSize || endSize == 0) {
				LOGGER.info("Total: purged {} work instructions", wisPurged);
				setPurgePhase(PurgePhase.PurgePhaseContainers);
				if (endSize > 0) {
					LOGGER.error("work instructions purge did not progress. Bailing out. Leaving {} work instructions that should have been purged", endSize);
				}
			} else if (batchCount % 10 == 0) {
				LOGGER.info("incremental total: purged {} work instructions", wisPurged);
			}
		} else if (currentPhase == PurgePhase.PurgePhaseContainers) {
			if (containerUuidsToPurge == null)
				buildCntrList();
			int startSize = containerUuidsToPurge.size();
			if (startSize > 0)
				cntrsPurged += purgeCntrBatch();
			int endSize = containerUuidsToPurge.size();
			if (endSize == startSize || endSize == 0) {
				LOGGER.info("Total: purged {} containers", cntrsPurged);
				setPurgePhase(PurgePhase.PurgePhaseWorkerEvents);
				if (endSize > 0) {
					LOGGER.error("container purge did not progress. Bailing out. Leaving {} containers that should have been purged", endSize);
				}
			} else if (batchCount % 10 == 0) {
				LOGGER.info("incremental total: purged {} containers", cntrsPurged);
			}
		} else if (currentPhase == PurgePhase.PurgePhaseWorkerEvents) {
			if (workerEventUuidsToPurge == null) {
				LOGGER.error("Expected worker event list in PurgePhaseWorkerEvents.Bailing out.");
				setPurgePhase(PurgePhase.PurgePhaseDone);
			}			
			
			int startSize = workerEventUuidsToPurge.size();
			if (startSize > 0)
				workerEventsPurged += purgeWorkerEventBatch();
			int endSize = workerEventUuidsToPurge.size();
			if (endSize == startSize || endSize == 0) {
				setPurgePhase(PurgePhase.PurgePhaseDone);
				LOGGER.info("Total: purged {} worker events", workerEventsPurged);
				if (endSize > 0) {
					LOGGER.error("workerEvents purge did not progress. Bailing out. Leaving {} workerEvents that should have been purged", endSize);
				}
			} else if (batchCount % 10 == 0) {
				LOGGER.info("incremental total: purged {} workerEvents", workerEventsPurged);
			}

		} else {
			LOGGER.error("Unexpected phase in PurgeProcessor doBatch {}", currentPhase);
			setPurgePhase(PurgePhase.PurgePhaseDone);
		}

		// kind of weak, but order deletion is the way slow part. 
		// Once through with orders, we continuously report the same doneness for the rest of the purge.
		return ordersPurged;
	}

	@Override
	public void doTeardown() {

	}

	@Override
	public boolean isDone() {
		return getPurgePhase() == PurgePhase.PurgePhaseDone;

	}
}