package com.codeshelf.model.domain;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.ICsvAislesFileImporter;
import com.codeshelf.edi.ICsvCrossBatchImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.ICsvOrderLocationImporter;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.validation.BatchResult;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

@Entity
@DiscriminatorValue("SFTP_ORDERS")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class SftpOrdersEdiService extends AbstractSftpEdiService {
	public static class SftpOrdersEdiServiceDao extends GenericDaoABC<SftpOrdersEdiService> implements ITypedDao<SftpOrdersEdiService> {
		public final Class<SftpOrdersEdiService> getDaoClass() {
			return SftpOrdersEdiService.class;
		}
	}

	static final Logger			LOGGER						= LoggerFactory.getLogger(SftpOrdersEdiService.class);
	public static final String	SFTP_SERVICE_NAME			= "SFTP_ORDERS";
	private static final String	FILENAME_SUFFIX_FAILED		= ".FAILED";
	private static final String	FILENAME_SUFFIX_PROCESSING	= ".PROCESSING";

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

		try {
			processOrders(inCsvOrderImporter);
		} catch (JSchException | SftpException | IOException e) {
			LOGGER.error("SFTP failure", e);
			return false;
		}
		return true;
	}

	private boolean processOrders(ICsvOrderImporter inCsvOrderImporter) throws JSchException, SftpException, IOException {
		boolean failure = false;
		
		connect();

		try {
			ArrayList<String> filesToImport = retrieveImportFileList();
			// process all regular files found
			if(filesToImport.size() == 0) {
				LOGGER.info("No files to process at {}",getConfiguration().getUrl());
			} else {
				for(int ix = 0; ix < filesToImport.size(); ix++ ) {
					String filename = filesToImport.get(ix);
					
					LOGGER.info("processing file: {}",filename);
					if (!processImportFile(inCsvOrderImporter, filename))
						failure = true;
				}
			}
		} finally {			
			disconnect();
		}

		return !failure;
	}

	private boolean processImportFile(ICsvOrderImporter inCsvOrderImporter, String filename) throws SftpException, IOException {
		boolean success = false;
		if(filename.endsWith(FILENAME_SUFFIX_FAILED)) {
			// ignore
			LOGGER.debug("Found FAILED file in SFTP: {}",filename);
		} else if(filename.endsWith(FILENAME_SUFFIX_PROCESSING)) {
			// warn
			LOGGER.warn("Found incomplete PROCESSING file in SFTP: {}",filename);
		} else if(getConfiguration().matchOrdersFilename(filename)) {
			// process this orders file
			String originalPath = getConfiguration().getImportPath()+"/"+filename;
			String processingPath = originalPath + FILENAME_SUFFIX_PROCESSING;
			String failedPath = originalPath + FILENAME_SUFFIX_FAILED;
			String archivePath = getConfiguration().getArchivePath()+"/"+filename;
			Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());

			// rename to .processing and begin
			ChannelSftp sftp = getChannel();
			sftp.rename(originalPath, processingPath);
			InputStream fileStream = sftp.get(processingPath);
			InputStreamReader reader = new InputStreamReader(fileStream);
			
			BatchResult<Object> results = inCsvOrderImporter.importOrdersFromCsvStream(reader, getParent(), ediProcessTime);
			String username = CodeshelfSecurityManager.getCurrentUserContext().getUsername();
			
			// record receipt
			long receivedTime = System.currentTimeMillis();
			//long receivedTime = 1000 * file.getAttrs().getMTime();
			inCsvOrderImporter.persistDataReceipt(getParent(), username, filename, receivedTime, results);

			success = results.isSuccessful();
			
			if(success) {
				sftp.rename(processingPath, archivePath);
			} else {
				sftp.rename(processingPath, failedPath);
			}
						
		} else {
			LOGGER.warn("Unexpected file in SFTP EDI: {}",filename);
		}
		return success;
		
	}

	@Override
	public void sendWorkInstructionsToHost(String exportMessage) throws IOException {
		// not implemented in this service
	}

	@SuppressWarnings("unchecked")
	@Override
	public ITypedDao<SftpOrdersEdiService> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<SftpOrdersEdiService> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(SftpOrdersEdiService.class);
	}

}
