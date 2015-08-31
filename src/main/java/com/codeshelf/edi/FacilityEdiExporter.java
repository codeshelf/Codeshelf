package com.codeshelf.edi;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.ExportReceipt;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;

public interface FacilityEdiExporter {

	public void notifyWiComplete(OrderHeader inOrder, Che inChe, WorkInstruction inWi);
	
	public void notifyOrderOnCart(OrderHeader inOrder, Che inChe);

	public ExportReceipt notifyOrderCompleteOnCart(OrderHeader inOrder, Che inChe);

	public void notifyOrderRemoveFromCart(OrderHeader inOrder, Che inChe);

}
