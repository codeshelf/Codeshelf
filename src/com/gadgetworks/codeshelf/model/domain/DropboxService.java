/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DropboxService.java,v 1.13 2012/10/01 07:16:28 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.bean.CsvToBean;
import au.com.bytecode.opencsv.bean.HeaderColumnNameMappingStrategy;

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
import com.gadgetworks.codeshelf.edi.CsvOrderImportBean;
import com.gadgetworks.codeshelf.model.EdiDocumentStatusEnum;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
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
@Table(name = "EDISERVICE")
@DiscriminatorValue("DROPBOX")
@CacheStrategy
public class DropboxService extends EdiServiceABC {

	@Inject
	public static ITypedDao<DropboxService>	DAO;

	@Singleton
	public static class DropboxServiceDao extends GenericDaoABC<DropboxService> implements ITypedDao<DropboxService> {
		public final Class<DropboxService> getDaoClass() {
			return DropboxService.class;
		}
	}

	private static final Log		LOGGER					= LogFactory.getLog(DropboxService.class);

	private static final String		APPKEY					= "feh3ontnajdmmin";
	private static final String		APPSECRET				= "4jm05vbugwnq9pe";
	private static final Integer	LINK_RETRIES			= 20;
	private static final Integer	RETRY_SECONDS			= 10 * 1000;

	private static final String		FACILITY_FOLDER_PATH	= "/FACILITY_";
	private static final String		IMPORT_PATH				= "/import";
	private static final String		EXPORT_PATH				= "/export";

	@Column(nullable = true)
	@Getter
	@Setter
	private String					cursor;

	public DropboxService() {

	}

	@JsonIgnore
	public final ITypedDao<DropboxService> getDao() {
		return DAO;
	}

	public final void updateDocuments() {
		// Make sure we believe that we're properly registered with the service before we try to contact it.
		if (this.getServiceStateEnum().equals(EdiServiceStateEnum.LINKED)) {

			DropboxAPI<Session> clientSession = getClientSession();
			if (clientSession != null) {
				documentCheck(clientSession);
			}
		}
	}

