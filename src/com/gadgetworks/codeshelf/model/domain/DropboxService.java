/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DropboxService.java,v 1.1 2012/09/08 03:03:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.io.IOException;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DeltaEntry;
import com.dropbox.client2.DropboxAPI.DeltaPage;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.WebAuthSession;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;

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

	//	@Inject
	//	public static ITypedDao<DropboxService>	DAO;
	//
	//	@Singleton
	//	public static class DropboxServiceDao extends GenericDaoABC<DropboxService> implements ITypedDao<DropboxService> {
	//		public final Class<DropboxService> getDaoClass() {
	//			return DropboxService.class;
	//		}
	//	}
	//
	private static final Log	LOGGER	= LogFactory.getLog(DropboxService.class);

	public DropboxService() {

	}

	//	@JsonIgnore
	//	public final ITypedDao<DropboxService> getDao() {
	//		return DAO;
	//	}

	@Override
	public void updateDocuments() {
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

		Boolean result = true;

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

			DeltaPage<Entry> page = client.delta(null);
			for (DeltaEntry<Entry> entry : page.entries) {
				LOGGER.info(entry.lcPath);
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
}
