package com.codeshelf.edi;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.ExportReceipt;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;

public interface FacilityEdiExporter {

	public void exportWiFinished(OrderHeader inOrder, Che inChe, WorkInstruction inWi);
	
	public void exportOrderOnCartAdded(OrderHeader inOrder, Che inChe);

	public ExportReceipt exportOrderOnCartFinished(OrderHeader inOrder, Che inChe);

	public void exportOrderOnCartRemoved(OrderHeader inOrder, Che inChe);

}
