package com.codeshelf.edi;

import java.io.IOException;

import com.codeshelf.model.domain.FileExportReceipt;

/**
 * Implementations are only responsible for attempting transport to the host.
 * Implementations should block until successful and throw if not successful with descriptive reason
 * 
 * @author pmonteiro
 *
 */
public interface IEdiExportGateway extends IEdiGateway {
	void transportWiFinished(String wiOrderId, String wiCheGuid, String message) throws IOException;

	FileExportReceipt transportOrderOnCartFinished(String wiOrderId, String wiCheGuid, String message) throws IOException;

	void transportOrderOnCartRemoved(String inOrderId, String inCheGuid, String message) throws IOException;

	FileExportReceipt transportOrderOnCartAdded(String inOrderId, String inCheGuid, String message) throws IOException;
}
