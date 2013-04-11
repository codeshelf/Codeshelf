/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DropboxService.java,v 1.34 2013/04/11 07:42:45 jeffw Exp $
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
import com.avaje.ebean.annotation.Transactional;
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
import com.gadgetworks.codeshelf.edi.ICsvImporter;
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
@Table(name = "edi_service", schema = "codeshelf")
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
	private static final String		IMPORT_PATH				= "/import";
	private static final String		EXPORT_PATH				= "/export";

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

	public final Boolean checkForCsvUpdates(ICsvImporter inCsvImporter) {
		Boolean result = false;

		// Make sure we believe that we're properly registered with the service before we try to contact it.
		if (this.getServiceStateEnum().equals(EdiServiceStateEnum.LINKED)) {

			DropboxAPI<Session> clientSession = getClientSession();
			if (clientSession != null) {
				result = checkForChangedDocuments(clientSession, inCsvImporter);
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inClientSession
	 */
	private Boolean checkForChangedDocuments(DropboxAPI<Session> inClientSession, ICsvImporter inCsvImporter) {
		Boolean result = false;

		if (ensureBaseDirectories(inClientSession)) {
			DeltaPage<Entry> page = getNextPage(inClientSession);
			while ((page != null) && (page.entries.size() > 0)) {
				result = true;
				iteratePage(inClientSession, page, inCsvImporter);
				if (page.hasMore) {
					page = getNextPage(inClientSession);
				} else {
					page = null;
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
			result = inClientSession.delta(dbCursor);
			if (result != null) {
				dbCursor = result.cursor;
				try {
					DropboxService.DAO.store(this);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
			}
		} catch (DropboxException e) {
			LOGGER.error("Dropbox session error", e);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inPath
	 * @return
	 */
	public final EdiDocumentLocator getDocumentLocatorByPath(String inPath) {
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

		String credentials = this.getProviderCredentials();
		try {
			if (credentials != null) {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode credentialsNode = mapper.readTree(credentials);
				JsonNode appKeyNode = credentialsNode.get("appToken");
				JsonNode accessTokenNode = credentialsNode.get("accessToken");

				AppKeyPair appKey = new AppKeyPair(appKeyNode.get("key").getTextValue(), appKeyNode.get("secret").getTextValue());
				AccessTokenPair accessToken = new AccessTokenPair(accessTokenNode.get("key").getTextValue(), accessTokenNode.get("secret").getTextValue());

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
	public final String getFacilityPath() {
		return FACILITY_FOLDER_PATH + getParent().getDomainId();
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final String getImportPath() {
		return getFacilityPath() + IMPORT_PATH;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final String getExportPath() {
		return getFacilityPath() + EXPORT_PATH;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean ensureBaseDirectories(DropboxAPI<Session> inClientSession) {
		boolean result = false;

		String facilityPath = FACILITY_FOLDER_PATH + getParent().getDomainId();
		String importPath = facilityPath + IMPORT_PATH;
		String exportPath = facilityPath + EXPORT_PATH;

		result = ensureDirectory(inClientSession, facilityPath);
		result &= ensureDirectory(inClientSession, importPath);
		result &= ensureDirectory(inClientSession, exportPath);

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
			this.setServiceStateEnum(EdiServiceStateEnum.LINKING);
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
				this.setServiceStateEnum(EdiServiceStateEnum.LINK_FAILED);
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

				this.setProviderCredentials(credentials);
				this.setServiceStateEnum(EdiServiceStateEnum.LINKED);
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
	 * @param inClientSession
	 * @param inPage
	 */
	private void iteratePage(DropboxAPI<Session> inClientSession, DeltaPage<Entry> inPage, ICsvImporter inCsvImporter) {
		for (DeltaEntry<Entry> entry : inPage.entries) {
			LOGGER.info(entry.lcPath);

			if (entry.metadata != null) {
				// Add, or modify.
				processEntry(inClientSession, entry, inCsvImporter);
			} else {
				removeEntry(inClientSession, entry);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inEntry
	 */
	private void processEntry(DropboxAPI<Session> inClientSession, DeltaEntry<Entry> inEntry, ICsvImporter inCsvImporter) {

		boolean shouldUpdateEntry = false;

		if (inEntry.metadata.path.startsWith(this.getImportPath())) {
			if (!inEntry.metadata.isDir) {
				handleImport(inClientSession, inEntry, inCsvImporter);
				shouldUpdateEntry = true;
			}
		} else if (inEntry.metadata.path.startsWith(this.getExportPath())) {
			if (!inEntry.metadata.isDir) {
				//handleExport();
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

				this.addEdiDocumentLocator(locator);
				try {
					EdiDocumentLocator.DAO.store(locator);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void handleImport(DropboxAPI<Session> inClientSession, DeltaEntry<Entry> inEntry, ICsvImporter inCsvImporter) {

		try {

			DropboxInputStream stream = inClientSession.getFileStream(inEntry.lcPath, null);
			InputStreamReader reader = new InputStreamReader(stream);
			if (inEntry.lcPath.toLowerCase().endsWith(".csv")) {
				if (inEntry.lcPath.contains("orders")) {
					inCsvImporter.importOrdersFromCsvStream(reader, this.getParent());
				} else if (inEntry.lcPath.contains("inventory-slotted")) {
					inCsvImporter.importSlottedInventoryFromCsvStream(reader, this.getParent());
				} else if (inEntry.lcPath.contains("inventory-ddc")) {
					inCsvImporter.importDdcInventoryFromCsvStream(reader, this.getParent());
				}
			}

		} catch (DropboxException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inEntry
	 */
	private void removeEntry(DropboxAPI<Session> inClientSession, DeltaEntry<Entry> inEntry) {
		EdiDocumentLocator locator = this.getDocumentLocatorByPath(inEntry.lcPath);
		if (locator != null) {
			try {
				EdiDocumentLocator.DAO.delete(locator);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
	}
}
