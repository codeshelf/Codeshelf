/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DropboxGateway.java,v 1.37 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.ICsvAislesFileImporter;
import com.codeshelf.edi.ICsvCrossBatchImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.ICsvOrderLocationImporter;
import com.codeshelf.edi.IEdiImportGateway;
import com.codeshelf.model.EdiGatewayStateEnum;
import com.codeshelf.model.EdiTransportType;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.validation.BatchResult;
import com.dropbox.core.DbxAccountInfo;
import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxDelta;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxEntry.File;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuthNoRedirect;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;

import lombok.Getter;
import lombok.Setter;

// --------------------------------------------------------------------------
/**
 * Dropbox Service
 *
 * An EDI service allows the system to import/export EDI documentLocators to/from Dropbox.
 *
 * @author jeffw
 */

@Entity
@DiscriminatorValue("DROPBOX")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class DropboxGateway extends EdiGateway implements IEdiImportGateway{
	public static final String	DROPBOX_SERVICE_NAME	= "DROPBOX";

	private static final Logger	LOGGER					= LoggerFactory.getLogger(DropboxGateway.class);

	private static final String	APPKEY					= "0l3auhytaxn2q50";
	private static final String	APPSECRET				= "5syhdiyq0bd2oxq";
	//private static final Integer	LINK_RETRIES			= 20;
	//private static final Integer	RETRY_SECONDS			= 10 * 1000;

	private static final String	FACILITY_FOLDER_PATH	= "FACILITY_";

	private static final String	IMPORT_DIR_PATH			= "import";
	private static final String	IMPORT_ORDERS_PATH		= "orders";
	private static final String	IMPORT_BATCHES_PATH		= "batches";
	private static final String	IMPORT_AISLES_PATH		= "site";											// site configuration, where you drop the aisles files
	private static final String	IMPORT_INVENTORY_PATH	= "inventory";
	private static final String	IMPORT_LOCATIONS_PATH	= "locations";										// this is location aliases
	private static final String	IMPORT_SLOTTING_PATH	= "slotting";
	private static final String	PROCESSED_PATH			= "processed";

	private static final String	EXPORT_DIR_PATH			= "export";
	private static final String	EXPORT_WIS_PATH			= "work";

	@Column(nullable = true, name = "db_cursor")
	@Getter
	@Setter
	@JsonProperty
	private String				dbCursor;

	public DropboxGateway() {
		super();
		setTransportType(EdiTransportType.DROPBOX);
	}

	public static class DropboxCredentials {
		public static final String CURRENT_VERSION = "3.0";

		@Getter @Setter
		@Expose
		private String	version;
		
		@Getter @Setter
		@Expose
		private String	code;

		@Getter @Setter
		@Expose
		private String	accessToken;

		public DropboxCredentials(String code, String accessToken) {
			this.code = code;
			this.accessToken = accessToken;
			this.version = CURRENT_VERSION;
		}
		
		@Override
		public String toString() {
			Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
			return gson.toJson(this);
		}
	}
	
	private Optional<DropboxCredentials> getCredentials() {
		Gson mGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		DropboxCredentials credentials = null;
		String rawProviderCredentials = getProviderCredentials();
		try {
			credentials = mGson.fromJson(rawProviderCredentials, DropboxCredentials.class);
			//any parsable json that didn't have version field had accessCode in code field (v2.0)
			if (credentials != null && !DropboxCredentials.CURRENT_VERSION.equals(credentials.getVersion())) {  
				//probably had a prior accessToken stored in code
				credentials = new DropboxCredentials("", credentials.getCode());
				this.setProviderCredentials(credentials.toString());
				this.getDao().store(this);
			}
		} catch (JsonSyntaxException e) {
			//when not valid json probably was plain string (v1.0) and needs to be turned into accessToken
			String accessToken = rawProviderCredentials;
			credentials = new DropboxCredentials("", accessToken);
			this.setProviderCredentials(credentials.toString());
			this.getDao().store(this);
		}
		return Optional.fromNullable(credentials);
	}

	public final String getServiceName() {
		return DROPBOX_SERVICE_NAME;
	}

	@Override
	@JsonProperty
	public boolean getHasCredentials() {
		return !Strings.isNullOrEmpty(getProviderCredentials()) &&
				getCredentials().isPresent() &&
			   !Strings.isNullOrEmpty(getCredentials().get().getAccessToken());
	}

	@Override
	public boolean getUpdatesFromHost(ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationAliasImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter,
		ICsvAislesFileImporter inCsvAislesFileImporter) {

		// Make sure we believe that we're properly registered with the service before we try to contact it.
		if (!isLinked()) {
			return false;
		} 
		if (!testConnection()){
			LOGGER.warn("Dropbox connection error");
			return false;
		}

		DbxClient client = getClient();
		if (client != null && ensureBaseDirectories(client)) {
			DbxDelta<DbxEntry> page = getNextPage(client);
			while ((page != null) && (page.entries.size() > 0)) {
				iteratePage(client, page, inCsvOrderImporter, inCsvOrderLocationImporter, inCsvInventoryImporter, inCsvLocationAliasImporter, inCsvCrossBatchImporter, inCsvAislesFileImporter);
				// If we've processed everything from the page correctly then save the current dbCursor, and get the next page
				try {
					DropboxGateway.staticGetDao().store(this);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
				if (page.hasMore) {
					page = getNextPage(client);
				} else {
					page = null;
				}
			}
		}
		return true;
	}

	/**
	 * Returns false if and only if it is configured and it fails to get account info
	 */
	@Override
	public boolean testConnection() {
		try {
			DbxClient client = getClient();
			if(client != null) {
				DbxAccountInfo info = client.getAccountInfo();
				if (info == null) {
					LOGGER.warn("Dropbox connectivity check failed, account info null");
					return false;
				}
			}
		} catch(Exception e) {
			LOGGER.warn("Dropbox connectivity check failed", e);
			return false;
		}
		return true;
	}
	// --------------------------------------------------------------------------


	// --------------------------------------------------------------------------
	/**
	 * Get the next delta/page and persist the dbCursor that comes back from DropBox.
	 * @param inClientSession
	 * @return
	 */
	private DbxDelta<DbxEntry> getNextPage(DbxClient inClient) {
		DbxDelta<DbxEntry> result = null;
		try {
			result = inClient.getDelta(getDbCursor());
			if (result != null) {
				setDbCursor(result.cursor);
			}
		} catch (DbxException e) {
			LOGGER.error("Dropbox session error", e);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inClientSession
	 * @param inPage
	 */
	private void iteratePage(DbxClient inClient,
		DbxDelta<DbxEntry> inPage,
		ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationAliasImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter,
		ICsvAislesFileImporter inCsvAislesFileImporter) {
		for (DbxDelta.Entry<DbxEntry> entry : inPage.entries) {
			LOGGER.trace("Dropbox found: " + entry.lcPath);
			try {
				if (entry.metadata != null && entry.lcPath.startsWith(getFacilityImportPath()) && entry.metadata.isFile()) {
					handleImport(inClient, entry, inCsvOrderImporter, inCsvOrderLocationImporter, inCsvInventoryImporter, inCsvLocationAliasImporter, inCsvCrossBatchImporter, inCsvAislesFileImporter);
				}
			} catch (RuntimeException e) {
				LOGGER.error("", e);
				// Should any weird, uncaught errors in EDI process should fail this deltas page?
				// No - it could end up in a permanent loop.
				// We need out-of-band notification here.
				// result = false;
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private DbxClient getClient() {
		DbxClient result = null;
		Optional<DropboxCredentials> credentials = getCredentials();
		if (credentials.isPresent() && !Strings.isNullOrEmpty(credentials.get().getAccessToken())) {
			DbxRequestConfig config = new DbxRequestConfig("Codeshelf Interface", Locale.getDefault().toString());
			result = new DbxClient(config, credentials.get().getAccessToken());
		} 
		return result;
	}

	// public to allow test to find this.
	public String getFacilityPath() {
		String returnStr = new String(System.getProperty("file.separator") + FACILITY_FOLDER_PATH + getParent().getDomainId()).toLowerCase();
		// decomposed for debugging ease
		return returnStr;
	}

	private String getFacilityImportPath() {
		String returnStr = new String(getFacilityPath() + System.getProperty("file.separator") + IMPORT_DIR_PATH).toLowerCase();
		return returnStr;
	}

	// public to allow test to find this.
	public String getFacilityImportSubDirPath(final String inImportSubDirPath) {
		String returnStr = new String(getFacilityImportPath() + System.getProperty("file.separator") + inImportSubDirPath).toLowerCase();
		return returnStr;
	}

	private String getFacilityImportSubDirProcessedPath(final String inImportSubDirPath) {
		String returnStr = new String(getFacilityImportPath() + System.getProperty("file.separator") + inImportSubDirPath
				+ System.getProperty("file.separator") + PROCESSED_PATH).toLowerCase();
		return returnStr;
	}

	private String getFacilityExportPath() {
		String returnStr = new String(getFacilityPath() + System.getProperty("file.separator") + EXPORT_DIR_PATH).toLowerCase();
		return returnStr;
	}

	@SuppressWarnings("unused")
	private String getFacilityExportSubDirPath(final String inExportSubDirPath) {
		String returnStr = new String(getFacilityExportPath() + System.getProperty("file.separator") + inExportSubDirPath).toLowerCase();
		return returnStr;
	}

	// --------------------------------------------------------------------------
	/**
	 * Make sure all of the directories we need exist for import and export at the facility.
	 * @return
	 */
	private boolean ensureBaseDirectories(DbxClient inClient) {
		boolean result = false;

		result = ensureDirectory(inClient, getFacilityPath());
		result &= ensureDirectory(inClient, getFacilityImportPath());
		result &= ensureDirectory(inClient, getFacilityImportSubDirPath(IMPORT_ORDERS_PATH));
		result &= ensureDirectory(inClient, getFacilityImportSubDirProcessedPath(IMPORT_ORDERS_PATH));
		result &= ensureDirectory(inClient, getFacilityImportSubDirPath(IMPORT_AISLES_PATH));
		result &= ensureDirectory(inClient, getFacilityImportSubDirPath(IMPORT_BATCHES_PATH));
		result &= ensureDirectory(inClient, getFacilityImportSubDirProcessedPath(IMPORT_AISLES_PATH));
		result &= ensureDirectory(inClient, getFacilityImportSubDirProcessedPath(IMPORT_BATCHES_PATH));
		result &= ensureDirectory(inClient, getFacilityImportSubDirPath(IMPORT_INVENTORY_PATH));
		result &= ensureDirectory(inClient, getFacilityImportSubDirProcessedPath(IMPORT_INVENTORY_PATH));
		result &= ensureDirectory(inClient, getFacilityImportSubDirPath(IMPORT_LOCATIONS_PATH));
		result &= ensureDirectory(inClient, getFacilityImportSubDirProcessedPath(IMPORT_LOCATIONS_PATH));
		result &= ensureDirectory(inClient, getFacilityImportSubDirPath(IMPORT_SLOTTING_PATH));
		result &= ensureDirectory(inClient, getFacilityImportSubDirProcessedPath(IMPORT_SLOTTING_PATH));

		result &= ensureDirectory(inClient, getFacilityExportPath());
		result &= ensureDirectory(inClient, getFacilityImportSubDirPath(EXPORT_WIS_PATH));

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inClientSession
	 * @param inPath
	 * @return
	 */
	private boolean ensureDirectory(DbxClient inClient, String inPath) {
		boolean result = false;

		if (inPath != null)
			LOGGER.trace("ensureDirectory: " + inPath);
		try {
			DbxEntry dirEntry = inClient.getMetadata(inPath);
			if (dirEntry == null) {
				result = createDirectory(inClient, inPath);
			} else {
				result = true;
			}
		} catch (DbxException e) {
			LOGGER.error("Dropbox session error", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inClientSession
	 * @param inPath
	 * @return
	 */
	private boolean createDirectory(DbxClient inClient, String inPath) {
		boolean result = false;

		try {
			DbxEntry dirEntry = inClient.createFolder(inPath);
			if ((dirEntry != null) && (dirEntry.isFolder())) {
				result = true;
			}
		} catch (DbxException e) {
			LOGGER.error("Dropbox session error", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public String startLink() {

		try {
			setGatewayState(EdiGatewayStateEnum.LINKING);
			DropboxGateway.staticGetDao().store(this);
		} catch (DaoException e) {
			LOGGER.error("Unable to change dropbox service state", e);
		}

		DbxAppInfo appInfo = new DbxAppInfo(APPKEY, APPSECRET);
		DbxRequestConfig config = new DbxRequestConfig("Codeshelf Interface", Locale.getDefault().toString());
		DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);
		String result = webAuth.start();

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inAuthSession
	 * @param inAuthInfo
	 */
	public boolean finishLink(final String inDbxCode) {
		String priorCode = null;
		if (this.getCredentials().isPresent()) {
			priorCode = this.getCredentials().get().getCode();
		}
		if (!Strings.isNullOrEmpty(inDbxCode) && !inDbxCode.equals(priorCode)) {
			boolean result = false;

			try {
				DbxAppInfo appInfo = new DbxAppInfo(APPKEY, APPSECRET);
				DbxRequestConfig config = new DbxRequestConfig("Codeshelf Interface", Locale.getDefault().toString());
				DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);
				DbxAuthFinish authFinish = webAuth.finish(inDbxCode);
				String accessToken = authFinish.accessToken;

				try {
					// We did get an access token.
					if (accessToken == null) {
						setGatewayState(EdiGatewayStateEnum.LINK_FAILED);
					} else {
						DropboxCredentials credentials = new DropboxCredentials(inDbxCode, accessToken);
						setProviderCredentials(credentials.toString());
						setGatewayState(EdiGatewayStateEnum.LINKED);
						setDbCursor("");
						result = true;
					}
					DropboxGateway.staticGetDao().store(this);
				} catch (DaoException e) {
					LOGGER.error("Unable to store dropbox gateway change after linking", e);
				}
			} catch (DbxException e) {
				LOGGER.error("Unable to get accessToken for dropbox service", e);
			}

			return result;
			
		}
		else {
			return true;
		}
	}

	// --------------------------------------------------------------------------

	// --------------------------------------------------------------------------
	/**
	 * Easy way to optionally have extreme logging in the system for developer, but easy off in production
	 * @param inClient
	 * @param inFilePath
	 */
	private void logDetails(String inStuffToLog) {
		final boolean LOG_EXTREME_DETAILS = false;

		if (LOG_EXTREME_DETAILS)
			LOGGER.debug(inStuffToLog);
	}

	// --------------------------------------------------------------------------
	/**
	 * Does the file path match the import folder, either as .csv or as .csv.processing?
	 * @param inFilePath
	 * @param inWhichFolder
	 */
	private boolean fileMatches(String inFilePath, String inWhichFolder) {
		final String regexSuffix1 = "/[^/]+\\.csv"; // used to be only this
		final String regexSuffix2 = "/[^/]+\\.csv.processing";

		return inFilePath.matches(getFacilityImportSubDirPath(inWhichFolder) + regexSuffix1)
				|| inFilePath.matches(getFacilityImportSubDirPath(inWhichFolder) + regexSuffix2);
	}

	// --------------------------------------------------------------------------
	/**
	 * Helper function. Adds .processing unless the file already ends that way.
	 * @param inClient
	 * @param inFilePath
	 */
	private String renameToProcessing(DbxClient inClient, String inFilePath) {
		String filepath = inFilePath;

		if (!inFilePath.contains(".processing")) {
			logDetails("Add .processing");
			filepath = renameToRemoveAppend(inClient, inFilePath, null, "processing");
			logDetails("2)" + filepath);
		}
		return filepath;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void handleImport(DbxClient inClient,
		DbxDelta.Entry<DbxEntry> inEntry,
		ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationAliasImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter,
		ICsvAislesFileImporter inCsvAislesFileImporter) {

		// IMPORTANT: if you change this at all, please go to DropboxRealTest.java. See comments there about how to run it locally.
		// It cannot run on TeamCity

		try {
			String filepath = inEntry.lcPath;

			/*  Warning: this is called rather indiscriminately. Whatever Dropbox sees anywhere (even in export and processed folders) gets called here.
			 *
			 *  New protocol deals with file redrop of the same name before we finish processing. See DropboxRealTest.java
			 * 1) Just skip the file if .FAILED
			 * 2) just after converting file to stream, rename the file as ".processing". But not if already .processing.
			 * 3) Process the file, which as two main parts you do not fully see here:
			 * 3a) Convert to bean list
			 * 3b) Process the bean list
			 * 4) On success, move to processed folder and rename removing the .processing
			 * 4b) On fail, rename to .FAILED and leave in import
			 * 4c) On exception, rename to .FAILED and leave in import
			 *
			 * Goals to achieve with all this:
			 * New files get processed.
			 * New file overwritten with the same name gets processed. (The name will change in the processed folder to avoid duplicate)
			 * .FAILED files are plainly visible in the import folder
			 * .processing files are visible. Most importantly, the host may drop a new file of the same name and we do not move the new file when we finish processing.
			 * And, do not rename, move, etc. files that are not actually processed.
			 */

			// 1) Just skip the file if .FAILED
			String lowerCaseFilePath = filepath;
			lowerCaseFilePath = lowerCaseFilePath.toLowerCase(Locale.ENGLISH);
			if (lowerCaseFilePath.contains(".failed")) {
				return;
			}

			// We used to load to stream all files, then process only the direct matches. Let's filter a bit to not stream all the files in the processed folder.
			// If the file path contains /processed/ or /export/, skip it.
			if (filepath.contains("/processed/") || filepath.contains("/export/")) {
				logDetails("Skipping: " + filepath);
				return;
			}

			logDetails("1) loading file into stream for potential processing" + filepath);
			Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			File dbxFile = inClient.getFile(inEntry.lcPath, null, outputStream);
			InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(outputStream.toByteArray()));

			// Done within each if. We only want to rename as processing if we are actually processing.
			// 2) just after converting file to stream, rename the file as ".processing".
			// 3) Process the file stream, if we match what we are looking for
			boolean success = false;
			boolean processedAttempt = false;

			try {
				// orders-slotting needs to come before orders, because orders is a subset of the orders-slotting regex.
				if (fileMatches(filepath, IMPORT_SLOTTING_PATH)) {
					filepath = renameToProcessing(inClient, filepath);
					processedAttempt = true;
					success = inCsvOrderLocationImporter.importOrderLocationsFromCsvStream(reader, getParent(), ediProcessTime);
				} else if (fileMatches(filepath, IMPORT_ORDERS_PATH)) {
					filepath = renameToProcessing(inClient, filepath);
					processedAttempt = true;
					BatchResult<Object> results = inCsvOrderImporter.importOrdersFromCsvStream(reader, getParent(), ediProcessTime);
					String username = CodeshelfSecurityManager.getCurrentUserContext().getUsername();
					inCsvOrderImporter.persistDataReceipt(getParent(), username, dbxFile.name, dbxFile.clientMtime.getTime(), EdiTransportType.DROPBOX, results);

					success = results.isSuccessful();

				} else if (fileMatches(filepath, IMPORT_INVENTORY_PATH)) {
					filepath = renameToProcessing(inClient, filepath);
					processedAttempt = true;
					success = inCsvInventoryImporter.importSlottedInventoryFromCsvStream(reader, getParent(), ediProcessTime);
				}
				// Notice that there is no distinguisher for DDC file. Following should never execute anyway. Making it more obvious.
				// Jeff says DDC may come back.
				/*else if (false && fileMatches(filepath, IMPORT_INVENTORY_PATH)) {
					filepath = renameToProcessing(inClient, filepath);
					processedAttempt = true;
					success = inCsvInventoryImporter.importDdcInventoryFromCsvStream(reader, getParent(), ediProcessTime);
				}*/ else if (fileMatches(filepath, IMPORT_LOCATIONS_PATH)) {
					filepath = renameToProcessing(inClient, filepath);
					processedAttempt = true;
					success = inCsvLocationAliasImporter.importLocationAliasesFromCsvStream(reader, getParent(), ediProcessTime);
				} else if (fileMatches(filepath, IMPORT_BATCHES_PATH)) {
					filepath = renameToProcessing(inClient, filepath);
					processedAttempt = true;
					int numRecords = inCsvCrossBatchImporter.importCrossBatchesFromCsvStream(reader, getParent(), ediProcessTime);
					success = (numRecords > 0);
				} else if (fileMatches(filepath, IMPORT_AISLES_PATH)) {
					filepath = renameToProcessing(inClient, filepath);
					processedAttempt = true;
					success = inCsvAislesFileImporter.importAislesFileFromCsvStream(reader, getParent(), ediProcessTime);
				}
				if (!processedAttempt) {
					LOGGER.warn("Did not find importer for: " + filepath);
				}
			} catch (Exception e) {
				// 4c) On exception, rename to .FAILED and leave in import
				LOGGER.warn("Exception during dropbox file read. Left a .FAILED file for " + filepath, e);
				filepath = renameToRemoveAppend(inClient, filepath, "processing", "FAILED");
				logDetails("4c)" + filepath);
			}

			if (success) {

				// 4) On success, move to processed folder and rename removing the .processing
				logDetails("Moving file to processed");
				filepath = moveEntryToProcessed(inClient, filepath);
				logDetails("4a1)" + filepath);

				logDetails("Removing the .processed suffix");
				filepath = renameToRemoveAppend(inClient, filepath, "processing", null);
				logDetails("4a2)" + filepath);

			} else if (processedAttempt) {
				// 4b) On fail, rename to .FAILED and leave in import. But don't rename random files that we did not process
				LOGGER.warn("Failed to complete dropbox file read. Left a .FAILED file for " + filepath);
				logDetails("Removing .processing; adding .FAILED");
				filepath = renameToRemoveAppend(inClient, filepath, "processing", "FAILED");
				logDetails("4b)" + filepath);

			}
		} catch (DbxException | IOException e) {
			LOGGER.error("DBX or IO exception trying to get the next dropbox file", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Returns the new file path if file of that name exists. Normally returns the path passed in .
	 * @param inClient
	 * @param inProposedFilePath
	 */
	private String avoidFileNameCollision(DbxClient inClient, String inProposedFilePath) {
		String toPath = inProposedFilePath;
		try {
			if (inClient.getMetadata(toPath) != null) {
				// The to path already exists.  Tack on some extra versioning.
				toPath = String.format("%s.%s.%s",
						FilenameUtils.removeExtension(toPath),
						safeTimestamp(System.currentTimeMillis()),
						"csv");
			}
		} catch (DbxException e) {
			LOGGER.error("avoidFileNameCollision", e);
		}
		return toPath;
	}

	// --------------------------------------------------------------------------
	/**
	 * Returns the new file path.  Will attempt a modification if intended file name already exists in target directory
	 * @param inClient
	 * @param inExistingFilePath
	 */
	private String moveEntryToProcessed(DbxClient inClient, String inExistingFilePath) {
		// Figure out where to add the "processed" path path.
		String fromPath = inExistingFilePath;
		java.nio.file.Path path = Paths.get(fromPath);
		String toPath = path.getParent() + System.getProperty("file.separator") + PROCESSED_PATH
				+ System.getProperty("file.separator") + path.getFileName();

		try {
			toPath = avoidFileNameCollision(inClient, toPath);
			inClient.move(fromPath, toPath);
			LOGGER.info("Dropbox processed: " + fromPath);
		} catch (DbxException e) {
			LOGGER.error("moveEntryToProcessed", e);
		}
		return toPath;
	}

	// --------------------------------------------------------------------------
	/**
	 * Rename implemented as a "move" to different path. This adds the "dot".
	 * If the file name already ends in the appendStr, another is not added.
	 * Returns the new file path. Will attempt a modification if intended file name already exists in target directory
	 * @param inClient
	 * @param inExistingFilePath
	 */
	private String renameToRemoveAppend(DbxClient inClient, String inExistingFilePath, String inRemoveStr, String inAppendStr) {
		if (inAppendStr != null && (inAppendStr.isEmpty() || inAppendStr.charAt(0) == '.')) {
			LOGGER.error("Incorrect call to renameToAppend()");
			return inExistingFilePath;
		}
		if (inRemoveStr != null && (inRemoveStr.isEmpty() || inRemoveStr.charAt(0) == '.')) {
			LOGGER.error("Incorrect call to renameToAppend()");
			return inExistingFilePath;
		}
		String fromPath = inExistingFilePath;
		String toPath = fromPath;

		if (inRemoveStr != null) {
			String suffix = "." + inRemoveStr;
			if (toPath.endsWith(suffix)) {
				toPath = toPath.substring(0, toPath.length() - suffix.length());
			}
		}

		if (inAppendStr != null) {
			// if already .FAILED or .processing, do not add another
			String suffix = "." + inAppendStr;
			if (!toPath.endsWith(suffix))
				toPath = toPath + "." + inAppendStr;
			else
				LOGGER.warn("file already had the suffix " + suffix + " ; not adding again"); // happens in some exceptions
		}

		if (!fromPath.equals(toPath)) { // don't do anything if nothing changed
			toPath = avoidFileNameCollision(inClient, toPath);
			try {
				if (inClient.getMetadata(fromPath) == null) {
					LOGGER.error("File does not exist in renameToRemoveAppend()"); // Calling logic error
				} else {
					inClient.move(fromPath, toPath);
					LOGGER.debug("Dropbox rename: " + toPath);
				}
			} catch (DbxException e) {
				LOGGER.error("renameToRemoveAppend", e);
			}
		}
		return toPath;
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.model.domain.IEdiGateway#sendCompletedWorkInstructions(java.util.List)
	 */
	public void sendWorkInstructionsToHost(final String exportMessage) {
		// Do nothing at DropBox (for now).
	}
}
