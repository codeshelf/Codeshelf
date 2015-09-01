package com.codeshelf.edi;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.ExportReceipt;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.google.common.util.concurrent.ListenableFuture;

public interface FacilityEdiExporter {

	public void exportWiFinished(OrderHeader inOrder, Che inChe, WorkInstruction inWi);
	
	public ListenableFuture<ExportReceipt> exportOrderOnCartAdded(OrderHeader inOrder, Che inChe);

	public ListenableFuture<ExportReceipt> exportOrderOnCartFinished(OrderHeader inOrder, Che inChe);

	public void exportOrderOnCartRemoved(OrderHeader inOrder, Che inChe);

}
