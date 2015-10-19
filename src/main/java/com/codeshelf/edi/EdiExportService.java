package com.codeshelf.edi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.script.ScriptException;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.domain.ExportMessage;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;
import com.codeshelf.service.AbstractCodeshelfIdleService;
import com.codeshelf.service.ExtensionPointEngine;
import com.google.inject.Singleton;

@Singleton
public class EdiExportService extends AbstractCodeshelfIdleService {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(EdiExportService.class);

	private Map<UUID, IFacilityEdiExporter> facilityEdiExporters = new HashMap<>();
		
	public IFacilityEdiExporter getEdiExporter(Facility facility) throws Exception {
		//See if the current export gateway is enabled or disabled by user
		IEdiExportGateway gateway = facility.getEdiExportGateway();
		if (gateway == null || !gateway.isActive()){
			return null;
		}
		synchronized(facilityEdiExporters) {
			if (!facilityEdiExporters.containsKey(facility.getPersistentId())) {
				updateEdiExporter(facility);
			}
			return facilityEdiExporters.get(facility.getPersistentId());
		}
	}
		
	public void updateEdiExporterSafe(Facility facility) {
		try {
			updateEdiExporter(facility);
		} catch (ScriptException e) {
			LOGGER.error("Unable to update EdiExporter for facility {}", facility, e);
		} catch (TimeoutException e) {
			LOGGER.error("Unable to update EdiExporter for facility {}", facility, e);
		}
	}

	public void updateEdiExporter(Facility facility) throws ScriptException, TimeoutException {
		IEdiExportGateway exportGateway = facility.getEdiExportGateway();
		if (exportGateway != null) {
			ExtensionPointEngine extensionPointEngine = ExtensionPointEngine.getInstance(facility);
			WiBeanStringifier stringifier = new WiBeanStringifier(extensionPointEngine);
			synchronized(facilityEdiExporters) {
				if (exportGateway.isActive()) {
					IFacilityEdiExporter exporter = facilityEdiExporters.get(facility.getPersistentId());
					if (exporter == null) {
						exporter = new FacilityAccumulatingExporter(facility);
						exporter.startAsync().awaitRunning(5, TimeUnit.SECONDS);
						facilityEdiExporters.put(facility.getPersistentId(), exporter);
					}
					LOGGER.info("Updating edi exporter for facility {}", facility);
					exporter.setEdiExportGateway(exportGateway);
					exporter.setStringifier(stringifier);
				} else {
					stopEdiExporter(facility);
				}
			}
		}
	}

	@Override
	protected void startUp() throws Exception {
		for (Tenant tenant : TenantManagerService.getInstance().getTenants()) {
			doExportEdiForTenant(tenant);
		}
	}

	@Override
	protected void shutDown() throws Exception {
		synchronized(facilityEdiExporters) {
			for (IFacilityEdiExporter exporter : facilityEdiExporters.values()) {
				exporter.stopAsync();
			};
			
			for (IFacilityEdiExporter exporter : facilityEdiExporters.values()) {
				try {
					exporter.awaitTerminated(2, TimeUnit.SECONDS);
				}catch(Exception e) {
					LOGGER.error("Exporting service {} did not shtudown within 2 seconds", exporter, e);
				}
			};	
		}		
	}
	
	public void stopEdiExporter(Facility facility){
		synchronized(facilityEdiExporters) {
			IFacilityEdiExporter exporter = facilityEdiExporters.remove(facility.getPersistentId());
			if (exporter != null) {
				exporter.waitUntillQueueIsEmpty(60000);
				exporter.stopAsync();
				try {
					exporter.awaitTerminated(2, TimeUnit.SECONDS);
				}catch(Exception e) {
					LOGGER.error("Exporting service {} did not shtudown within 2 seconds", exporter, e);
				}
			}
		}
	}
	
	private boolean doExportEdiForTenant(Tenant tenant) {
		long startTime = System.currentTimeMillis();
		try {
			UserContext systemUser = CodeshelfSecurityManager.getUserContextSYSTEM();
			CodeshelfSecurityManager.setContext(systemUser, tenant);
			TenantPersistenceService.getInstance().beginTransaction();

			LOGGER.info("Check if there are any unsent EDI messages in for tenant {}", tenant.getName());
			List<Criterion> filterParams = new ArrayList<Criterion>();
			filterParams.add(Restrictions.eq("active", true));
			List<ExportMessage> exportMessages = ExportMessage.staticGetDao().findByFilter(filterParams);
			HashSet<Facility> facilitiesToInit = new HashSet<>();
			for (ExportMessage message : exportMessages) {
				facilitiesToInit.add(message.getFacility());
			}
			
			LOGGER.info("Located {} facilities in tennant {} with unsent EDI messages. Initialting send threads for them", facilitiesToInit.size(), tenant.getName());
			for (Facility facility : facilitiesToInit) {
				updateEdiExporter(facility);
			}
			TenantPersistenceService.getInstance().commitTransaction();
			return true;
		} catch (Exception e) {
			LOGGER.error("Unable to process export edi for tenant " + tenant.getId(), e);
			TenantPersistenceService.getInstance().rollbackTransaction();
			return false;
		} finally {
			long endTime = System.currentTimeMillis();
			LOGGER.info("Checked for unsent export messages for tenant {} in {}s", tenant.getName(), (endTime - startTime) / 1000);
			CodeshelfSecurityManager.removeContext();
		}
	}

}
