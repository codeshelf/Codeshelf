package com.codeshelf.edi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.service.AbstractCodeshelfIdleService;
import com.codeshelf.service.ExtensionPointService;
import com.google.inject.Singleton;

@Singleton
public class EdiExporterProvider extends AbstractCodeshelfIdleService {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(EdiExporterProvider.class);

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
		EdiExportTransport exportTransport = facility.getEdiExportTransport();
		if (exportTransport != null) {
			ExtensionPointService extensionPointService = ExtensionPointService.createInstance(facility);
			WiBeanStringifier stringifier = new WiBeanStringifier(extensionPointService);
			synchronized(facilityEdiExporters) {
				FacilityEdiExporter exporter = facilityEdiExporters.get(facility.getPersistentId());
				if (exporter == null) {
					EdiExportAccumulator accumulator = new EdiExportAccumulator();
					exporter = new FacilityAccumulatingExporter(accumulator, stringifier, exportTransport);
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
		// TODO Auto-generated method stub
		
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
}
