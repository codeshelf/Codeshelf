package com.codeshelf.edi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.service.AbstractCodeshelfIdleService;
import com.codeshelf.service.ExtensionPointService;

public class EdiExporterProvider extends AbstractCodeshelfIdleService {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(EdiExporterProvider.class);

	//per facility 
	//  export queue 
	//  
	
	
	
	private Map<UUID, EdiExportAccumulator> facilityEdiAccumulators = new HashMap<>();
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

				FacilityAccumulatingExporter exporter = new FacilityAccumulatingExporter(accumulator, stringifier, exportService);
				facilityEdiExporters.put(facility.getPersistentId(), exporter);
			}
		}
	}

	@Override
	protected void startUp() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void shutDown() throws Exception {
		// TODO Auto-generated method stub
		
	}
}
