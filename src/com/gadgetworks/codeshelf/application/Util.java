/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Util.java,v 1.22 2013/03/03 23:27:21 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */

public final class Util implements IUtil {

	// This is a slightly weird case.
	// log4j needs to find a system property for one of its file appenders (in log4j.properties, but
	// we have to compute it before we attempt to use the LogFactory.  This means it needs to 
	// be part of pre-main static initialization.  Since it uses methods from Util we moved it
	// here before Util tries to use the LogFactory.
	static {

		Util util = new Util();
		String appLogPath = util.getApplicationLogDirPath();
		System.setProperty("codeshelf.logfile", appLogPath + System.getProperty("file.separator") + "codeshelf.log");
		File appDir = new File(appLogPath);
		if (!appDir.exists()) {
			try {
				appDir.mkdir();
			} catch (SecurityException e) {
				e.printStackTrace();
				util.exitSystem();
			}
		}

		URL log4jURL = ClassLoader.getSystemClassLoader().getResource("conf/log4j.properties");
		if (log4jURL != null) {
			//			System.out.println("Log4J props file:" + log4jURL.toString());
			PropertyConfigurator.configure(log4jURL);
		}

		URL javaUtilLoggingjURL = ClassLoader.getSystemClassLoader().getResource("conf/logging.properties");
		if (javaUtilLoggingjURL != null) {
			try {
				java.util.logging.LogManager.getLogManager().readConfiguration(javaUtilLoggingjURL.openStream());
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static final Logger	LOGGER		= LoggerFactory.getLogger(Util.class);

	/**
	 * 
	 */
	public Util() {

	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public String getVersionString() {
		String result = "???";

		Properties versionProps = new Properties();
		try {
			URL url = ClassLoader.getSystemClassLoader().getResource("conf/version.properties");
			BufferedInputStream inStream = new BufferedInputStream(url.openStream());
			versionProps.load(inStream);
			inStream.close();
			result = versionProps.getProperty("version.major");
			result += "." + versionProps.getProperty("version.minor");
			result += "." + versionProps.getProperty("version.revision");
			result += "." + versionProps.getProperty("version.status");
		} catch (FileNotFoundException e) {
			result = "conf/version.properties not found";
		} catch (IOException e) {
			result = "conf/version.properties not readable";
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Handle ungraceful error/exit conditions.
	 */
	public void exitSystem() {
		System.exit(-1);
	}

	/**
	 * This routine exists simply so that main<init> can invoke Util<init> to setup logging.
	 */
	public static void initLogging() {

	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inURLName
	 *  @return
	 */
	public URL findResource(String inResourceName) {
		//		try {
		//			//return new URL(inURLName);
		return Util.class.getClassLoader().getResource(inResourceName);
		//		} catch (MalformedURLException e) {
		//			throw new RuntimeException("Malformed URL " + inResourceName, e);
		//		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public String getApplicationDataDirPath() {
		String result = "";

		// Setup the data directory for this application.
		result = System.getProperty("user.home");
		String platform = System.getProperty("os.name");
		if (platform.toLowerCase().indexOf("mac") > -1) {
			result += "/Library/Application Support/CodeShelf";
		} else if (platform.toLowerCase().indexOf("windows") > -1) {
			result += System.getProperty("file.separator") + "Application Data" + System.getProperty("file.separator") + "CodeShelf";
		} else if (platform.toLowerCase().indexOf("linux") > -1) {
			result += System.getProperty("file.separator") + ".CodeShelf";
		} else {
			// It's an OS we don't support (yet).
			exitSystem();
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public String getApplicationLogDirPath() {
		String result = "";

		// Setup the data directory for this application.
		result = getApplicationDataDirPath() + System.getProperty("file.separator") + "logs";

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * After we've either initialized the prefs or changed them then call this to effect the changes.
	 */
	public void setLoggingLevelsFromPrefs(Organization inOrganization, ITypedDao<PersistentProperty> inPersistentPropertyDao) {

//		PersistentProperty gwLogLvlProp = inPersistentPropertyDao.findByDomainId(inOrganization, PersistentProperty.GENERAL_INTF_LOG_LEVEL);
//		Level level = Level.toLevel(gwLogLvlProp.getCurrentValueAsStr());
//
//		// Set the root logger level.
//		Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
//		logger.setLevel(level);
//
//		// Set the GW logger level.
//		logger = (Logger) LoggerFactory.getLogger("com.gadgetworks");
//		logger.setLevel(level);
//
//		gwLogLvlProp = inPersistentPropertyDao.findByDomainId(inOrganization, PersistentProperty.GATEWAY_INTF_LOG_LEVEL);
//		level = Level.toLevel(gwLogLvlProp.getCurrentValueAsStr());
//		//Logger.getRootLogger().setLevel(level);
//		//Logger.getLogger("com.gadgetworks.codeshelf.controller").setLevel(level);
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inMode
	 *  @param inPassword
	 *  @return
	 *  @throws Exception
	 */

	public Cipher getCipher(int inMode, char[] inPassword) throws Exception {

		int saltBytes = 8;
		byte[] salt = new byte[saltBytes];
		int count = 20;

		// First let's get the salt from the .salt file.
		// NB: The salt is not secret - it's just meant to protect against dictionary attacks on the PBE algol for various keys.
		File file = new File(this.getApplicationDataDirPath() + File.separatorChar + ".salt");
		if (!file.exists()) {
			// The salt file didn't exist, so let's create one, and populate it with a new, random salt.

			// First let's create a new, random salt.
			Random randomBytes = new SecureRandom();
			randomBytes.nextBytes(salt);

			// Now store that new salt value in the file.
			try {
				file.createNewFile();
			} catch (IOException e) {
				LOGGER.error("", e);
			}
			FileOutputStream outputStream = new FileOutputStream(file);
			outputStream.write(salt, 0, saltBytes);
			outputStream.close();
		} else {
			FileInputStream inputStream = new FileInputStream(file);
			int bytesRead = inputStream.read(salt, 0, saltBytes);
			if (bytesRead == 0) {
				LOGGER.error("Unable to read salt value.");
			}
			inputStream.close();
		}

		PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
		PBEKeySpec pbeKeySpec = new PBEKeySpec(inPassword);
		SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
		SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);
		Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");

		switch (inMode) {
			case Cipher.ENCRYPT_MODE:
				pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
				break;
			case Cipher.DECRYPT_MODE:
				pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
				break;
			default:
				assert (false);
		}

		return pbeCipher;
	}
}
