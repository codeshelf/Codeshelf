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
import com.codeshelf.service.ExtensionPointService;
import com.google.inject.Singleton;

@Singleton
public class EdiExportService extends AbstractCodeshelfIdleService {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(EdiExportService.class);

	private Map<UUID, FacilityEdiExporter> facilityEdiExporters = new HashMap<>();
		
	public FacilityEdiExporter getEdiExporter(Facility facility) throws Exception {
		//look up the export service for the facility
		// > WI > WIRecord/OrderRecord/CheRecord > WIMessage > Queue > Transport Tasks
		//retry
		//capacity
		//messageFormatter
		//return facility.getEdiExportService();
		
		synchronized(facilityEdiExporters) {
			if (!facilityEdiExporters.containsKey(facility.getPersistentId())) {
				updateEdiExporter(facility);
			}
			return facilityEdiExporters.get(facility.getPersistentId());
		}
	}
	
	public void extensionPointsUpdated(Facility facility) {
		try {
			updateEdiExporter(facility);
		} catch (ScriptException e) {
			LOGGER.error("Unable to update EdiExporter for facility {}", facility, e);
		} catch (TimeoutException e) {
			LOGGER.error("Unable to update EdiExporter for facility {}", facility, e);
		}
	}
	
	public void ediExportServiceUpdated(Facility facility) {
		try {
			updateEdiExporter(facility);
		} catch (ScriptException e) {
			LOGGER.error("Unable to update EdiExporter for facility {}", facility, e);
		} catch (TimeoutException e) {
			LOGGER.error("Unable to update EdiExporter for facility {}", facility, e);
		}
	}

	private void updateEdiExporter(Facility facility) throws ScriptException, TimeoutException {
		IEdiExportGateway exportTransport = facility.getEdiExportTransport();
		if (exportTransport != null) {
			ExtensionPointService extensionPointService = ExtensionPointService.createInstance(facility);
			WiBeanStringifier stringifier = new WiBeanStringifier(extensionPointService);
			synchronized(facilityEdiExporters) {
				FacilityEdiExporter exporter = facilityEdiExporters.get(facility.getPersistentId());
				if (exporter == null) {
					EdiExportAccumulator accumulator = new EdiExportAccumulator();
					exporter = new FacilityAccumulatingExporter(facility, accumulator, stringifier, exportTransport);
					exporter.startAsync().awaitRunning(5, TimeUnit.SECONDS);
					facilityEdiExporters.put(facility.getPersistentId(), exporter);
				} else {
					LOGGER.info("Updating edi exporter for facility {}", facility);
					exporter.setEdiExportTransport(exportTransport);
					exporter.setStringifier(stringifier);
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
			for (FacilityEdiExporter exporter : facilityEdiExporters.values()) {
				exporter.stopAsync();
			};
			
			for (FacilityEdiExporter exporter : facilityEdiExporters.values()) {
				try {
					exporter.awaitTerminated(2, TimeUnit.SECONDS);
				}catch(Exception e) {
					LOGGER.error("Exporting service {} did not shtudown within 2 seconds", exporter, e);
				}
			};	
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
