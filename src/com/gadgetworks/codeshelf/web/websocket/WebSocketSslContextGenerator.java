/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSocketSslContextGenerator.java,v 1.2 2012/11/16 08:05:56 jeffw Exp $
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

/**
 * @author jeffw
 *
 */
public class WebSocketSslContextGenerator implements IWebSocketSslContextGenerator {

	private static final String	STORETYPE		= "JKS";
	private static final String	KEYSTORE		= "codeshelf.keystore";
	private static final String	STOREPASSWORD	= "x2HPbC2avltYQR";
	private static final String	KEYPASSWORD		= "x2HPbC2avltYQR";

	private static final Log	LOGGER			= LogFactory.getLog(WebSocketSslContextGenerator.class);

	@Inject
	public WebSocketSslContextGenerator() {

	}

	public final SSLContext getSslContext() {
		SSLContext result = null;

			try {
				KeyStore ks = KeyStore.getInstance(STORETYPE);
				String keystorePath = System.getProperty(KEYSTORE);
				File file=new File(keystorePath);
				URL url = file.toURL();
				ks.load(url.openStream(), STOREPASSWORD.toCharArray());

				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
				kmf.init(ks, KEYPASSWORD.toCharArray());
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
