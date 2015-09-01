package com.codeshelf.edi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.service.ExtensionPointService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class EdiExporterProvider {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(EdiExporterProvider.class);

	private Map<UUID, EdiExportAccumulator> facilityEdiAccumulators = new HashMap<>();
	private Map<UUID, FacilityEdiExporter> facilityEdiExporters = new HashMap<>();
	private Map<UUID, ListeningExecutorService> facilityExecutorServices = new HashMap<>();
		
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
		}
	}
	
	public void ediExportServiceUpdated(Facility facility) {
		try {
			updateEdiExporter(facility);
		} catch (ScriptException e) {
			LOGGER.error("Unable to update EdiExporter for facility {}", facility, e);
		}
	}

	private void updateEdiExporter(Facility facility) throws ScriptException {
		EdiExportTransport exportService = facility.getEdiExportTransport();
		if (exportService != null) {
			ExtensionPointService extensionPointService = ExtensionPointService.createInstance(facility);
			WiBeanStringifier stringifier = new WiBeanStringifier(extensionPointService);
			synchronized (facilityEdiAccumulators) {
				EdiExportAccumulator accumulator = facilityEdiAccumulators.get(facility.getPersistentId());
				if (accumulator == null) {
					accumulator = new EdiExportAccumulator();
					facilityEdiAccumulators.put(facility.getPersistentId(), accumulator);
				}

				ListeningExecutorService executor = facilityExecutorServices.get(facility.getPersistentId());
				if (executor == null) {
					executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
					facilityExecutorServices.put(facility.getPersistentId(), executor);
				}

				FacilityAccumulatingExporter exporter = new FacilityAccumulatingExporter(accumulator, executor, stringifier, exportService);
				facilityEdiExporters.put(facility.getPersistentId(), exporter);
			}
		}
	}
}
