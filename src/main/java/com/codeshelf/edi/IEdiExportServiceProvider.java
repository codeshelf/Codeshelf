package com.codeshelf.edi;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IEdiService;

public interface IEdiExportServiceProvider {

	public IEdiService getWorkInstructionExporter(Tenant tenant,Facility facility);

}
