/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DropboxService.java,v 1.37 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DeltaEntry;
import com.dropbox.client2.DropboxAPI.DeltaPage;
import com.dropbox.client2.DropboxAPI.DropboxInputStream;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;
import com.dropbox.client2.session.WebAuthSession;
import com.gadgetworks.codeshelf.edi.ICsvInventoryImporter;
import com.gadgetworks.codeshelf.edi.ICsvLocationAliasImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderLocationImporter;
import com.gadgetworks.codeshelf.edi.ICsvCrossBatchImporter;
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
@CacheStrategy
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

	private static final String		APPKEY					= "feh3ontnajdmmin";
	private static final String		APPSECRET				= "4jm05vbugwnq9pe";
	private static final Integer	LINK_RETRIES			= 20;
	private static final Integer	RETRY_SECONDS			= 10 * 1000;

	private static final String		FACILITY_FOLDER_PATH	= "/FACILITY_";

	private static final String		IMPORT_DIR_PATH			= "import/";
	private static final String		IMPORT_ORDERS_PATH		= "orders/";
	private static final String		IMPORT_BATCHES_PATH		= "batches/";
	private static final String		IMPORT_INVENTORY_PATH	= "inventory/";
	private static final String		IMPORT_LOCATIONS_PATH	= "locations/";

	private static final String		EXPORT_DIR_PATH			= "export/";
	private static final String		EXPORT_WIS_PATH			= "work/";

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

	public final Boolean checkForCsvUpdates(ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationAliasImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter) {
		Boolean result = false;

		// Make sure we believe that we're properly registered with the service before we try to contact it.
		if (getServiceStateEnum().equals(EdiServiceStateEnum.LINKED)) {

			DropboxAPI<Session> clientSession = getClientSession();
			if (clientSession != null) {
				result = checkForChangedDocuments(clientSession,
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
	private Boolean checkForChangedDocuments(DropboxAPI<Session> inClientSession,
		ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationAliasImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter) {
		Boolean result = false;

		if (ensureBaseDirectories(inClientSession)) {
			DeltaPage<Entry> page = getNextPage(inClientSession);
			while ((page != null) && (page.entries.size() > 0)) {
				// Signal that we got some deltas
				result = true;
				if (iteratePage(inClientSession,
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
						page = getNextPage(inClientSession);
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
	private DeltaPage<Entry> getNextPage(DropboxAPI<Session> inClientSession) {
		DeltaPage<Entry> result = null;
		try {
			result = inClientSession.delta(getDbCursor());
			if (result != null) {
				setDbCursor(result.cursor);
			}
		} catch (DropboxException e) {
			LOGGER.error("Dropbox session error", e);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inClientSession
	 * @param inPage
	 */
	private Boolean iteratePage(DropboxAPI<Session> inClientSession,
		DeltaPage<Entry> inPage,
		ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationAliasImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter) {
		Boolean result = true;

		for (DeltaEntry<Entry> entry : inPage.entries) {
			LOGGER.info(entry.lcPath);
			try {
				if (entry.metadata != null) {
					// Add, or modify.
					result &= processEntry(inClientSession,
						entry,
						inCsvOrderImporter,
						inCsvOrderLocationImporter,
						inCsvInventoryImporter,
						inCsvLocationAliasImporter,
						inCsvCrossBatchImporter);
				} else {
					result &= removeEntry(inClientSession, entry);
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
	private DropboxAPI<Session> getClientSession() {

		DropboxAPI<Session> result = null;

		String credentials = getProviderCredentials();
		try {
			if (credentials != null) {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode credentialsNode = mapper.readTree(credentials);
				JsonNode appKeyNode = credentialsNode.get("appToken");
				JsonNode accessTokenNode = credentialsNode.get("accessToken");

				AppKeyPair appKey = new AppKeyPair(appKeyNode.get("key").getTextValue(), appKeyNode.get("secret").getTextValue());
				AccessTokenPair accessToken = new AccessTokenPair(accessTokenNode.get("key").getTextValue(),
					accessTokenNode.get("secret").getTextValue());

				WebAuthSession session = new WebAuthSession(appKey, WebAuthSession.AccessType.APP_FOLDER);
				session.setAccessTokenPair(accessToken);
				result = new DropboxAPI<Session>(session);
			}
		} catch (JsonProcessingException e) {
			LOGGER.error("Couldn't process JSON credentials for Dropbox", e);
		} catch (IOException e) {
			LOGGER.error("Couldn't process JSON credentials for Dropbox", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private String getFacilityPath() {
		return new String(FACILITY_FOLDER_PATH + getParent().getDomainId() + "/").toLowerCase();
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private String getFacilityImportPath() {
		return new String(FACILITY_FOLDER_PATH + getParent().getDomainId() + "/" + IMPORT_DIR_PATH).toLowerCase();
	}

	// --------------------------------------------------------------------------
	/**
	 * Make sure all of the directories we need exist for import and export at the facility.
	 * @return
	 */
	private boolean ensureBaseDirectories(DropboxAPI<Session> inClientSession) {
		boolean result = false;

		String facilityPath = getFacilityPath();

		result = ensureDirectory(inClientSession, facilityPath);
		result &= ensureDirectory(inClientSession, facilityPath + IMPORT_DIR_PATH);
		result &= ensureDirectory(inClientSession, facilityPath + IMPORT_DIR_PATH + IMPORT_ORDERS_PATH);
		result &= ensureDirectory(inClientSession, facilityPath + IMPORT_DIR_PATH + IMPORT_BATCHES_PATH);
		result &= ensureDirectory(inClientSession, facilityPath + IMPORT_DIR_PATH + IMPORT_INVENTORY_PATH);
		result &= ensureDirectory(inClientSession, facilityPath + IMPORT_DIR_PATH + IMPORT_LOCATIONS_PATH);

		result &= ensureDirectory(inClientSession, facilityPath + EXPORT_DIR_PATH);
		result &= ensureDirectory(inClientSession, facilityPath + EXPORT_DIR_PATH + EXPORT_WIS_PATH);

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inClientSession
	 * @param inPath
	 * @return
	 */
	private boolean ensureDirectory(DropboxAPI<Session> inClientSession, String inPath) {
		boolean result = false;

		try {
			Entry dirEntry = inClientSession.metadata(inPath, 1, null, false, null);
			if ((dirEntry == null) || (dirEntry.isDeleted)) {
				result = createDirectory(inClientSession, inPath);
			} else {
				result = true;
			}
		} catch (DropboxServerException e) {
			if (e.error == DropboxServerException._404_NOT_FOUND) {
				result = createDirectory(inClientSession, inPath);
			}
		} catch (DropboxException e) {
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
	private boolean createDirectory(DropboxAPI<Session> inClientSession, String inPath) {
		boolean result = false;

		try {
			Entry dirEntry = inClientSession.createFolder(inPath);
			if ((dirEntry != null) && (dirEntry.isDir)) {
				result = true;
			}
		} catch (DropboxException e) {
			LOGGER.error("Dropbox session error", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final String link() {
		String result = "";

		try {
			setServiceStateEnum(EdiServiceStateEnum.LINKING);
			try {
				DropboxService.DAO.store(this);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}

			AppKeyPair appKeyPair = new AppKeyPair(APPKEY, APPSECRET);
			final WebAuthSession authSession = new WebAuthSession(appKeyPair, Session.AccessType.APP_FOLDER);

			// Make the user log in and authorize us.
			final WebAuthSession.WebAuthInfo authInfo = authSession.getAuthInfo();
			result = authInfo.url;

			Runnable runnable = new Runnable() {
				public void run() {
					tryCredentials(authSession, authInfo);
				}
			};

			Thread linkThread = new Thread(runnable);
			linkThread.start();

		} catch (DropboxException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inAuthSession
	 * @param inAuthInfo
	 */
	private void tryCredentials(final WebAuthSession inAuthSession, final WebAuthSession.WebAuthInfo inAuthInfo) {

		String credentials = "";

		try {
			AccessTokenPair accessToken = getAccessTokens(inAuthSession, inAuthInfo);

			// We got an access token.
			if (accessToken == null) {
				setServiceStateEnum(EdiServiceStateEnum.LINK_FAILED);
				try {
					DropboxService.DAO.store(this);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
			} else {
				ObjectMapper mapper = new ObjectMapper();
				ObjectNode credentialsNode = mapper.createObjectNode();
				ObjectNode appNode = credentialsNode.putObject("appToken");
				appNode.put("key", "feh3ontnajdmmin");
				appNode.put("secret", "4jm05vbugwnq9pe");
				ObjectNode accessNode = credentialsNode.putObject("accessToken");
				accessNode.put("key", accessToken.key);
				accessNode.put("secret", accessToken.secret);

				StringWriter sw = new StringWriter();
				mapper.writeValue(sw, credentialsNode);
				credentials = sw.toString();

				setProviderCredentials(credentials);
				setServiceStateEnum(EdiServiceStateEnum.LINKED);
				setDbCursor("");
				try {
					DropboxService.DAO.store(this);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
			}
		} catch (JsonGenerationException e) {
			LOGGER.error("", e);
		} catch (JsonMappingException e) {
			LOGGER.error("", e);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inAuthSession
	 * @param inAuthInfo
	 * @return
	 */
	private AccessTokenPair getAccessTokens(final WebAuthSession inAuthSession, final WebAuthSession.WebAuthInfo inAuthInfo) {
		AccessTokenPair result = null;

		// Try LINK_RETRIES times for RETRY_SECONDS each and then give up.
		int retries = 0;
		while ((result == null) && (retries < LINK_RETRIES)) {
			try {
				inAuthSession.retrieveWebAccessToken(inAuthInfo.requestTokenPair);
				result = inAuthSession.getAccessTokenPair();
				LOGGER.info(result.toString());
			} catch (DropboxException e) {
				LOGGER.error("", e);
				retries++;
				try {
					Thread.sleep(RETRY_SECONDS);
				} catch (InterruptedException e1) {
					LOGGER.error("", e);
				}
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inEntry
	 */
	private Boolean processEntry(DropboxAPI<Session> inClientSession,
		DeltaEntry<Entry> inEntry,
		ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationAliasImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter) {
		Boolean result = true;

		Boolean shouldUpdateEntry = false;

		if (inEntry.lcPath.startsWith(getFacilityImportPath())) {
			if (!inEntry.metadata.isDir) {
				handleImport(inClientSession,
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
				locator.setDocumentPath(inEntry.metadata.parentPath());
				locator.setDocumentName(inEntry.metadata.fileName());

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
	private void handleImport(DropboxAPI<Session> inClientSession,
		DeltaEntry<Entry> inEntry,
		ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationAliasImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter) {

		try {

			String filepath = inEntry.lcPath;

			DropboxInputStream stream = inClientSession.getFileStream(filepath, null);
			InputStreamReader reader = new InputStreamReader(stream);

			// orders-slotting needs to come before orders, because orders is a subset of the orders-slotting regex.
			if (filepath.matches(getFacilityImportPath() + IMPORT_ORDERS_PATH + ".*orders-slotting.*\\.csv")) {
				inCsvOrderLocationImporter.importOrderLocationsFromCsvStream(reader, getParent());
			} else if (filepath.matches(getFacilityImportPath() + IMPORT_ORDERS_PATH + ".*orders.*\\.csv")) {
				inCsvOrderImporter.importOrdersFromCsvStream(reader, getParent());
			} else if (filepath.matches(getFacilityImportPath() + IMPORT_INVENTORY_PATH + ".*inventory-slotted.*\\.csv")) {
				inCsvInventoryImporter.importSlottedInventoryFromCsvStream(reader, getParent());
			} else if (filepath.matches(getFacilityImportPath() + IMPORT_INVENTORY_PATH + ".*inventory-ddc.*\\.csv")) {
				inCsvInventoryImporter.importDdcInventoryFromCsvStream(reader, getParent());
			} else if (filepath.matches(getFacilityImportPath() + IMPORT_LOCATIONS_PATH + ".*location-aliases.*\\.csv")) {
				inCsvLocationAliasImporter.importLocationAliasesFromCsvStream(reader, getParent());
			} else if (filepath.matches(getFacilityImportPath() + IMPORT_BATCHES_PATH + ".*cross-batch.*\\.csv")) {
				inCsvCrossBatchImporter.importCrossBatchesFromCsvStream(reader, getParent());
			}

		} catch (DropboxException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inEntry
	 */
	private Boolean removeEntry(DropboxAPI<Session> inClientSession, DeltaEntry<Entry> inEntry) {
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
}
