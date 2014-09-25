/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DropboxService.java,v 1.37 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxDelta;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuthNoRedirect;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.edi.ICsvAislesFileImporter;
import com.gadgetworks.codeshelf.edi.ICsvCrossBatchImporter;
import com.gadgetworks.codeshelf.edi.ICsvInventoryImporter;
import com.gadgetworks.codeshelf.edi.ICsvLocationAliasImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderLocationImporter;
import com.gadgetworks.codeshelf.model.EdiDocumentStatusEnum;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistencyService;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * Dropbox Service
 * 
 * An EDI service allows the system to import/export EDI documentLocators to/from Dropbox.
 * 
 * @author jeffw
 */

@Entity
//@Table(name = "edi_service")
@DiscriminatorValue("DROPBOX")
//@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class DropboxService extends EdiServiceABC {

	@Inject
	public static ITypedDao<DropboxService>	DAO;

	@Singleton
	public static class DropboxServiceDao extends GenericDaoABC<DropboxService> implements ITypedDao<DropboxService> {
		@Inject
		public DropboxServiceDao(PersistencyService persistencyService) {
			super(persistencyService);
		}

		public final Class<DropboxService> getDaoClass() {
			return DropboxService.class;
		}
	}

	public static final String		DROPBOX_SERVICE_NAME	= "DROPBOX";

	private static final Logger		LOGGER					= LoggerFactory.getLogger(DropboxService.class);

	private static final String		APPKEY					= "0l3auhytaxn2q50";
	private static final String		APPSECRET				= "5syhdiyq0bd2oxq";
	//private static final Integer	LINK_RETRIES			= 20;
	//private static final Integer	RETRY_SECONDS			= 10 * 1000;

	private static final String		FACILITY_FOLDER_PATH	= "FACILITY_";

	private static final String		IMPORT_DIR_PATH			= "import";
	private static final String		IMPORT_ORDERS_PATH		= "orders";
	private static final String		IMPORT_BATCHES_PATH		= "batches";
	private static final String		IMPORT_AISLES_PATH		= "site"; // site configuration, where you drop the aisles files
	private static final String		IMPORT_INVENTORY_PATH	= "inventory";
	private static final String		IMPORT_LOCATIONS_PATH	= "locations"; // this is location aliases
	private static final String		IMPORT_SLOTTING_PATH	= "slotting";
	private static final String		PROCESSED_PATH			= "processed";

	private static final String		EXPORT_DIR_PATH			= "export";
	private static final String		EXPORT_WIS_PATH			= "work";

	private static final String		TIME_FORMAT				= "HH-mm-ss";

	@Column(nullable = true, name = "CURSOR")
	@Getter
	@Setter
	@JsonProperty
	private String					dbCursor;

	public DropboxService() {

	}
	
	public final static void setDao(ITypedDao<DropboxService> dao) {
		DropboxService.DAO = dao;
	}

	public final String getServiceName() {
		return DROPBOX_SERVICE_NAME;
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<DropboxService> getDao() {
		return DAO;
	}

	@Override
	@JsonProperty
	public boolean getHasCredentials() {
		return !Strings.isNullOrEmpty(getProviderCredentials());
	}
	
	public final boolean getUpdatesFromHost(ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationAliasImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter,
		ICsvAislesFileImporter inCsvAislesFileImporter) {
		boolean result = false;

		// Make sure we believe that we're properly registered with the service before we try to contact it.
		if (getServiceStateEnum().equals(EdiServiceStateEnum.LINKED)) {

			DbxClient client = getClient();
			if (client != null) {
				result = checkForChangedDocuments(client,
					inCsvOrderImporter,
					inCsvOrderLocationImporter,
					inCsvInventoryImporter,
					inCsvLocationAliasImporter,
					inCsvCrossBatchImporter,
					inCsvAislesFileImporter);
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inClientSession
	 */
	private boolean checkForChangedDocuments(DbxClient inClient,
		ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationAliasImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter,
		ICsvAislesFileImporter inCsvAislesFileImporter) {
		boolean result = false;

		if (ensureBaseDirectories(inClient)) {
			DbxDelta<DbxEntry> page = getNextPage(inClient);
			while ((page != null) && (page.entries.size() > 0)) {
				// Signal that we got some deltas
				result = true;
				if (iteratePage(inClient,
					page,
					inCsvOrderImporter,
					inCsvOrderLocationImporter,
					inCsvInventoryImporter,
					inCsvLocationAliasImporter,
					inCsvCrossBatchImporter,
					inCsvAislesFileImporter)) {
					// If we've processed everything from the page correctly then save the current dbCursor, and get the next page
					try {
						EdiServiceABC.DAO.store(this);
					} catch (DaoException e) {
						LOGGER.error("", e);
					}

					if (page.hasMore) {
						page = getNextPage(inClient);
					} else {
						page = null;
					}
				}
			}
		}
		return result;
	}

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
	private boolean iteratePage(DbxClient inClient,
		DbxDelta<DbxEntry> inPage,
		ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationAliasImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter,
		ICsvAislesFileImporter inCsvAislesFileImporter) {
		boolean result = true;

		for (DbxDelta.Entry<DbxEntry> entry : inPage.entries) {
			LOGGER.info("Dropbox found: " + entry.lcPath);
			try {
				if (entry.metadata != null) {
					// Add, or modify.
					result &= processEntry(inClient,
						entry,
						inCsvOrderImporter,
						inCsvOrderLocationImporter,
						inCsvInventoryImporter,
						inCsvLocationAliasImporter,
						inCsvCrossBatchImporter,
						inCsvAislesFileImporter);
				} else {
					result &= removeEntry(inClient, entry);
				}
			} catch (RuntimeException e) {
				LOGGER.error("", e);
				// Should any weird, uncaught errors in EDI process should fail this deltas page?
				// No - it could end up in a permanent loop.
				// We need out-of-band notification here.
				// result = false;
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inPath
	 * @return
	 */
	private final EdiDocumentLocator getDocumentLocatorByPath(String inPath) {
		EdiDocumentLocator result = null;

		for (EdiDocumentLocator locator : getDocumentLocators()) {
			if (locator.getDocumentPath().equals(inPath)) {
				result = locator;
				break;
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private DbxClient getClient() {

		DbxClient result = null;

		String credentials = getProviderCredentials();
		if (credentials != null) {
			DbxRequestConfig config = new DbxRequestConfig("Codeshelf Interface", Locale.getDefault().toString());
			result = new DbxClient(config, credentials);
		}
		return result;
	}

	private String getFacilityPath() {
		return new String(System.getProperty("file.separator") + FACILITY_FOLDER_PATH + getParent().getDomainId()).toLowerCase();
	}

	private String getFacilityImportPath() {
		return new String(getFacilityPath() + System.getProperty("file.separator") + IMPORT_DIR_PATH).toLowerCase();
	}

	private String getFacilityImportSubDirPath(final String inImportSubDirPath) {
		return new String(getFacilityImportPath() + System.getProperty("file.separator") + inImportSubDirPath).toLowerCase();
	}

	private String getFacilityImportSubDirProcessedPath(final String inImportSubDirPath) {
		return new String(getFacilityImportPath() + System.getProperty("file.separator") + inImportSubDirPath
				+ System.getProperty("file.separator") + PROCESSED_PATH).toLowerCase();
	}

	private String getFacilityExportPath() {
		return new String(getFacilityPath() + System.getProperty("file.separator") + EXPORT_DIR_PATH).toLowerCase();
	}

	@SuppressWarnings("unused")
	private String getFacilityExportSubDirPath(final String inExportSubDirPath) {
		return new String(getFacilityExportPath() + System.getProperty("file.separator") + inExportSubDirPath).toLowerCase();
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
	public final String startLink() {

		try {
			setServiceStateEnum(EdiServiceStateEnum.LINKING);
			EdiServiceABC.DAO.store(this);
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
	public final boolean finishLink(final String inDbxCode) {

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
					setServiceStateEnum(EdiServiceStateEnum.LINK_FAILED);
				} else {
					setProviderCredentials(accessToken);
					setServiceStateEnum(EdiServiceStateEnum.LINKED);
					setDbCursor("");
					result = true;
				}
				EdiServiceABC.DAO.store(this);
			} catch (DaoException e) {
				LOGGER.error("Unable to store dropboxservice change after linking", e);
			}
		} catch (DbxException e) {
			LOGGER.error("Unable to get accessToken for dropbox service", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inEntry
	 */
	private boolean processEntry(DbxClient inClient,
		DbxDelta.Entry<DbxEntry> inEntry,
		ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationAliasImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter,
		ICsvAislesFileImporter inCsvAislesFileImporter) {
		boolean result = true;

		boolean shouldUpdateEntry = false;

		if (inEntry.lcPath.startsWith(getFacilityImportPath())) {
			if (inEntry.metadata.isFile()) {
				handleImport(inClient,
					inEntry,
					inCsvOrderImporter,
					inCsvOrderLocationImporter,
					inCsvInventoryImporter,
					inCsvLocationAliasImporter,
					inCsvCrossBatchImporter,
					inCsvAislesFileImporter);
				shouldUpdateEntry = true;
			}
		}

		if (shouldUpdateEntry) {
			EdiDocumentLocator locator = EdiDocumentLocator.DAO.findByDomainId(this, inEntry.lcPath);
			if (locator == null) {
				locator = new EdiDocumentLocator();
				locator.setParent(this);
				locator.setReceived(new Timestamp(System.currentTimeMillis()));
				locator.setDocumentStateEnum(EdiDocumentStatusEnum.NEW);
				locator.setDomainId(inEntry.lcPath);
				locator.setDocumentPath(inEntry.metadata.asFile().path);
				locator.setDocumentName(inEntry.metadata.asFile().name);

				addEdiDocumentLocator(locator);
				try {
					EdiDocumentLocator.DAO.store(locator);
				} catch (DaoException e) {
					LOGGER.error("", e);
					result = false;
				}
			}
		}

		return result;
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

		try {

			String filepath = inEntry.lcPath;
			Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());

			//DropboxInputStream stream = inClient.getFileStream(filepath, null);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			//DbxEntry.File downloadedFile = 
			inClient.getFile(inEntry.lcPath, null, outputStream);
			InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(outputStream.toByteArray()));

			boolean success = false;

			// orders-slotting needs to come before orders, because orders is a subset of the orders-slotting regex.
			if (filepath.matches(getFacilityImportSubDirPath(IMPORT_SLOTTING_PATH) + "/[^/]+\\.csv")) {
				success = inCsvOrderLocationImporter.importOrderLocationsFromCsvStream(reader, getParent(), ediProcessTime);
			} else if (filepath.matches(getFacilityImportSubDirPath(IMPORT_ORDERS_PATH) + "/[^/]+\\.csv")) {
				success = inCsvOrderImporter.importOrdersFromCsvStream(reader, getParent(), ediProcessTime).isSuccessful();
			} else if (filepath.matches(getFacilityImportSubDirPath(IMPORT_INVENTORY_PATH) + "/[^/]+\\.csv")) {
				success = inCsvInventoryImporter.importSlottedInventoryFromCsvStream(reader, getParent(), ediProcessTime);
			} else if (filepath.matches(getFacilityImportSubDirPath(IMPORT_INVENTORY_PATH) + "/[^/]+\\.csv")) {
				success = inCsvInventoryImporter.importDdcInventoryFromCsvStream(reader, getParent(), ediProcessTime);
			} else if (filepath.matches(getFacilityImportSubDirPath(IMPORT_LOCATIONS_PATH) + "/[^/]+\\.csv")) {
				success = inCsvLocationAliasImporter.importLocationAliasesFromCsvStream(reader, getParent(), ediProcessTime);
			} else if (filepath.matches(getFacilityImportSubDirPath(IMPORT_BATCHES_PATH) + "/[^/]+\\.csv")) {
				success = inCsvCrossBatchImporter.importCrossBatchesFromCsvStream(reader, getParent(), ediProcessTime);
			} else if (filepath.matches(getFacilityImportSubDirPath(IMPORT_AISLES_PATH) + "/[^/]+\\.csv")) {
				success = inCsvAislesFileImporter.importAislesFileFromCsvStream(reader, getParent(), ediProcessTime);
			}

			if (success) {
				moveEntryToProcessed(inClient, inEntry);
			}
		} catch (DbxException | IOException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inClient
	 * @param inEntry
	 */
	private void moveEntryToProcessed(DbxClient inClient, DbxDelta.Entry<DbxEntry> inEntry) {
		// Figure out where to add the "processed" path path.
		String fromPath = inEntry.lcPath;
		java.nio.file.Path path = Paths.get(fromPath);
		String toPath = path.getParent() + System.getProperty("file.separator") + PROCESSED_PATH
				+ System.getProperty("file.separator") + path.getFileName();

		try {
			if (inClient.getMetadata(toPath) != null) {
				// The to path already exists.  Tack on some extra versioning.
				toPath = FilenameUtils.removeExtension(toPath) + "." + new SimpleDateFormat(TIME_FORMAT).format(System.currentTimeMillis()) + ".csv";
			}

			inClient.move(fromPath, toPath);
			LOGGER.info("Dropbox processed: " + fromPath);
		} catch (DbxException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inEntry
	 */
	private boolean removeEntry(DbxClient inClient, DbxDelta.Entry<DbxEntry> inEntry) {
		boolean result = true;

		EdiDocumentLocator locator = getDocumentLocatorByPath(inEntry.lcPath);
		if (locator != null) {
			try {
				EdiDocumentLocator.DAO.delete(locator);
			} catch (DaoException e) {
				LOGGER.error("", e);
				result = false;
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.IEdiService#sendCompletedWorkInstructions(java.util.List)
	 */
	public void sendWorkInstructionsToHost(final List<WorkInstruction> inWiMessage) {
		// Do nothing at DropBox (for now).
	}
}
