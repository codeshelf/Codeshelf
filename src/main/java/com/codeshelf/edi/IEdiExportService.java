package com.codeshelf.edi;

import java.io.IOException;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.ExportReceipt;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;

public interface IEdiExportService {

	void notifyWiComplete(WorkInstruction inWi);

	ExportReceipt notifyOrderCompleteOnCart(OrderHeader wiOrder, Che wiChe);

	void notifyOrderRemoveFromCart(OrderHeader inOrder, Che inChe);

	void notifyOrderOnCart(OrderHeader inOrder, Che inChe);

	boolean isLinked();

	void sendWorkInstructionsToHost(String messageBody) throws IOException;


}