	public final EdiDocumentLocator getDocumentLocatorByPath(String inPath) {
		EdiDocumentLocator result = null;

		for (EdiDocumentLocator locator : getDocumentLocators()) {
			if (locator.getDocumentId().equals(inPath)) {
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
	public String getFacilityPath() {
		return FACILITY_FOLDER_PATH + getParent().getDomainId();
	}

	public String getImportPath() {
		return getFacilityPath() + IMPORT_PATH;
	}

	public String getExportPath() {
		return getFacilityPath() + EXPORT_PATH;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inClientSession
	 */
	private void documentCheck(DropboxAPI<Session> inClientSession) {
		if (ensureBaseDirectories(inClientSession)) {
			try {
				DropboxServiceHelper dropboxHelper = new DropboxServiceHelper(this);
				DeltaPage<Entry> page = null;
				while ((page == null) || (page.hasMore)) {
					page = inClientSession.delta(getCursor());
					dropboxHelper.processDeltas(inClientSession, page);
				}
			} catch (DropboxException e) {
				LOGGER.error("Dropbox session error", e);
			}
		}
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
				LOGGER.info(result);
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

	/**
	 * @author jeffw
	 * 
	 * We need this bit of nonsense private class since there is some weird byte-code gen problem between EBean, Lombok and the Dropbox API.
	 * It appears that if we call these methods when there is a java-generics local variable then part of this becomes messed up.
	 * 
	 * The results is Java VerifyError or Internal Exception 35.
	 * 
	 * We've already wasted several hours on this stupid problem.  Please don't waste more time on this unless you're CERTAIN how to first fix it.
	 *
	 */
	private class DropboxServiceHelper {

		private DropboxService	mDropboxService;

		public DropboxServiceHelper(final DropboxService inDropboxService) {
			mDropboxService = inDropboxService;
		}

		// --------------------------------------------------------------------------
		/**
		 * @param inClientSession
		 * @param inPage
		 */
		private void processDeltas(DropboxAPI<Session> inClientSession, DeltaPage<Entry> inPage) {
			if ((inPage != null) && (inPage.cursor != null) && (!inPage.cursor.equals(mDropboxService.getCursor()))) {
				iteratePage(inClientSession, inPage);

				mDropboxService.setCursor(inPage.cursor);
				try {
					DropboxService.DAO.store(mDropboxService);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
			}
		}

		// --------------------------------------------------------------------------
		/**
		 * @param inClientSession
		 * @param inPage
		 */
		private void iteratePage(DropboxAPI<Session> inClientSession, DeltaPage<Entry> inPage) {
			for (DeltaEntry<Entry> entry : inPage.entries) {
				LOGGER.info(entry.lcPath);

				if (entry.metadata != null) {
					// Add, or modify.
					processEntry(inClientSession, entry);
				} else {
					removeEntry(inClientSession, entry);
				}
			}
		}

		// --------------------------------------------------------------------------
		/**
		 * @param inEntry
		 */
		private void processEntry(DropboxAPI<Session> inClientSession, DeltaEntry<Entry> inEntry) {

			boolean shouldUpdateEntry = false;

			if (inEntry.metadata.path.startsWith(mDropboxService.getImportPath())) {
				if (!inEntry.metadata.isDir) {
					handleImport(inClientSession, inEntry);
					shouldUpdateEntry = true;
				}
			} else if (inEntry.metadata.path.startsWith(mDropboxService.getExportPath())) {
				if (!inEntry.metadata.isDir) {
					//handleExport();
					shouldUpdateEntry = true;
				}
			}

			if (shouldUpdateEntry) {
				EdiDocumentLocator locator = mDropboxService.getDocumentLocatorByPath(inEntry.lcPath);
				if (locator == null) {
					locator = new EdiDocumentLocator();
					locator.setParentEdiService(mDropboxService);
					locator.setDomainId(computeDefaultDomainId());
					locator.setReceived(new Timestamp(System.currentTimeMillis()));
					locator.setDocumentStateEnum(EdiDocumentStatusEnum.NEW);
					locator.setDocumentId(inEntry.lcPath);
					locator.setDocumentPath(inEntry.metadata.parentPath());
					locator.setDocumentName(inEntry.metadata.fileName());

					mDropboxService.addEdiDocumentLocator(locator);
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
		private void handleImport(DropboxAPI<Session> inClientSession, DeltaEntry<Entry> inEntry) {
			CSVReader csvReader = null;
			try {

				DropboxInputStream stream = inClientSession.getFileStream(inEntry.lcPath, null);
				InputStreamReader reader = new InputStreamReader(stream);
				csvReader = new CSVReader(reader);
				//				List<String[]> entries = csvReader.readAll();
				//				for (String[] entry : entries) {
				//					LOGGER.debug(entry);
				//				}

				HeaderColumnNameMappingStrategy<CsvOrderImportBean> strategy = new HeaderColumnNameMappingStrategy<CsvOrderImportBean>();
				strategy.setType(CsvOrderImportBean.class);
				//				String[] columns = new String[] {"orderId", "description"};
				//				strategy.setColumnMapping(columns);

				CsvToBean<CsvOrderImportBean> csv = new CsvToBean<CsvOrderImportBean>();
				List<CsvOrderImportBean> list = csv.parse(strategy, csvReader);

				for (CsvOrderImportBean importBean : list) {
					LOGGER.info(importBean);

					OrderHeader order = mDropboxService.getParentFacility().findOrder(importBean.getOrderId());

					if (order == null) {
						order = new OrderHeader();
						order.setParentFacility(getParentFacility());
						order.setDomainId(importBean.getOrderId());
						order.setOrderId(importBean.getOrderId());
						mDropboxService.getParentFacility().addOrderHeader(order);
						try {
							OrderHeader.DAO.store(order);
						} catch (DaoException e) {
							LOGGER.error("", e);
						}
					}

					OrderDetail orderDetail = order.findOrderDetail(importBean.getOrderDetailId());
					if (orderDetail == null) {
						orderDetail = new OrderDetail();
						orderDetail.setParentOrderHeader(order);
						orderDetail.setDomainId(importBean.getOrderDetailId());
						orderDetail.setDetailId(importBean.getOrderDetailId());
						order.addOrderDetail(orderDetail);
					}
					orderDetail.setItemId(importBean.getItemId());
					orderDetail.setDescription(importBean.getDescription());
					orderDetail.setQuantity(Integer.valueOf(importBean.getQuantity()));
					orderDetail.setUomId(importBean.getUomId());
					orderDetail.setOrderDate(Timestamp.valueOf(importBean.getOrderDate()));
					
					try {
						OrderDetail.DAO.store(orderDetail);
					} catch (DaoException e) {
						LOGGER.error("", e);
					}
				}

				csvReader.close();
			} catch (FileNotFoundException e) {
				LOGGER.error("", e);
			} catch (IOException e) {
				LOGGER.error("", e);
			} catch (DropboxException e) {
				LOGGER.error("", e);
			}
		}

		// --------------------------------------------------------------------------
		/**
		 * @param inEntry
		 */
		private void removeEntry(DropboxAPI<Session> inClientSession, DeltaEntry<Entry> inEntry) {
			EdiDocumentLocator locator = mDropboxService.getDocumentLocatorByPath(inEntry.lcPath);
			if (locator != null) {
				try {
					EdiDocumentLocator.DAO.delete(locator);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
			}
		}
	}
}
