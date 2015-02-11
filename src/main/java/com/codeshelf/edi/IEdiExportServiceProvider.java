package com.codeshelf.edi;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IEdiService;

public interface IEdiExportServiceProvider {

	public IEdiService getWorkInstructionExporter(Facility facility);

}
