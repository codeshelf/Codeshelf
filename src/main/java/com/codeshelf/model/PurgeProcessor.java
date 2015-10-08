package com.codeshelf.model;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.BatchProcessor;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.service.ExtensionPointService;

public class PurgeProcessor implements BatchProcessor {
	@SuppressWarnings("unused")
	private static final Logger	LOGGER				= LoggerFactory.getLogger(PurgeProcessor.class);

	Facility					facility;
	DataPurgeParameters			purgeParams			= null;
	List<UUID>					ordersToPurge		= null;
	List<UUID>					containersToPurge	= null;
	List<UUID>					wisToPurge			= null;

	enum PurgePhase {
		PurgePhaseInit,
		PurgePhaseSetup,
		PurgePhaseOrders,
		PurgePhaseContainers,
		PurgePhaseWis
	}

	@Setter
	@Getter
	PurgePhase	purgePhase;

	public PurgeProcessor(Facility inFacility) {
		facility = inFacility;
		setPurgePhase(PurgePhase.PurgePhaseInit);
	}

	/**
	 *  This needs to return a count of how many "things" it is going to do.
	 *  And assemble and remember what doBatch is going to do. This is called in an appropriate transaction
	 *  Unfortunately, we cannot really know how many containers or wis will be purged until after the order purge is done.
	 */
	@Override
	public int doSetup() throws Exception {
		setPurgePhase(PurgePhase.PurgePhaseSetup);
		facility = facility.reload();
		// get our purge parameters
		ExtensionPointService ss = ExtensionPointService.createInstance(facility);
		purgeParams = ss.getDataPurgeParameters();
		
		@SuppressWarnings("unused")
		DomainObjectManager doMananager = new DomainObjectManager(facility);
		// doMananager.getOrderUuidsToPurge(daysOldToCount, inCls, 1000);


		return 0;
	}

	@Override
	public int doBatch(int batchCount) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void doTeardown() {

	}

	@Override
	public boolean isDone() {
		// TODO Auto-generated method stub
		return false;
	}

}