package com.codeshelf.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.EdiFileWriteException;
import com.codeshelf.model.domain.DropboxGateway;
import com.codeshelf.model.domain.EdiGateway;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.SftpGateway;
import com.codeshelf.model.domain.SftpOrderGateway;
import com.codeshelf.model.domain.SftpWiGateway;
import com.codeshelf.service.ExtensionPointEngine;

public class PurgeProcessorEdiFiles implements BatchProcessor {
	private static final Logger	LOGGER							= LoggerFactory.getLogger(PurgeProcessorEdiFiles.class);
	private static final int 	BATCH_SIZE						= 50;
	
	private Facility facility;
	private DataPurgeParameters	purgeParams;
	private Date purgeThreshold;
	private EdiFilesPurgePhase purgePhase = EdiFilesPurgePhase.PurgeSftpOrders;
	private List<String> sftpOrdersToPurge = new ArrayList<>();
	private List<String> sftpWisToPurge = new ArrayList<>();
	private List<String> dropboxOrdersToPurge = new ArrayList<>();
	private SftpOrderGateway sftpOrderGateway;
	private SftpWiGateway sftpWiGateway;
	private DropboxGateway dropboxOrderGateway;
	private int filesDeleted = 0;
	
	enum EdiFilesPurgePhase {RetrieveFiles, PurgeSftpOrders, PurgeSftpWIs, PurgeDropboxOrders, PurgeDone};

	public PurgeProcessorEdiFiles(Facility inFacility) {
		facility = inFacility;
		purgePhase = EdiFilesPurgePhase.RetrieveFiles;
	}
	
	@Override
	public int doSetup() throws Exception {
		LOGGER.info("Starting EDI Files Purge");
		//Determine age threshold for deleting files 
		purgeParams = ExtensionPointEngine.getInstance(facility).getDataPurgeParameters();
		int daysOldThreshold = purgeParams.getPurgeAfterDaysValue();
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, - daysOldThreshold);
		purgeThreshold = cal.getTime();
		return 0;
	}

	@Override
	public int doBatch(int batchCount) throws Exception {
		LOGGER.info("EDI Files Purge - do Batch " + purgePhase);
		facility = facility.reload();
		if (purgePhase == EdiFilesPurgePhase.RetrieveFiles){
			retrieveFiles();
			purgePhase = EdiFilesPurgePhase.PurgeSftpOrders;
		} else if (purgePhase == EdiFilesPurgePhase.PurgeSftpOrders){
			if (sftpOrdersToPurge.isEmpty()) {
				purgePhase = EdiFilesPurgePhase.PurgeSftpWIs;
			} else {
				filesDeleted += deleteSftpFiles(sftpOrderGateway, sftpOrdersToPurge);
			}
		} else if (purgePhase == EdiFilesPurgePhase.PurgeSftpWIs) {
			if (sftpWisToPurge.isEmpty()) {
				purgePhase = EdiFilesPurgePhase.PurgeDropboxOrders;
			} else {
				filesDeleted += deleteSftpFiles(sftpWiGateway, sftpWisToPurge);
			}
		} else if (purgePhase == EdiFilesPurgePhase.PurgeDropboxOrders) {
			if (dropboxOrdersToPurge.isEmpty()) {
				purgePhase = EdiFilesPurgePhase.PurgeDone;
			} else {
				filesDeleted += dropboxOrderGateway.deleteFileBatch(dropboxOrdersToPurge, BATCH_SIZE);
			}
		}
		return filesDeleted;
	}

	@Override
	public void doTeardown() {
	}
	
	@Override
	public boolean isDone() {
		return purgePhase == EdiFilesPurgePhase.PurgeDone;
	}
	
	private void retrieveFiles(){
		List<EdiGateway> gateways = facility.getEdiGateways();
		for (EdiGateway gateway : gateways) {
			if (gateway.isActive() && gateway.isLinked()) {
				if (gateway instanceof SftpOrderGateway){
					sftpOrderGateway = (SftpOrderGateway) gateway;
					sftpOrdersToPurge = sftpOrderGateway.retrieveOldProcessedFilesList(purgeThreshold);
				} else if (gateway instanceof SftpWiGateway) {
					sftpWiGateway = (SftpWiGateway) gateway;
					sftpWisToPurge = sftpWiGateway.retrieveOldProcessedFilesList(purgeThreshold);
				} else if (gateway instanceof DropboxGateway) {
					dropboxOrderGateway = (DropboxGateway) gateway;
					dropboxOrdersToPurge = dropboxOrderGateway.retrieveOldProcessedFilesList(purgeThreshold);
				}
			}
		}
	}
	
	private int deleteSftpFiles(SftpGateway gateway, List<String> filesToDelete){
		int deleted = 0;
		gateway = (SftpGateway)EdiGateway.staticGetDao().reload(gateway);
		int filesLeftToDelete = (BATCH_SIZE < filesToDelete.size()) ? BATCH_SIZE : filesToDelete.size();
		for (; filesLeftToDelete > 0; filesLeftToDelete--){
			String filename = filesToDelete.remove(0);
			try {
				gateway.delete(filename);
				deleted++;
			} catch (EdiFileWriteException e) {
				LOGGER.warn("Unable to delete sftp file " + filename, e);
			}
		}
		return deleted;
	}	
}
