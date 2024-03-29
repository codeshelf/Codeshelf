package com.codeshelf.application;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public final class JvmProperties {
	private static Boolean loaded = null;

	/**
	 * prepare to run application by configuring system properties + logging subsystems. 
	 * should be called from static block before main() and injection.
	 * 
	 * @param appName the name of the app running (sitecontroller or server or test)  
	 */
	public static synchronized void load(String appName) {		
		if (loaded!=null) {
			return;
		}
		loaded=true;

		// set path for local log file automatically, if not configured
		System.setProperty("log.file.location", JvmProperties.getApplicationLogDirPath());
		System.setProperty("log.file.basename", appName);

		// load app configuration from various places
		loadSystemPropertiesNamed("common.config.properties");
		loadSystemPropertiesNamed(appName+".config.properties");
		
		// configure logging
		prepareLog4j2(appName);
	}

	private static void prepareLog4j2(String appName) {
		String key = "log4j.configurationFile";
		String localSpecificConfig = "local/log4j2-"+appName+".yml";
		String localGeneralConfig = "local/log4j2.yml";
		// similar to automatic log4j2 initialization behavior - log4j2-test.yml is checked first if testing
		String defaultSpecificConfig = "log4j2-"+appName+".yml";
		String defaultGeneralConfig = "log4j2.yml";
		
		if(canReadFileOrResource(localSpecificConfig)) {
			System.out.println("log4j configuration from "+localSpecificConfig);
			System.setProperty(key, localSpecificConfig);
		} else if(canReadFileOrResource(localGeneralConfig)) {
			System.out.println("log4j configuration from "+localGeneralConfig);
			System.setProperty(key, localGeneralConfig);
		} else if(canReadFileOrResource(defaultSpecificConfig)) {
			System.out.println("log4j configuration from "+defaultSpecificConfig);
			System.setProperty(key, defaultSpecificConfig);
		} else if(canReadFileOrResource(defaultGeneralConfig)) {
			System.out.println("log4j configuration from "+defaultGeneralConfig);
			System.setProperty(key, defaultGeneralConfig);
		} else {
			System.out.println("Cannot find log4j configuration, shutting down");
			System.exit(1);
		}
		
		// initialize logging + all supported APIs
		org.apache.logging.log4j.Logger log4j2_logger = org.apache.logging.log4j.LogManager.getLogger(JvmProperties.class.getName()+".log4j2");
		log4j2_logger.info("logging: log4j (v2)");
		
		org.slf4j.Logger slf4j_logger = org.slf4j.LoggerFactory.getLogger(JvmProperties.class.getName()+".slf4j");
		slf4j_logger.info("logging: slf4j");
		
		System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
		java.util.logging.Logger jul_logger = java.util.logging.Logger.getLogger(JvmProperties.class.getName()+".jul");
		jul_logger.log(java.util.logging.Level.INFO, "logging: java.util.logging");
		
		org.apache.commons.logging.Log commons_logger = org.apache.commons.logging.LogFactory.getLog(JvmProperties.class.getName()+".commons");
		commons_logger.info("logging: commons");
		
		org.apache.log4j.Logger log4j12_logger = org.apache.log4j.LogManager.getLogger(JvmProperties.class.getName()+".log4j12");
		log4j12_logger.info("logging: log4j (v1.2)");
		
		org.jboss.logging.Logger jboss_logger = org.jboss.logging.Logger.getLogger(JvmProperties.class.getName()+".jboss");
		jboss_logger.info("logging: jboss");
		
		org.eclipse.jetty.util.log.Logger jetty_logger = org.eclipse.jetty.util.log.Log.getLogger(JvmProperties.class.getName()+".jetty");
		jetty_logger.info("logging: jetty");
	}
		
	private static boolean canReadFileOrResource(String name) {
		InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
		if(stream == null) {
			try {
				stream = new FileInputStream(name);
			} catch (FileNotFoundException e) {
			}
		}
		
		return stream != null;
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
				//System.out.println("no configuration "+sourceType+"named "+configFileName);
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
				
		propertiesLoaded |= tryMergeProperties(properties,propertiesFileName,true); // base config (as resource)
		propertiesLoaded |= tryMergeProperties(properties,"/etc/codeshelf/"+propertiesFileName,false); // production standard config 

		// developer specific local config
		propertiesLoaded |= tryMergeProperties(properties,"local/"+propertiesFileName,false); 
		
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
	private static String getApplicationDataDirPath() {
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
	
		ensureFolderExists(result);
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private static String getApplicationLogDirPath() {
		String result = "";
	
		// Setup the data directory for this application.
		result = getApplicationDataDirPath() + System.getProperty("file.separator") + "logs";

		ensureFolderExists(result);
		return result;
	}

	private static Properties getVersionProperties() {
		Properties versionProps = new Properties();
		String errorMsg = null;
		try {
			URL url = ClassLoader.getSystemClassLoader().getResource("version.properties");
			
			if(url != null) {
				BufferedInputStream inStream = new BufferedInputStream(url.openStream());
				versionProps.load(inStream);
				inStream.close();
			}
		} catch (FileNotFoundException e) {
			errorMsg = "version.properties not found";
		} catch (IOException e) {
			errorMsg = "version.properties not readable";
		}
		if(errorMsg != null) {
			throw new RuntimeException(errorMsg);
		}
		return versionProps;
	}
	
	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public static String getVersionString() {
		Properties versionProps = getVersionProperties();
		String result;
		result = versionProps.getProperty("version.major");
		result += "." + versionProps.getProperty("version.revision");
		String date = versionProps.getProperty("version.date");
		if (!date.equals("none")) {
			result += " (" + versionProps.getProperty("version.branch") + "-" + versionProps.getProperty("version.commit")
					+ " " + date + ")";
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public static String getVersionStringShort() {
		Properties versionProps = getVersionProperties();
		String result;
		result = versionProps.getProperty("version.major");
		result += "." + versionProps.getProperty("version.revision");
		return result;
	}
}

