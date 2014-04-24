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
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxDelta;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuthNoRedirect;
import com.gadgetworks.codeshelf.edi.ICsvCrossBatchImporter;
import com.gadgetworks.codeshelf.edi.ICsvInventoryImporter;
import com.gadgetworks.codeshelf.edi.ICsvLocationAliasImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderLocationImporter;
import com.gadgetworks.codeshelf.model.EdiDocumentStatusEnum;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
@Table(name = "edi_service")
@DiscriminatorValue("DROPBOX")
@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class DropboxService extends EdiServiceABC {

	@Inject
	public static ITypedDao<DropboxService>	DAO;

	@Singleton
	public static class DropboxServiceDao extends GenericDaoABC<DropboxService> implements ITypedDao<DropboxService> {
		@Inject
		public DropboxServiceDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<DropboxService> getDaoClass() {
			return DropboxService.class;
		}
	}

	public static final String		DROPBOX_SERVICE_NAME	= "DROPBOX";

	private static final Logger		LOGGER					= LoggerFactory.getLogger(DropboxService.class);

	private static final String		APPKEY					= "0l3auhytaxn2q50";
	private static final String		APPSECRET				= "5syhdiyq0bd2oxq";
	private static final Integer	LINK_RETRIES			= 20;
	private static final Integer	RETRY_SECONDS			= 10 * 1000;

	private static final String		FACILITY_FOLDER_PATH	= "FACILITY_";

	private static final String		IMPORT_DIR_PATH			= "import";
	private static final String		IMPORT_ORDERS_PATH		= "orders";
	private static final String		IMPORT_BATCHES_PATH		= "batches";
	private static final String		IMPORT_INVENTORY_PATH	= "inventory";
	private static final String		IMPORT_LOCATIONS_PATH	= "locations";
	private static final String		IMPORT_SLOTTING_PATH	= "slotting";

	private static final String		EXPORT_DIR_PATH			= "export";
	private static final String		EXPORT_WIS_PATH			= "work";

	@Column(nullable = true, name = "CURSOR")
	@Getter
	@Setter
	@JsonProperty
	private String					dbCursor;

	public DropboxService() {

	}

	public final String getServiceName() {
		return DROPBOX_SERVICE_NAME;
	}

	public final ITypedDao<DropboxService> getDao() {
		return DAO;
	}

	public final boolean getUpdatesFromHost(ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationAliasImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter) {
		Boolean result = false;

		// Make sure we believe that we're properly registered with the service before we try to contact it.
		if (getServiceStateEnum().equals(EdiServiceStateEnum.LINKED)) {

			DbxClient client = getClient();
			if (client != null) {
				result = checkForChangedDocuments(client,
					inCsvOrderImporter,
					inCsvOrderLocationImporter,
					inCsvInventoryImporter,
					inCsvLocationAliasImporter,
					inCsvCrossBatchImporter);
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inClientSession
	 */
	private Boolean checkForChangedDocuments(DbxClient inClient,
		ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationAliasImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter) {
		Boolean result = false;

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
					inCsvCrossBatchImporter)) {
					// If we've processed everything from the page correctly then save the current dbCursor, and get the next page
					try {
						DropboxService.DAO.store(this);
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
	private Boolean iteratePage(DbxClient inClient,
		DbxDelta<DbxEntry> inPage,
		ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationAliasImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter) {
		Boolean result = true;

		for (DbxDelta.Entry<DbxEntry> entry : inPage.entries) {
			LOGGER.info(entry.lcPath);
			try {
				if (entry.metadata != null) {
					// Add, or modify.
					result &= processEntry(inClient,
						entry,
						inCsvOrderImporter,
						inCsvOrderLocationImporter,
						inCsvInventoryImporter,
						inCsvLocationAliasImporter,
						inCsvCrossBatchImporter);
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
		return new String("/" + FACILITY_FOLDER_PATH + getParent().getDomainId()).toLowerCase();
	}

	private String getFacilityImportPath() {
		return new String(getFacilityPath() + "/" + IMPORT_DIR_PATH).toLowerCase();
	}

	private String getFacilityImportSubDirPath(final String inImportSubDirPath) {
		return new String(getFacilityImportPath() + "/" + inImportSubDirPath).toLowerCase();
	}

	private String getFacilityExportPath() {
		return new String(getFacilityPath() + "/" + EXPORT_DIR_PATH).toLowerCase();
	}

	private String getFacilityExportSubDirPath(final String inExportSubDirPath) {
		return new String(getFacilityExportPath() + "/" + inExportSubDirPath).toLowerCase();
	}

	// --------------------------------------------------------------------------
	/**
	 * Make sure all of the directories we need exist for import and export at the facility.
	 * @return
	 */
	private boolean ensureBaseDirectories(DbxClient inClient) {
		boolean result = false;

		String facilityPath = getFacilityPath();

		result = ensureDirectory(inClient, getFacilityPath());
		result &= ensureDirectory(inClient, getFacilityImportPath());
		result &= ensureDirectory(inClient, getFacilityImportSubDirPath(IMPORT_ORDERS_PATH));
		result &= ensureDirectory(inClient, getFacilityImportSubDirPath(IMPORT_BATCHES_PATH));
		result &= ensureDirectory(inClient, getFacilityImportSubDirPath(IMPORT_INVENTORY_PATH));
		result &= ensureDirectory(inClient, getFacilityImportSubDirPath(IMPORT_LOCATIONS_PATH));
		result &= ensureDirectory(inClient, getFacilityImportSubDirPath(IMPORT_SLOTTING_PATH));

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
		String result = "";

		setServiceStateEnum(EdiServiceStateEnum.LINKING);
		try {
			DropboxService.DAO.store(this);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		DbxAppInfo appInfo = new DbxAppInfo(APPKEY, APPSECRET);
		DbxRequestConfig config = new DbxRequestConfig("Codeshelf Interface", Locale.getDefault().toString());
		DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);
		result = webAuth.start();

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

			// We did get an access token.
			if (accessToken == null) {
				setServiceStateEnum(EdiServiceStateEnum.LINK_FAILED);
			} else {
				result = true;
				setProviderCredentials(accessToken);
				setServiceStateEnum(EdiServiceStateEnum.LINKED);
				setDbCursor("");
			}
			try {
				DropboxService.DAO.store(this);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		} catch (DbxException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inEntry
	 */
	private Boolean processEntry(DbxClient inClient,
		DbxDelta.Entry<DbxEntry> inEntry,
		ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationAliasImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter) {
		Boolean result = true;

		Boolean shouldUpdateEntry = false;

		if (inEntry.lcPath.startsWith(getFacilityImportPath())) {
			if (inEntry.metadata.isFile()) {
				handleImport(inClient,
					inEntry,
					inCsvOrderImporter,
					inCsvOrderLocationImporter,
					inCsvInventoryImporter,
					inCsvLocationAliasImporter,
					inCsvCrossBatchImporter);
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
		ICsvCrossBatchImporter inCsvCrossBatchImporter) {

		try {

			String filepath = inEntry.lcPath;

			//DropboxInputStream stream = inClient.getFileStream(filepath, null);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			DbxEntry.File downloadedFile = inClient.getFile(inEntry.lcPath, null, outputStream);
			InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(outputStream.toByteArray()));

			// orders-slotting needs to come before orders, because orders is a subset of the orders-slotting regex.
			if (filepath.matches(getFacilityImportSubDirPath(IMPORT_SLOTTING_PATH) + "/.*\\.csv")) {
				inCsvOrderLocationImporter.importOrderLocationsFromCsvStream(reader, getParent());
			} else if (filepath.matches(getFacilityImportSubDirPath(IMPORT_ORDERS_PATH) + "/.*\\.csv")) {
				inCsvOrderImporter.importOrdersFromCsvStream(reader, getParent());
			} else if (filepath.matches(getFacilityImportSubDirPath(IMPORT_INVENTORY_PATH) + "/.*\\.csv")) {
				inCsvInventoryImporter.importSlottedInventoryFromCsvStream(reader, getParent());
			} else if (filepath.matches(getFacilityImportSubDirPath(IMPORT_INVENTORY_PATH) + "/.*\\.csv")) {
				inCsvInventoryImporter.importDdcInventoryFromCsvStream(reader, getParent());
			} else if (filepath.matches(getFacilityImportSubDirPath(IMPORT_LOCATIONS_PATH) + "/.*\\.csv")) {
				inCsvLocationAliasImporter.importLocationAliasesFromCsvStream(reader, getParent());
			} else if (filepath.matches(getFacilityImportSubDirPath(IMPORT_BATCHES_PATH) + "/.*\\.csv")) {
				inCsvCrossBatchImporter.importCrossBatchesFromCsvStream(reader, getParent());
			}

		} catch (DbxException | IOException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inEntry
	 */
	private Boolean removeEntry(DbxClient inClient, DbxDelta.Entry<DbxEntry> inEntry) {
		Boolean result = true;

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
	public void sendWorkInstructionsToHost(final List<WorkInstruction> inWiList) {
		// Do nothing at DropBox (for now).
	}
}
