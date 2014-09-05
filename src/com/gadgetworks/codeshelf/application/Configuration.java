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
	private static Boolean commonSetupDone = null;
	private static Boolean mainConfigDone = null;

	/**
	 * set up global common app properties before loading other configuration files - may be used in tests etc 
	 * @param inUtil
	 */
	public static void commonSetup() {
		if(commonSetupDone!=null)
			return;

		commonSetupDone=true;
		String appDataDir = Util.getApplicationDataDirPath();
		System.setProperty("app.data.dir", appDataDir);
		ensureFolderExists(appDataDir);
		
		String appLogPath = Util.getApplicationLogDirPath();
		System.setProperty("app.log.dir", appLogPath);
		ensureFolderExists(appLogPath);

		loadAllConfigsNamed("common.config.properties");
	}

	/**
	 * prepare to run application by configuring system properties + logging subsystems. should be called from static block before main() and injection.
	 * @param appName the name of the app running (sitecontroller or server)  
	 */
	public static void loadConfig(String appName) {		
		if(mainConfigDone!=null)
			return;
		mainConfigDone=true;

		commonSetup();
				
		// if config.properties is defined, use that file ONLY for app configuration.
		String configFileName = System.getProperty("config.properties");
		if(configFileName != null) {
			// only load this one config from a file
			if(!tryLoadConfig(configFileName,false)) {
				System.err.println("failed to load specified configuration file "+configFileName+" - terminating");
				System.exit(1);;
			}
		} else { 
			// Otherwise load properties config file(s) from defined locations/names
			loadAllConfigsNamed(appName+".config.properties");			
		}

		// This is a slightly weird case.
		// log4j needs to find a system property for one of its file appenders (in log4j.properties, but
		// we have to compute it before we attempt to use the LogFactory.  This means it needs to 
		// be part of pre-main static initialization.
		//
		// Needs to run here before trying to use the LogFactory.
		// Guice (injector) will invoke log4j, so we need to set some log dir parameters before we call it.

		// main log file
		System.setProperty("codeshelf.logfile",
			System.getProperty("app.log.dir") + System.getProperty("file.separator") + System.getProperty("cs.logfile.name"));


		URL log4jURL = ClassLoader.getSystemClassLoader().getResource("conf/log4j.properties");
		if (log4jURL != null) {
			//			System.out.println("Log4J props file:" + log4jURL.toString());
			PropertyConfigurator.configure(log4jURL);
		}

		URL javaUtilLoggingjURL = ClassLoader.getSystemClassLoader().getResource(System.getProperty("java.util.logging.config.file"));
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

	private static void ensureFolderExists(String appFolder) {
		File appDir = new File(appFolder);
		if (!appDir.exists()) {
			try {
				appDir.mkdir();
			} catch (SecurityException e) {
				System.err.println("FATAL: Could not create folder "+appFolder);
				Util.exitSystem();
			}
		}
	}

	private static boolean tryLoadConfig(String configFileName, boolean inIsResource) {
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
				Properties properties = new Properties();
				try {
					properties.load(configFileStream);
				} catch (IOException e) {
					System.err.println("ERROR: could not load properties from "+sourceType+configFileName);
					properties = null;
				}
				if(properties != null) {
					System.out.println("loading properties from "+sourceType+configFileName);
					for (String name : properties.stringPropertyNames()) {
						String value = properties.getProperty(name);
						// LOGGER.trace("Setting "+name+" to "+value);
						String oldValue = System.getProperty(name);
						if(oldValue != null) {
							if(!oldValue.equals(value)) {
								System.out.println("configuration override: "+name+"="+value+" (was "+oldValue+") from "+sourceType+configFileName);
							}
						}
						System.setProperty(name, value);
					}
					return true;
				}
			} else {
				System.out.println("no configuration "+sourceType+" named "+configFileName);
			}
		}
		return false;
	}

	private static void loadAllConfigsNamed(String baseConfigFileName) {
		boolean propertiesLoaded = false;
		
		String userName = System.getProperty("user.name");
		String osName = System.getProperty("os.name");
		
		propertiesLoaded |= tryLoadConfig("conf/"+baseConfigFileName,true); // resource
		propertiesLoaded |= tryLoadConfig("conf/"+baseConfigFileName,false); // resource
		propertiesLoaded |= tryLoadConfig("/etc/codeshelf/"+baseConfigFileName,false); 
		propertiesLoaded |= tryLoadConfig("/etc/codeshelf/"+baseConfigFileName+"."+osName,false); 
		propertiesLoaded |= tryLoadConfig("/etc/codeshelf/"+baseConfigFileName+"."+userName,false); 
		propertiesLoaded |= tryLoadConfig("conf/"+baseConfigFileName+"."+userName,false); 
		
		if(!propertiesLoaded) {
			System.err.println("no configuration file "+baseConfigFileName+" available, terminating");
			System.exit(1);;
		}
	}

}
