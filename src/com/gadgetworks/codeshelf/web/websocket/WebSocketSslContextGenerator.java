/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSocketSslContextGenerator.java,v 1.3 2012/11/19 10:48:25 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author jeffw
 *
 */
public class WebSocketSslContextGenerator implements IWebSocketSslContextGenerator {

	private static final Log	LOGGER			= LogFactory.getLog(WebSocketSslContextGenerator.class);

	private String				mKeystorePath;
	private String				mKeystoreType;
	private String				mKeystoreStorePassword;
	private String				mKeystoreKeyPassword;

	@Inject
	public WebSocketSslContextGenerator(@Named(KEYSTORE_PATH_PROPERTY) final String inKeystorePath,
		@Named(KEYSTORE_TYPE_PROPERTY) final String inKeystoreType,
		@Named(KEYSTORE_STORE_PASSWORD_PROPERTY) final String inKeystoreStorePassword,
		@Named(KEYSTORE_KEY_PASSWORD_PROPERTY) final String inKeystoreKeyPassword) {
		mKeystorePath = inKeystorePath;
		mKeystoreType = inKeystoreType;
		mKeystoreStorePassword = inKeystoreStorePassword;
		mKeystoreKeyPassword = inKeystoreKeyPassword;
	}

	public final SSLContext getSslContext() {
		SSLContext result = null;

		try {
			KeyStore ks = KeyStore.getInstance(mKeystoreType);
			File file = new File(mKeystorePath);
			URL url = file.toURL();
			ks.load(url.openStream(), mKeystoreStorePassword.toCharArray());

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, mKeystoreKeyPassword.toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(ks);

			result = SSLContext.getInstance("TLSv1.2");
			result.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		} catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			LOGGER.error("", e);
		}

		return result;
	}

}
