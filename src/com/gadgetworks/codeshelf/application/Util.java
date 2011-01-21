/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Util.java,v 1.3 2011/01/21 04:25:54 jeffw Exp $
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.gadgetworks.codeshelf.model.dao.ISystemDAO;
import com.gadgetworks.codeshelf.model.dao.SystemDAO;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty;
import com.gadgetworks.codeshelf.ui.AboutBox;
import com.gadgetworks.codeshelf.ui.Console;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */

public final class Util {

	public static final String			DAO_BEAN_ID							= "systemDAO";

	public static final String			CODESHELF_ABOUT_ICON				= "codeshelf_about_icon";

	public static final String			DEBUG_ICON							= "debug";
	public static final String			PERSON_ICON							= "person";
	public static final String			PERSON_ICON_QUARTER_ALPHA			= "person-fourth";
	public static final String			PERSON_ICON_RED						= "person-yellow";
	public static final String			PERSON_ICON_YELLOW					= "person-yellow";
	public static final String			PERSON_ICON_GREEN					= "person-yellow";
	public static final String			ACCOUNT_ICON						= "account";
	public static final String			ACCOUNT_ICON_QUARTER_ALPHA			= "account-fourth";
	public static final String			ACCOUNT_ICON_RED					= "account-red";
	public static final String			ACCOUNT_ICON_YELLOW					= "account-yellow";
	public static final String			ACCOUNT_ICON_GREEN					= "account-green";
	public static final String			WIRELESS_DEVICE_ICON				= "radio";
	public static final String			WIRELESS_DEVICE_ICON_QUARTER_ALPHA	= "radio-fourth";

	public static final String			SYSTEM_TRAY_ICON_CLEAR				= "system-tray-icon-clear";
	public static final String			SYSTEM_TRAY_ICON_UPDATES			= "system-tray-icon-updates";

	public static final String			TWITTER_ICON						= "twitter";
	public static final String			FACEBOOK_SMALL_ICON					= "facebook-small";
	public static final String			FACEBOOK_BIG_ICON					= "facebook-big";

	public static final String			DPF_ICON							= "dpf";
	public static final String			SETTINGS_ICON						= "settings";
	public static final String			LOCK_SMALL_ICON						= "lock-small";
	public static final String			LOCK_BIG_ICON						= "lock-big";
	public static final String			REDX_SMALL_ICON						= "redx-small";
	public static final String			REDX_BIG_ICON						= "redx-big";
	public static final String			CHECK_RED_ICON						= "check-red";
	public static final String			CHECK_GREEN_ICON					= "check-green";

	public static final String			UP_BUTTON_ICON						= "up_button";
	public static final String			DOWN_BUTTON_ICON					= "down_button";
	public static final String			STOP_BUTTON_ICON					= "stop_button";
	public static final String			TALK_BUTTON_ICON					= "talk_butuon";

	public static final String			START_TOOLBAR_ITEM					= "start";
	public static final String			ADD_TOOLBAR_ITEM					= "add";
	public static final String			DELETE_TOOLBAR_ITEM					= "delete";

	public static final String			LIGHT_ORANGE_COLOR					= "lt orange";
	public static final String			LIGHT_GREY_COLOR					= "lt grey";
	public static final String			ABOUT_BGND_COLOR					= "about-b";
	public static final String			ABOUT_FGND_COLOR					= "about-f";
	public static final String			CHATITEM_IN_COLOR					= "chatitemin";
	public static final String			CHATITEM_OUT_COLOR					= "chatitemout";
	public static final String			BACKGROUND_COLOR					= "background";

	public static final int				ALPHA_LEVEL_QUARTER					= 64;
	public static final int				ALPHA_LEVEL_HALF					= 128;
	public static final int				ALPHA_LEVEL_FULL					= 256;

	public static final int				DEFAULT_FONT_SIZE					= 12;
	public static final int				SESSIONBOX_FONT_SIZE				= 11;
	public static final String			DEFAULT_FONT_NAME					= "Lucida Grande";
	public static final String			SESSIONBOX_FONT_NAME				= "Lucida Grande";
	public static final String			CONSOLE_FONT_NAME					= "Courier New";

	public static final String			CONSOLE_TEXT						= "consoleText";
	public static final String			OWNERBOX_TEXT						= "personBoxText";
	public static final String			DATOBLOKVIEW_TEXT					= "datoBlokViewText";
	public static final String			WIRELESSDEVICEBOX_TEXT				= "wirelessdeviceBoxText";

	private static final String			KEEP_FORWARD_THREAD_NAME			= "Keep Dialog Forward";

