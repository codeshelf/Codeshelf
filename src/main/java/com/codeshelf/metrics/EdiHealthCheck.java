package com.codeshelf.metrics;

import java.util.List;

import com.codeshelf.edi.EdiExportService;
import com.codeshelf.edi.EdiImportService;
import com.codeshelf.edi.IEdiExportGateway;
import com.codeshelf.edi.IEdiGateway;
import com.codeshelf.edi.IEdiImportGateway;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.domain.EdiGateway;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;

public class EdiHealthCheck extends CodeshelfHealthCheck {
	final static int EDI_SERVICE_CYCLE_TIMEOUT_SECONDS = 60*5; // timeout if EDI takes longer than 5 mins
	
	private EdiImportService ediService;
	@SuppressWarnings("unused")
	private EdiExportService ediExportService;
	
	
	public EdiHealthCheck(EdiImportService ediService, EdiExportService ediExportService) {
		super("EDI");
		this.ediService = ediService;
		this.ediExportService = ediExportService;
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
				
				List<EdiGateway> ediGateways = EdiGateway.staticGetDao().getAll();
				for (IEdiGateway ediGateway : ediGateways){
					//Skip inactive EDI gateways
					if (!ediGateway.isActive()){
						continue;
					}
					if(!ediGateway.isLinked()) {
						//Check if EDI gateway is linked
						errors.append(String.format("Active EDI service %s not linked, tenant: %s, facility: %s. ", ediGateway.getDomainId(), tenant.getTenantIdentifier(), ediGateway.getFacility().getDomainId()));
					} else if (ediGateway instanceof IEdiImportGateway) {
						//Check if active EDI importer has checked for new files recently
						long timeSinceLastImportCheck = System.currentTimeMillis() - ediGateway.getLastSuccessTime().getTime();
						if (timeSinceLastImportCheck > EDI_SERVICE_CYCLE_TIMEOUT_SECONDS * 1000){
							errors.append(String.format("Active EDI import service hadn't had a successful check since %s, tenant: %s, facility: %s. ", ediGateway.getLastSuccessTime(), tenant.getTenantIdentifier(), ediGateway.getFacility().getDomainId()));
						}
					} else if (ediGateway instanceof IEdiExportGateway) {
						//Check if EDI exporter can connect to destination
						if (!ediGateway.testConnection()){
							errors.append(String.format("Active EDI service %s connection error, tenant: %s, facility: %s. ", ediGateway.getDomainId(), tenant.getTenantIdentifier(), ediGateway.getFacility().getDomainId()));
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
