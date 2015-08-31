package com.codeshelf.edi;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.ExportReceipt;
import com.codeshelf.model.domain.OrderHeader;

public interface EdiExportTransport {

	boolean isLinked();
	
	void transportWiComplete(OrderHeader wiOrder, Che wiChe, String message);

	ExportReceipt transportOrderCompleteOnCart(OrderHeader wiOrder, Che wiChe, String message);

	void transportOrderRemoveFromCart(OrderHeader inOrder, Che inChe, String message);

	void transportOrderOnCart(OrderHeader inOrder, Che inChe, String message);

}
