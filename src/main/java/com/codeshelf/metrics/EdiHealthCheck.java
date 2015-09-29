package com.codeshelf.metrics;

import java.util.List;

import com.codeshelf.edi.EdiExportService;
import com.codeshelf.edi.EdiImportService;
import com.codeshelf.edi.FacilityEdiExporter;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;

public class EdiHealthCheck extends CodeshelfHealthCheck {
	final static int EDI_SERVICE_CYCLE_TIMEOUT_SECONDS = 60*5; // timeout if EDI takes longer than 5 mins
	
	private EdiImportService ediService;
	private EdiExportService ediExporterProvider;
	
	
	public EdiHealthCheck(EdiImportService ediService, EdiExportService ediExporterProvider) {
		super("EDI");
		this.ediService = ediService;
		this.ediExporterProvider = ediExporterProvider;
	}

	@Override
	protected Result check() throws Exception {
		if(!ediService.isRunning()) {
			return Result.unhealthy("EDI not running");
		} //else
		String err = ediService.getErrorStatus();
		if(err != null) {
			return Result.unhealthy(err);
		} //else
		long lastSuccessTime = ediService.getLastSuccessTime();
		long secondsSinceLastSuccess = (System.currentTimeMillis() - lastSuccessTime)/1000; 
		if(secondsSinceLastSuccess > EDI_SERVICE_CYCLE_TIMEOUT_SECONDS) {
			return Result.unhealthy(String.format("%d secs since last success", secondsSinceLastSuccess));
		} //else
		Result badSftpResult = testEDIServices();
		if (badSftpResult != null) {
			return badSftpResult;
		} //else
		return Result.healthy(OK);
	}

	private Result testEDIServices(){
		StringBuilder errors = new StringBuilder();
		for (Tenant tenant : TenantManagerService.getInstance().getTenants()) {
			try{
				CodeshelfSecurityManager.setContext(CodeshelfSecurityManager.getUserContextSYSTEM(), tenant);
				TenantPersistenceService.getInstance().beginTransaction();
				List<Facility> facilities = Facility.staticGetDao().getAll();
				for (Facility facility : facilities) {
					FacilityEdiExporter exporter = ediExporterProvider.getEdiExporter(facility);
					if (exporter == null) {
						continue;
					}
					long lastSuccessTime = exporter.getLastSuccessTime();
					long secondsSinceLastSuccess = (System.currentTimeMillis() - lastSuccessTime)/1000; 
					if(secondsSinceLastSuccess > EDI_SERVICE_CYCLE_TIMEOUT_SECONDS) {
						boolean goodConnection = exporter.testConnection();
						if (!goodConnection) {
							errors.append(String.format("Bad SFTP exporter %s, tenant: %s, facility: %s. ", exporter.getDomainId(), tenant.getTenantIdentifier(), facility.getDomainId()));
						}
					}
				}
				/*
				List<EdiServiceABC> ediServices = EdiServiceABC.staticGetDao().getAll();
				for (EdiServiceABC ediSerivce : ediServices) {
					if (ediSerivce.getActive()){
						if (ediSerivce.getServiceState() != EdiServiceStateEnum.LINKED) {
							errors.append(String.format("Active EDI service %s not linked, tenant: %s, facility: %s. ", ediSerivce.getDomainId(), tenant.getTenantIdentifier(), ediSerivce.getFacility().getDomainId()));
						}
					}
				}
				*/
				TenantPersistenceService.getInstance().commitTransaction();
			} catch (Exception e) {
				TenantPersistenceService.getInstance().rollbackTransaction();
				e.printStackTrace();
			} finally {
				CodeshelfSecurityManager.removeContext();
			}
		}
		if (errors.length() == 0) {
			return null;
		}
		return Result.unhealthy(errors.toString());
	}
}
