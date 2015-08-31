package com.codeshelf.edi;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.ExportReceipt;
import com.codeshelf.model.domain.OrderHeader;

public interface EdiExportTransport {

	boolean isLinked();
	
	void transportWiComplete(OrderHeader wiOrder, Che wiChe, String message);

	ExportReceipt transportOrderOnCartFinished(OrderHeader wiOrder, Che wiChe, String message);

	void transportOrderOnCartRemoved(OrderHeader inOrder, Che inChe, String message);

	void transportOrderOnCartAdded(OrderHeader inOrder, Che inChe, String message);

}
