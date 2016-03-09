package com.codeshelf.metrics;

import java.util.List;

import com.codeshelf.edi.EdiImportService;
import com.codeshelf.edi.IEdiExportGateway;
import com.codeshelf.edi.IEdiImportGateway;
import com.codeshelf.model.domain.EdiGateway;
import com.codeshelf.model.domain.Facility;
import com.google.inject.Inject;

public class EdiHealthCheck extends HealthCheckRefreshJob{
	final static int EDI_SERVICE_CYCLE_TIMEOUT_SECONDS = 60*5; // timeout if EDI takes longer than 5 mins
	
	private EdiImportService ediService;
	
	@Inject
	public EdiHealthCheck(EdiImportService ediService){
		this.ediService = ediService;
	}
	
	@Override
	public void check(Facility facility) throws Exception {
		try {
			if(!ediService.isRunning()) {
				saveResults(facility, false, "EDI not running");
				return;
			}
			String err = ediService.getErrorStatus();
			if(err != null) {
				saveResults(facility, false, err);
				return;
			}
			long lastSuccessTime = ediService.getLastSuccessTime();
			long secondsSinceLastSuccess = (System.currentTimeMillis() - lastSuccessTime)/1000;
			if(secondsSinceLastSuccess > EDI_SERVICE_CYCLE_TIMEOUT_SECONDS) {
				saveResults(facility, false, String.format("%d secs since last success", secondsSinceLastSuccess));
				return;
			}
			String badSftpResult = testEDIServices(facility);
			if (badSftpResult != null) {
				saveResults(facility, false, badSftpResult);
				return;
			}
			saveResults(facility, true, "Success");
		} catch (Exception e) {
			saveResults(facility, false, "Internal Server Error");
			throw e;
		}
	}

	private String testEDIServices(Facility facility) throws Exception{
		StringBuilder errors = new StringBuilder();
		List<EdiGateway> ediGateways = facility.getEdiGateways();
		for (EdiGateway ediGateway : ediGateways){
			//Skip inactive EDI gateways
			if (!ediGateway.isActive()){
				continue;
			}
			String header = String.format("Issue with active EDI service %s.%s: ", ediGateway.getFacility().getDomainId(), ediGateway.getDomainId());
			if(!ediGateway.isLinked()) {
				//Check if EDI gateway is linked
				errors.append(header + "not linked. ");
			} else if (ediGateway instanceof IEdiImportGateway) {
				//Check if active EDI importer has checked for new files recently
				long timeSinceLastImportCheck = System.currentTimeMillis() - ediGateway.getLastSuccessTime().getTime();
				if (timeSinceLastImportCheck > EDI_SERVICE_CYCLE_TIMEOUT_SECONDS * 1000){
					errors.append(header + "hadn't had a successful check since " + ediGateway.getLastSuccessTime() + ". ");
				}
			} else if (ediGateway instanceof IEdiExportGateway) {
				//Check if EDI exporter can connect to destination
				if (!ediGateway.testConnection()){
					errors.append(header + "connection error. ");
				}
			}
		}
		if (errors.length() == 0) {
			return null;
		}
		return errors.toString();
	}	
}
