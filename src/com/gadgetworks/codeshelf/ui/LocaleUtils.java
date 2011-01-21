/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: LocaleUtils.java,v 1.1 2011/01/21 01:08:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ui;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class LocaleUtils {

	private static ResourceBundle mResourceBundle = ResourceBundle.getBundle("conf/locale");
	
	private LocaleUtils() {
		
	}

	/**
	 * Returns a string from the resource bundle.
	 * We don't want to crash because of a missing String.
	 * Returns the key if not found.
	 */
	public static String getStr(String inKey) {
		try {
			return mResourceBundle.getString(inKey);
		} catch (MissingResourceException e) {
			return inKey;
		} catch (NullPointerException e) {
			return "!" + inKey + "!";
		}			
	}

	/**
	 * Returns a string from the resource bundle and binds it
	 * with the given arguments. If the key is not found,
	 * return the key.
	 */
	public static String getStr(String inKey, Object[] inArgs) {
		try {
			return MessageFormat.format(getStr(inKey), inArgs);
		} catch (MissingResourceException e) {
			return inKey;
		} catch (NullPointerException e) {
			return "!" + inKey + "!";
		}
	}

}
