package com.codeshelf.model.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.IEdiExportGateway;
import com.codeshelf.edi.EdiFileWriteException;
import com.codeshelf.edi.ICsvAislesFileImporter;
import com.codeshelf.edi.ICsvCrossBatchImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.ICsvOrderLocationImporter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

@Entity
@DiscriminatorValue("SFTP_WIS")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class SftpWiGateway extends SftpGateway implements IEdiExportGateway {
	static final Logger			LOGGER						= LoggerFactory.getLogger(SftpWiGateway.class);
	public static final String	SFTP_SERVICE_NAME			= "SFTP_WIS";

	public SftpWiGateway() {
		super();
	}
	
	public SftpWiGateway(String domainId) {
		super(domainId);
	}

	@Override
	public String getServiceName() {
		return SFTP_SERVICE_NAME;
	}

	@Override
	public boolean getUpdatesFromHost(ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationsImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter,
		ICsvAislesFileImporter inCsvAislesFileImporter) {

		// not implemented in this service

		return true;
	}

	@Override
	public void transportWiFinished(String inOrderId, String inCheGuid, String exportMessage) {
	
	}

	@Override
	public void transportOrderOnCartRemoved(String inOrderId, String inCheGuid, String message) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public FileExportReceipt transportOrderOnCartFinished(String inOrderId, String inCheGuid, String contents) throws EdiFileWriteException {
		String filename = String.format("COMPLETE_%s_%s_%s.DAT",  inOrderId, inCheGuid, System.currentTimeMillis());
		final String absoluteFilename = this.getConfiguration().getExportPath() + "/" + filename;
		return uploadAsFile(contents, absoluteFilename);
	}

	@Override
	public FileExportReceipt transportOrderOnCartAdded(String inOrderId, String inCheGuid, String contents) throws EdiFileWriteException {
		String filename = String.format("LOADED_%s_%s_%s.DAT",  inOrderId, inCheGuid, System.currentTimeMillis());
		final String absoluteFilename = this.getConfiguration().getExportPath() + "/" + filename;
		return uploadAsFile(contents, absoluteFilename);
	}
}
