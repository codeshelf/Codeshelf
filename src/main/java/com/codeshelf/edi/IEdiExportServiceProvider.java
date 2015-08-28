package com.codeshelf.edi;

import com.codeshelf.model.domain.Facility;

public interface IEdiExportServiceProvider {

	public IEdiExportService getWorkInstructionExporter(Facility facility);

}