	private static ImageRegistry		mImageRegistry;
	private static FontRegistry			mFontRegistry;
	private static ColorRegistry		mColorRegistry;
	private static volatile Clipboard	mClipboard;
	//private static ApplicationContext	mAppContext					= new ClassPathXmlApplicationContext(new String[] { "CodeShelfContext.xml" });
	//private static ISystemDAO			mSystemDAO					= (ISystemDAO) mAppContext.getBean(APP_BEAN_ID);
	//private static ISystemDAO		mSystemDAO						= (ISystemDAO) SystemBeanFactory.getBean(DAO_BEAN_ID);
	private static volatile ISystemDAO	mSystemDAO;
	private static Console				mConsole;

	private static boolean				isLinux								= SWT.getPlatform().equalsIgnoreCase("gtk");
	private static boolean				isOSX								= (SWT.getPlatform().equalsIgnoreCase("carbon"))
																					|| (SWT.getPlatform().equalsIgnoreCase("cocoa"));
	private static boolean				isSolaris							= SWT.getPlatform().equalsIgnoreCase("motif");
	private static boolean				isWindows							= SWT.getPlatform().equalsIgnoreCase("win32");

	// This is a slightly weird case.
	// log4j needs to find a system property for one of its file appenders (in log4j.properties, but
	// we have to compute it before we attempt to use the LogFactory.  This means it needs to 
	// be part of pre-main static initialization.  Since it uses methods from Util we moved it
	// here before Util tries to use the LogFactory.
	static {
		String appLogPath = getApplicationLogDirPath();
		System.setProperty("codeshelf.logfile", appLogPath + System.getProperty("file.separator") + "codeshelf.log");
		File appDir = new File(appLogPath);
		if (!appDir.exists()) {
			try {
				appDir.mkdir();
			} catch (SecurityException e) {
				e.printStackTrace();
				exitSystem();
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

	private static final Log			LOGGER								= LogFactory.getLog(Util.class);

	// --------------------------------------------------------------------------
	/**
	 * Default constructor hidden for static class.
	 */
	private Util() {

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
	public static void exitSystem() {
		System.exit(-1);
	}

	/**
	 * This routine exists simply so that main<init> can invoke Util<init> to setup logging.
	 */
	public static void initLogging() {

	}

	// --------------------------------------------------------------------------
	/**
	 * Determine the underlying OS.
	 */
	public static boolean isOSX() {
		return isOSX;
	}

	public static boolean isWindows() {
		return isWindows;
	}

	public static boolean isLinux() {
		return isLinux;
	}

	public static boolean isSolaris() {
		return isSolaris;
	}

	// --------------------------------------------------------------------------
	/**
	 *  Start the console at application startup.
	 *  Only do this once.
	 *  @param inShell
	 */
	public static synchronized void startConsole(Shell inShell, CodeShelfApplication inCodeShelfApplication) {
		if (mConsole == null) {
			mConsole = new Console(inShell, inCodeShelfApplication);
		} else {
			LOGGER.error("Console already started - only do this once.");
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void stopConsole() {
		if (mConsole != null)
			mConsole.stopConsole();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void openConsole() {
		if (mConsole != null)
			mConsole.openConsole();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void closeConsole() {
		if (mConsole != null)
			mConsole.closeConsole();
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public static Console getConsole() {
		return mConsole;
	}

	// --------------------------------------------------------------------------
	/**
	 *  Show the about box.
	 *  @return
	 */
	public static int showAbout() {
		Shell shell = new Shell();
		AboutBox about = new AboutBox(shell.getDisplay());
		about.aboutOpen();
		about.aboutClose();
		shell.dispose();
		return 0; //OS.noErr;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDialog
	 */
	public static void keepDialogForward(final Dialog inDialog) {
		// This little bit of weirdness brings the dialog forward every two seconds since we are a background application.

		final Display display = Display.getCurrent();

		Thread keepForwardThread = new Thread(KEEP_FORWARD_THREAD_NAME) {
			public void run() {
				// Give the dialog 1 second to open.
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					LOGGER.error("", e);
				}
				while ((inDialog.getShell() != null) && (!inDialog.getShell().isDisposed())) {
					display.asyncExec(new Runnable() {
						public void run() {
							Shell shell = inDialog.getShell();
							if ((shell != null) && (!shell.isDisposed())) {
								shell.forceActive();
							}
						};
					});

					// Wait 2 seconds before trying to bring it forward again.
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						LOGGER.error("", e);
					}
				}
			}
		};
		keepForwardThread.start();
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
	public static synchronized ImageRegistry getImageRegistry() {

		if (Display.getCurrent() != null) {
			if (mImageRegistry == null) {
				mImageRegistry = new ImageRegistry();

				// Images
				mImageRegistry.put(CODESHELF_ABOUT_ICON, ImageDescriptor.createFromURL(findResource("images/codeshelf_about.png")));

				mImageRegistry.put(DEBUG_ICON, ImageDescriptor.createFromURL(findResource("images/debug.gif")));

				mImageRegistry.put(PERSON_ICON, ImageDescriptor.createFromURL(findResource("images/person.png")));
				mImageRegistry.put(PERSON_ICON_RED, ImageDescriptor.createFromURL(findResource("images/person_red.png")));
				mImageRegistry.put(PERSON_ICON_YELLOW, ImageDescriptor.createFromURL(findResource("images/person_yellow.png")));
				mImageRegistry.put(PERSON_ICON_GREEN, ImageDescriptor.createFromURL(findResource("images/person_green.png")));

				mImageRegistry.put(ACCOUNT_ICON, ImageDescriptor.createFromURL(findResource("images/account.png")));
				mImageRegistry.put(ACCOUNT_ICON_RED, ImageDescriptor.createFromURL(findResource("images/account_red.png")));
				mImageRegistry.put(ACCOUNT_ICON_YELLOW, ImageDescriptor.createFromURL(findResource("images/account_yellow.png")));
				mImageRegistry.put(ACCOUNT_ICON_GREEN, ImageDescriptor.createFromURL(findResource("images/account_green.png")));

				mImageRegistry.put(WIRELESS_DEVICE_ICON, ImageDescriptor.createFromURL(findResource("images/account.tiff")));

				mImageRegistry.put(TWITTER_ICON, ImageDescriptor.createFromURL(findResource("images/twitter.png")));
				mImageRegistry.put(FACEBOOK_SMALL_ICON, ImageDescriptor.createFromURL(findResource("images/facebook_small.png")));
				mImageRegistry.put(FACEBOOK_BIG_ICON, ImageDescriptor.createFromURL(findResource("images/facebook_big.png")));
				mImageRegistry.put(SYSTEM_TRAY_ICON_CLEAR,
					ImageDescriptor.createFromURL(findResource("images/system_tray_icon_clear.png")));
				mImageRegistry.put(SYSTEM_TRAY_ICON_UPDATES,
					ImageDescriptor.createFromURL(findResource("images/system_tray_icon_updates.png")));

				mImageRegistry.put(DPF_ICON, ImageDescriptor.createFromURL(findResource("images/dpf.png")));
				mImageRegistry.put(SETTINGS_ICON, ImageDescriptor.createFromURL(findResource("images/settings.png")));
				mImageRegistry.put(LOCK_SMALL_ICON, ImageDescriptor.createFromURL(findResource("images/lock_small.png")));
				mImageRegistry.put(LOCK_BIG_ICON, ImageDescriptor.createFromURL(findResource("images/lock_big.png")));
				mImageRegistry.put(REDX_SMALL_ICON, ImageDescriptor.createFromURL(findResource("images/x_red_small.png")));
				mImageRegistry.put(REDX_BIG_ICON, ImageDescriptor.createFromURL(findResource("images/x_red_big.png")));
				mImageRegistry.put(CHECK_RED_ICON, ImageDescriptor.createFromURL(findResource("images/check_red_32x32.png")));
				mImageRegistry.put(CHECK_GREEN_ICON, ImageDescriptor.createFromURL(findResource("images/check_green_32x32.png")));

				mImageRegistry.put(UP_BUTTON_ICON, ImageDescriptor.createFromURL(findResource("images/button_up.gif")));
				mImageRegistry.put(DOWN_BUTTON_ICON, ImageDescriptor.createFromURL(findResource("images/button_down.gif")));
				mImageRegistry.put(STOP_BUTTON_ICON, ImageDescriptor.createFromURL(findResource("images/button_stop.gif")));
				mImageRegistry.put(TALK_BUTTON_ICON, ImageDescriptor.createFromURL(findResource("images/button_talk.gif")));

				ImageData imageData = mImageRegistry.getDescriptor(PERSON_ICON).getImageData();
				imageData.alpha = Util.ALPHA_LEVEL_QUARTER;
				mImageRegistry.put(PERSON_ICON_QUARTER_ALPHA, new Image(Display.getDefault(), imageData));

				imageData = mImageRegistry.getDescriptor(ACCOUNT_ICON).getImageData();
				imageData.alpha = Util.ALPHA_LEVEL_QUARTER;
				mImageRegistry.put(ACCOUNT_ICON_QUARTER_ALPHA, new Image(Display.getDefault(), imageData));

				imageData = mImageRegistry.getDescriptor(WIRELESS_DEVICE_ICON).getImageData();
				imageData.alpha = Util.ALPHA_LEVEL_QUARTER;
				mImageRegistry.put(WIRELESS_DEVICE_ICON_QUARTER_ALPHA, new Image(Display.getDefault(), imageData));

				// Toolbar items
				mImageRegistry.put(START_TOOLBAR_ITEM, ImageDescriptor.createFromURL(findResource("images/start.png")));
				mImageRegistry.put(ADD_TOOLBAR_ITEM, ImageDescriptor.createFromURL(findResource("images/add.png")));
				mImageRegistry.put(DELETE_TOOLBAR_ITEM, ImageDescriptor.createFromURL(findResource("images/delete.png")));

			}
		}
		return mImageRegistry;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public static synchronized FontRegistry getFontRegistry() {
		if ((Display.getCurrent() != null) && (mFontRegistry == null)) {
			//				FontData[] fontData = Display.getCurrent().getFontList(null, true);
			//				LOGGER.debug(fontData);
			mFontRegistry = new FontRegistry();
			mFontRegistry.put(CONSOLE_TEXT, new FontData[] { new FontData(CONSOLE_FONT_NAME, DEFAULT_FONT_SIZE, SWT.NORMAL) });
			mFontRegistry.put(OWNERBOX_TEXT, new FontData[] { new FontData(DEFAULT_FONT_NAME, DEFAULT_FONT_SIZE, SWT.NORMAL) });
			mFontRegistry.put(WIRELESSDEVICEBOX_TEXT, new FontData[] { new FontData(DEFAULT_FONT_NAME,
				DEFAULT_FONT_SIZE,
				SWT.NORMAL) });
		}

		return mFontRegistry;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public static synchronized ColorRegistry getColorRegistry() {

		if (Display.getCurrent() != null) {
			if (mColorRegistry == null) {
				mColorRegistry = new ColorRegistry();
				mColorRegistry.put(LIGHT_ORANGE_COLOR, new RGB(255, 200, 80));
				mColorRegistry.put(LIGHT_GREY_COLOR, new RGB(240, 240, 240));
				mColorRegistry.put(ABOUT_BGND_COLOR, new RGB(5, 80, 240));
				mColorRegistry.put(ABOUT_FGND_COLOR, new RGB(250, 202, 56));
				mColorRegistry.put(CHATITEM_IN_COLOR, new RGB(220, 240, 240));
				mColorRegistry.put(CHATITEM_OUT_COLOR, new RGB(240, 220, 240));
				mColorRegistry.put(BACKGROUND_COLOR, new RGB(245, 245, 250));
			}
		}
		return mColorRegistry;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public static Clipboard getClipboard() {
		if (mClipboard == null) {
			mClipboard = new Clipboard(Display.getCurrent());
		}

		return mClipboard;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public static String getApplicationDataDirPath() {
		String result = "";

		// Setup the data directory for this application.
		result = System.getProperty("user.home");
		if (isOSX) {
			result += "/Library/Application Support/CodeShelf";
		} else if (isWindows) {
			result += System.getProperty("file.separator") + "Application Data" + System.getProperty("file.separator")
					+ "CodeShelf";
		} else {
			result += System.getProperty("file.separator") + ".CodeShelf";
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public static String getApplicationInitDatabaseURL() {
		String result = "";

		// Setup the data directory for this application.
		result = "jdbc:h2:" + getApplicationDataDirPath() + System.getProperty("file.separator") + "db"
				+ System.getProperty("file.separator") + "database" + ";TRACE_LEVEL_FILE=0";

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public static String getApplicationDatabaseURL() {
		String result = "";

		// Setup the data directory for this application.
		result = "jdbc:h2:" + getApplicationDataDirPath() + System.getProperty("file.separator") + "db"
				+ System.getProperty("file.separator") + "database" + ";SCHEMA=CODESHELF;TRACE_LEVEL_FILE=0";

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
	public static ISystemDAO getSystemDAO() {
		if (mSystemDAO == null) {
			//mSystemDAO = (ISystemDAO) SystemBeanFactory.getBean(DAO_BEAN_ID);
			mSystemDAO = new SystemDAO();
		}
		return mSystemDAO;
	}

	// --------------------------------------------------------------------------
	/**
	 * After we've either initialized the prefs or changed them then call this to effect the changes.
	 */
	public static void setLoggingLevelsFromPrefs() {

		PersistentProperty gwLogLvlProp = Util.getSystemDAO().findPersistentProperty(PersistentProperty.GENERAL_INTF_LOG_LEVEL);
		Level level = Level.toLevel(gwLogLvlProp.getCurrentValueAsStr());
		Logger.getRootLogger().setLevel(level);
		Logger.getLogger("com.gadgetworks").setLevel(level);

		gwLogLvlProp = Util.getSystemDAO().findPersistentProperty(PersistentProperty.GATEWAY_INTF_LOG_LEVEL);
		level = Level.toLevel(gwLogLvlProp.getCurrentValueAsStr());
		//Logger.getRootLogger().setLevel(level);
		//Logger.getLogger("com.gadgetworks.codeshelf.controller").setLevel(level);
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
