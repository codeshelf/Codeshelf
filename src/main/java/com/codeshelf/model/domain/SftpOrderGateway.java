package com.codeshelf.model.domain;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.EdiFileWriteException;
import com.codeshelf.edi.ICsvAislesFileImporter;
import com.codeshelf.edi.ICsvCrossBatchImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.ICsvOrderLocationImporter;
import com.codeshelf.edi.IEdiImportGateway;
import com.codeshelf.edi.SftpConfiguration;
import com.codeshelf.model.EdiTransportType;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.validation.BatchResult;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpStatVFS;

@Entity
@DiscriminatorValue("SFTP_ORDERS")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class SftpOrderGateway extends SftpGateway implements IEdiImportGateway{
	static final Logger			LOGGER						= LoggerFactory.getLogger(SftpOrderGateway.class);
	public static final String	SFTP_SERVICE_NAME			= "SFTP_ORDERS";
	private static final String	FILENAME_SUFFIX_FAILED		= ".FAILED";
	private static final String	FILENAME_SUFFIX_PROCESSING	= ".PROCESSING";

	public SftpOrderGateway() {
		super();
	}
	
	public SftpOrderGateway(String domainId) {
		super(domainId);
	}
	
	@Override
	public String getServiceName() {
		return SFTP_SERVICE_NAME;
	}

	@Override
	synchronized public boolean getUpdatesFromHost(ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationsImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter,
		ICsvAislesFileImporter inCsvAislesFileImporter) {

		try {
			processOrders(inCsvOrderImporter);
		} catch (JSchException | SftpException | IOException e) {
			LOGGER.warn("SFTP failure while processing orders", e);
			return false;
		}
		return true;
	}

	private boolean processOrders(ICsvOrderImporter inCsvOrderImporter) throws JSchException, SftpException, IOException {
		boolean failure = false;
		
		ChannelSftp sftp = connect();

		try {
			ArrayList<String> filesToImport = retrieveImportFileList(sftp);
			// process all regular files found
			if(filesToImport.size() == 0) {
				LOGGER.info("No files to process at {}",getConfiguration().getUrl());
			} else {
				for(int ix = 0; ix < filesToImport.size(); ix++ ) {
					String filename = filesToImport.get(ix);
					
					if (!processImportFile(sftp, inCsvOrderImporter, filename))
						failure = true;
				}
			}
		} finally {			
			disconnect();
		}

		return !failure;
	}

	private boolean processImportFile(ChannelSftp sftp, ICsvOrderImporter inCsvOrderImporter, String filename) throws SftpException, IOException {
		boolean success = false;
		if(filename.endsWith(FILENAME_SUFFIX_FAILED)) {
			// ignore
			LOGGER.debug("Skipping FAILED file in SFTP: {}",filename);
		} else if(filename.endsWith(FILENAME_SUFFIX_PROCESSING)) {
			// warn
			LOGGER.warn("Skipping incomplete PROCESSING file in SFTP: {}",filename);
		} else if(getConfiguration().matchOrdersFilename(filename)) {
			// process this orders file
			String originalPath = getConfiguration().getImportPath()+"/"+filename;
			String processingPath = originalPath + FILENAME_SUFFIX_PROCESSING;
			String failedPath = originalPath + FILENAME_SUFFIX_FAILED;
			String archivePath = getConfiguration().getArchivePath()+"/"+filename;
			Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());

			// record receipt
			long receivedTime = System.currentTimeMillis();
			// rename to .processing and begin
			renameFile(sftp, originalPath, processingPath);
			BatchResult<Object> results = null;
			try(InputStream fileStream = sftp.get(processingPath)) {
				InputStreamReader reader = new InputStreamReader(fileStream);
				results = inCsvOrderImporter.importOrdersFromCsvStream(reader, getParent(), ediProcessTime);
			}
			String username = CodeshelfSecurityManager.getCurrentUserContext().getUsername();
			//long receivedTime = 1000 * file.getAttrs().getMTime();
			inCsvOrderImporter.persistDataReceipt(getParent(), username, filename, receivedTime, EdiTransportType.SFTP, results);

			success = results.isSuccessful();
			
			if(success) {
				renameFile(sftp, processingPath, archivePath);
			} else {
				renameFile(sftp, processingPath, failedPath);
			}
						
		} else {
			LOGGER.warn("Skipping unexpected file in SFTP EDI: {} {}", filename, toSftpChannelDebug());
		}
		return success;
		
	}

	private void renameFile(ChannelSftp sftp, String originalPath, String processingPath) throws IOException {
		try {
			sftp.rename(originalPath, processingPath);
		} catch(SftpException e) {
			String msg = String.format("unable to rename from %s to %s for %s", originalPath, processingPath, toSftpChannelDebug());
			throw new IOException(msg, e);
		}

	}

	@Override
	public List<String> checkForAvailableSpaceIssues(long minAvailableFiles, long minAvailableSpaceMB) throws EdiFileWriteException {
		SftpConfiguration conf = getConfiguration();
		SftpStatVFS stats = getDirectoryStats(conf.getArchivePath());
		return checkForAvailableSpaceIssues(minAvailableFiles, minAvailableSpaceMB, stats);
	}	
}
