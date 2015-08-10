package com.codeshelf.service;

import java.io.IOException;

import com.codeshelf.edi.ICsvAislesFileImporter;
import com.codeshelf.edi.ICsvCrossBatchImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.ICsvOrderLocationImporter;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.EdiServiceABC;
import com.codeshelf.model.domain.IDomainObject;

class PhonyPFSWebExportService extends EdiServiceABC{
	public PhonyPFSWebExportService(){
	}
	
	@Override
	public String getServiceName() {
		return "phonyExporter";
	}
	@Override
	public boolean getUpdatesFromHost(ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationsImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter,
		ICsvAislesFileImporter inCsvAislesFileImporter) {
		return false;
	}
	@Override
	public void sendWorkInstructionsToHost(String exportMessage) throws IOException {			
	}
	@Override
	public <T extends IDomainObject> ITypedDao<T> getDao() {
		return null;
	}
	@Override
	public boolean getHasCredentials() {
		return false;
	}
	
	/**
	 * Override this to use the standard output accumulator on your EDI service.
	 * If the standard accumulator is not suitable, also override createEdiOutputAccumulator()
	 */
	@Override
	protected boolean needsEdiOutputAccumulator() {
		return true;
	}

	/**
	 * Override this to use the standard groovy WI transformation (and future ones) on your EDI service that includes output.
	 */
	@Override
	protected boolean needsGroovyOutputExtensions() {
		return true;
	}

}