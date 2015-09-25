package com.codeshelf.metrics;

import java.util.List;

import com.codeshelf.edi.EdiExporterProvider;
import com.codeshelf.edi.EdiProcessorService;
import com.codeshelf.edi.FacilityEdiExporter;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;

public class EdiHealthCheck extends CodeshelfHealthCheck {
	final static int EDI_SERVICE_CYCLE_TIMEOUT_SECONDS = 60*5; // timeout if EDI takes longer than 5 mins
	
	private EdiProcessorService ediService;
	private EdiExporterProvider ediExporterProvider;
	
	
	public EdiHealthCheck(EdiProcessorService ediService, EdiExporterProvider ediExporterProvider) {
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
		Result badSftpResult = testSFTPServices();
		if (badSftpResult != null) {
			return badSftpResult;
		} //else
		return Result.healthy(OK);
	}

	private Result testSFTPServices(){
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
