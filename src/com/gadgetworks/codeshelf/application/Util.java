/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Util.java,v 1.23 2013/04/11 22:47:12 jeffw Exp $
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

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */

@Singleton
public final class Util {

	/**
	 * 
	 */
	public Util() {

	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public static String getVersionString() {
		String result = "???";

		Properties versionProps = new Properties();
		try {
			URL url = ClassLoader.getSystemClassLoader().getResource("conf/version.properties");
			BufferedInputStream inStream = new BufferedInputStream(url.openStream());
			versionProps.load(inStream);
			inStream.close();
			result = versionProps.getProperty("version.major");
			//result += "." + versionProps.getProperty("version.minor");
			result += "." + versionProps.getProperty("version.revision");
			//result += "." + versionProps.getProperty("version.status");
			String date = versionProps.getProperty("version.date");
			if (!date.equals("none")) {
				result += " (" + versionProps.getProperty("version.branch") + "-" + versionProps.getProperty("version.commit")
						+ " " + date + ")";
			}
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
	public static void exitSystem() {
		System.exit(-1);
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inURLName
	 *  @return
	 */
	public static URL findResource(String inResourceName) {
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
	public static String getApplicationDataDirPath() {
		String result = "";

		// Setup the data directory for this application.
		result = System.getProperty("user.home");
		String platform = System.getProperty("os.name");
		if (platform.toLowerCase().indexOf("mac") > -1) {
			result += "/Library/Application Support/Codeshelf";
		} else if (platform.toLowerCase().indexOf("windows") > -1) {
			result += System.getProperty("file.separator") + "Application Data" + System.getProperty("file.separator")
					+ "Codeshelf";
		} else if (platform.toLowerCase().indexOf("linux") > -1) {
			result += System.getProperty("file.separator") + ".Codeshelf";
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
	public static String getApplicationLogDirPath() {
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

	public static Cipher getCipher(int inMode, char[] inPassword) throws Exception {

		int saltBytes = 8;
		byte[] salt = new byte[saltBytes];
		int count = 20;

		// First let's get the salt from the .salt file.
		// NB: The salt is not secret - it's just meant to protect against dictionary attacks on the PBE algol for various keys.
		File file = new File(Util.getApplicationDataDirPath() + File.separatorChar + ".salt");
		if (!file.exists()) {
			// The salt file didn't exist, so let's create one, and populate it with a new, random salt.

			// First let's create a new, random salt.
			Random randomBytes = new SecureRandom();
			randomBytes.nextBytes(salt);

			// Now store that new salt value in the file.
			try {
				file.createNewFile();
			} catch (IOException e) {
				System.err.println("FATAL: Unable to create salt file "+file.getName());
				Util.exitSystem();
			}
			FileOutputStream outputStream = new FileOutputStream(file);
			outputStream.write(salt, 0, saltBytes);
			outputStream.close();
		} else {
			FileInputStream inputStream = new FileInputStream(file);
			int bytesRead = inputStream.read(salt, 0, saltBytes);
			if (bytesRead == 0) {
				System.err.println("FATAL: Unable to read salt value from "+file.getName());
				Util.exitSystem();
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
