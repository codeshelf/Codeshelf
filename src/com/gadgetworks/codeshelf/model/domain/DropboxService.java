/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DropboxService.java,v 1.3 2012/09/08 23:46:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.io.IOException;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DeltaEntry;
import com.dropbox.client2.DropboxAPI.DeltaPage;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.WebAuthSession;
import com.gadgetworks.codeshelf.model.EdiDocumentStateEnum;
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
public class DropboxService extends EdiServiceABC {

	@Inject
	public static ITypedDao<DropboxService>	DAO;

	@Singleton
	public static class DropboxServiceDao extends GenericDaoABC<DropboxService> implements ITypedDao<DropboxService> {
		public final Class<DropboxService> getDaoClass() {
			return DropboxService.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(DropboxService.class);

	@Column(nullable = true)
	@Getter
	@Setter
	private String				cursor;

	public DropboxService() {

	}

	@JsonIgnore
	public final ITypedDao<DropboxService> getDao() {
		return DAO;
	}

	public final void updateDocuments() {
		// Make sure we believe that we're properly registered with the service before we try to contact it.
		if (this.getServiceStateEnum().equals(EdiServiceStateEnum.REGISTERED)) {
			if (connect()) {
				documentCheck();
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private boolean connect() {

		boolean result = false;

		try {
			String credentials = this.getProviderCredentials();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode credentialsNode = mapper.readTree(credentials);
			JsonNode appKeyNode = credentialsNode.get("appToken");
			JsonNode accessTokenNode = credentialsNode.get("accessToken");

			AppKeyPair appKey = new AppKeyPair(appKeyNode.get("key").getTextValue(), appKeyNode.get("secret").getTextValue());
			AccessTokenPair accessToken = new AccessTokenPair(accessTokenNode.get("key").getTextValue(), accessTokenNode.get("secret").getTextValue());

			WebAuthSession session = new WebAuthSession(appKey, WebAuthSession.AccessType.APP_FOLDER);
			session.setAccessTokenPair(accessToken);
			DropboxAPI<?> client = new DropboxAPI<WebAuthSession>(session);

			DropboxHelper dropboxHelper = new DropboxHelper(this);
			if (client != null) {
				result = true;
				DeltaPage<Entry> page = null;
				while ((page == null) || (page.hasMore)) {
					page = client.delta(getCursor());
					dropboxHelper.handlePage(page);
				}
			}
		} catch (JsonProcessingException e) {
			LOGGER.error("Couldn't process JSON credentials for Dropbox", e);
		} catch (IOException e) {
			LOGGER.error("Couldn't process JSON credentials for Dropbox", e);
		} catch (DropboxException e) {
			LOGGER.error("Dropbox session error", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void documentCheck() {

	}
	
	/**
	 * @author jeffw
	 * 
	 * We need this bit of nonsense since there is some weird byte-code rewrite problem between EBean, Lombok and the Dropbox API.
	 * It appears that if we call these methods when there is a java-generics local variable then part of this becomes messed up.
	 * 
	 * The results is Java VerifyError or Internal Exception 35.
	 * 
	 * We've already wasted several hours on this stupid problem.  Please don't waste more time on this unless your CERTAIN how to first fix it.
	 *
	 */
	private class DropboxHelper {
		
		private DropboxService mDropboxService;
		
		public DropboxHelper(final DropboxService inDropboxService) {
			mDropboxService = inDropboxService;
		}

		private void handlePage(DeltaPage<Entry> inPage) {
			DropboxService service = DropboxService.DAO.findByPersistentId(mDropboxService.getPersistentId());
			if ((inPage != null) && (inPage.cursor != null) && (!inPage.cursor.equals(service.getCursor()))) {
				service.setCursor(inPage.cursor);
				try {
					DropboxService.DAO.store(service);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
				iteratePage(inPage);
			}
		}

		private void iteratePage(DeltaPage<Entry> inPage) {
			for (DeltaEntry<Entry> entry : inPage.entries) {
				LOGGER.info(entry.lcPath);
				EdiDocumentLocator locator = EdiDocumentLocator.DAO.findByDomainId(mDropboxService, entry.lcPath);
				if (locator == null) {
					locator = new EdiDocumentLocator();
					locator.setParentEdiService(mDropboxService);
					locator.setReceived(new Timestamp(System.currentTimeMillis()));
					locator.setDocumentStateEnum(EdiDocumentStateEnum.NEW);
					locator.setDomainId(entry.lcPath);
					locator.setDocumentId(entry.lcPath);
					locator.setDocumentName(entry.metadata.fileName());
					try {
						EdiDocumentLocator.DAO.store(locator);
					} catch (DaoException e) {
						LOGGER.error("", e);
					}
				}
			}
		}
	}
}
