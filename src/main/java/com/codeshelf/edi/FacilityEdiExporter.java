package com.codeshelf.edi;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.ExportReceipt;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;

public interface FacilityEdiExporter extends Service {

	public void exportWiFinished(OrderHeader inOrder, Che inChe, WorkInstruction inWi);
	
	public ListenableFuture<ExportReceipt> exportOrderOnCartAdded(OrderHeader inOrder, Che inChe);

	public ListenableFuture<ExportReceipt> exportOrderOnCartFinished(OrderHeader inOrder, Che inChe);

	public void exportOrderOnCartRemoved(OrderHeader inOrder, Che inChe);
	
	public void setEdiExportTransport(EdiExportTransport transport);

	public void setStringifier(WiBeanStringifier stringifier);

	public void waitUntillQueueIsEmpty(int timeout);
}