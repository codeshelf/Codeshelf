package com.codeshelf.edi;

import java.io.IOException;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.ExportReceipt;
import com.codeshelf.model.domain.OrderHeader;

/**
 * Implementations are only responsible for attempting transport to the host.
 * Implementations should block until successful and throw if not successful with descriptive reason
 * 
 * @author pmonteiro
 *
 */
public interface EdiExportTransport {

	boolean isLinked();
	
	void transportWiComplete(OrderHeader wiOrder, Che wiChe, String message) throws IOException;

	ExportReceipt transportOrderOnCartFinished(OrderHeader wiOrder, Che wiChe, String message);

	void transportOrderOnCartRemoved(OrderHeader inOrder, Che inChe, String message);

	void transportOrderOnCartAdded(OrderHeader inOrder, Che inChe, String message);

}
