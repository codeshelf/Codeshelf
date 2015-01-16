package com.gadgetworks.codeshelf.application;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

public final class Configuration {
	private static Boolean mainConfigDone = null;

	/**
	 * prepare to run application by configuring system properties + logging subsystems. should be called from static block before main() and injection.
	 * @param appName the name of the app running (sitecontroller or server)  
	 */
	public static synchronized void loadConfig(String appName) {		
		if (mainConfigDone!=null) {
			return;
		}
		mainConfigDone=true;

		String appDataDir = Configuration.getApplicationDataDirPath();
		System.setProperty("app.data.dir", appDataDir);
		ensureFolderExists(appDataDir);
		
		String appLogPath = Configuration.getApplicationLogDirPath();
		System.setProperty("app.log.dir", appLogPath);
		ensureFolderExists(appLogPath);

		loadSystemPropertiesNamed("common.config.properties");
				
		// Load properties config file(s) from defined locations/names
		loadSystemPropertiesNamed(appName+".config.properties");

		// main log file
		System.setProperty("codeshelf.logfile",
			System.getProperty("app.log.dir") + System.getProperty("file.separator") + System.getProperty("cs.logfile.name"));

		// Currently, when PropertyConfigurator.configure(URL) is called, the log4j system will initialize itself
		// searching for the DEFAULT configuration file always at "conf/log4j.properties" no matter what URL
		// we pass in. Then, it will initialize from our URL.
		//
		// This does not seem to be documented behavior, but it happens nonetheless. So here we do
		// explicitly (re)load the default file in case that behavior changes in the future.

		Properties log4jProps = loadAllPropertiesFilesNamed("log4j.properties");
		if(log4jProps!=null) {
			PropertyConfigurator.configure(log4jProps);
		} else {
			System.err.println("Failed to initialize log4j");
			//System.exit(1);	
		}

		URL javaUtilLoggingjURL = ClassLoader.getSystemClassLoader().getResource(System.getProperty("java.util.logging.config.file"));
		if (javaUtilLoggingjURL != null) {
			System.out.println("java.util.logging props file:" + javaUtilLoggingjURL.toString());
			try {
				java.util.logging.LogManager.getLogManager().readConfiguration(javaUtilLoggingjURL.openStream());
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Failed to init java.util.logging properties");
			//System.exit(1);
		}
		System.out.println("Configuration done");
	}

	private static void ensureFolderExists(String appFolder) {
		File appDir = new File(appFolder);
		if (!appDir.exists()) {
			try {
				appDir.mkdir();
			} catch (SecurityException e) {
				System.err.println("FATAL: Could not create folder "+appFolder);
				System.exit(1);
			}
		}
	}

	private static Properties tryLoadProperties(String configFileName, boolean inIsResource) {
		Properties properties=null;
		if (configFileName != null) {
			InputStream configFileStream = null;
			String sourceType;
			if(inIsResource) {
				sourceType="resource ";
				configFileStream = ClassLoader.getSystemClassLoader().getResourceAsStream(configFileName);
			} else {
				sourceType="file ";
				try {
					configFileStream = new FileInputStream(configFileName);
				} catch (FileNotFoundException e) {
				}
			}
			if (configFileStream != null) {
				properties = new Properties();
				try {
					System.out.println("loading properties from "+sourceType+configFileName);
					properties.load(configFileStream);
				} catch (IOException e) {
					System.err.println("ERROR: could not load properties from "+sourceType+configFileName);
					properties = null;
				}
			} else {
				System.out.println("no configuration "+sourceType+" named "+configFileName);
			}
		}
		return properties;
	}
	
	private static boolean tryMergeProperties(Properties properties, String configFileName, boolean inIsResource) {
		Properties p1 = tryLoadProperties(configFileName, inIsResource);
		if(p1 != null) {
			for (String name : p1.stringPropertyNames()) {
				String value = p1.getProperty(name);
				// LOGGER.trace("Setting "+name+" to "+value);
				String oldValue = properties.getProperty(name);
				if(oldValue != null) {
					if(!oldValue.equals(value)) {
						// don't print passwords and such to console, silly
						// System.out.println("configuration override: "+name+"="+value+" (was "+oldValue+") from "+configFileName);
						System.out.println("configuration override: "+name+" from "+configFileName);
					}
				}
				properties.setProperty(name, value);
			}
			return true;
		}
		return false;
	}

	private static Properties loadAllPropertiesFilesNamed(String propertiesFileName) {
		Properties properties=new Properties();
		boolean propertiesLoaded = false;
		String userName = System.getProperty("user.name");
		String osName = System.getProperty("os.name");
				
		propertiesLoaded |= tryMergeProperties(properties,"conf/"+propertiesFileName,true); // base config (as resource)
		propertiesLoaded |= tryMergeProperties(properties,propertiesFileName,true); // base config (as resource)
		//propertiesLoaded |= tryLoadConfig("conf/"+baseConfigFileName,false); // base common config (as file in dev environment)
		propertiesLoaded |= tryMergeProperties(properties,"/etc/codeshelf/"+propertiesFileName,false); // production standard config 
		propertiesLoaded |= tryMergeProperties(properties,"/etc/codeshelf/"+propertiesFileName+"."+osName,false); // os-specific
		propertiesLoaded |= tryMergeProperties(properties,"/etc/codeshelf/"+propertiesFileName+"."+userName,false); // instance customized
		propertiesLoaded |= tryMergeProperties(properties,"conf/"+propertiesFileName+"."+userName,false); // developer customized
		
		return (propertiesLoaded?properties:null);
	}
	
	private static void loadSystemPropertiesNamed(String baseConfigFileName) {
		Properties systemProps=loadAllPropertiesFilesNamed(baseConfigFileName);
		
		if(systemProps != null) {
			for (String name : systemProps.stringPropertyNames()) {
				System.setProperty(name, systemProps.getProperty(name));
			}
		} else {
			System.err.println("no configuration file "+baseConfigFileName+" available!");
			//System.exit(1);
		}
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
			System.exit(1);
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
	 *  @return
	 */
	public static String getVersionString() {
		String result = "???";
	
		Properties versionProps = new Properties();
		try {
			URL url = ClassLoader.getSystemClassLoader().getResource("conf/version.properties");
			if(url == null) {
				url = ClassLoader.getSystemClassLoader().getResource("version.properties");
			}
			if(url != null) {
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
	 *  @return
	 */
	public static String getVersionStringShort() {
		String result = "???";
	
		Properties versionProps = new Properties();
		try {
			URL url = ClassLoader.getSystemClassLoader().getResource("conf/version.properties");
			if(url == null) {
				url = ClassLoader.getSystemClassLoader().getResource("version.properties");
			}
			if(url != null) {
				BufferedInputStream inStream = new BufferedInputStream(url.openStream());
				versionProps.load(inStream);
				inStream.close();
				result = versionProps.getProperty("version.major");
				result += "." + versionProps.getProperty("version.revision");
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
	
	public static Cipher getCipher(int inMode, char[] inPassword) throws Exception {
	
		int saltBytes = 8;
		byte[] salt = new byte[saltBytes];
		int count = 20;
	
		// First let's get the salt from the .salt file.
		// NB: The salt is not secret - it's just meant to protect against dictionary attacks on the PBE algol for various keys.
		File file = new File(getApplicationDataDirPath() + File.separatorChar + ".salt");
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
	*/
}

