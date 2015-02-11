package com.gadgetworks.codeshelf.edi;

import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IEdiService;

public interface IEdiExportServiceProvider {

	public IEdiService getWorkInstructionExporter(Facility facility);

}
