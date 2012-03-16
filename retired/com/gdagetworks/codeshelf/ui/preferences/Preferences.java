/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Preferences.java,v 1.1 2012/03/16 15:59:10 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ui.preferences;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.model.PreferencesStore;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public class Preferences {

	public static final String	DEBUG_PREFS_ID		= "Debug";
	public static final String	CONNECTION_PREFS_ID	= "Connection";
	public static final String	ACTIVEMQ_PREFS_ID	= "ActiveMQ";

	private PreferenceManager	mPrefsManager;
	private PreferenceNode		mDebugNode;
	private PreferenceNode		mConnectionNode;
	private PreferenceNode		mActiveMqNode;
	private PreferenceDialog	mPreferenceDialog;
	private PreferencesStore	mPreferencesStore;

	public Preferences() {
		mPrefsManager = new PreferenceManager();
		mDebugNode = new PreferenceNode(DEBUG_PREFS_ID,
			DEBUG_PREFS_ID,
			Util.getImageRegistry().getDescriptor(Util.DEBUG_ICON),
			DebugPage.class.getName());
		//mDebugConnectionNode = new PreferenceNode(CONNECTION_PREFS_ID, new ConnectionPage());
		mConnectionNode = new PreferenceNode(CONNECTION_PREFS_ID, CONNECTION_PREFS_ID, null, ConnectionPage.class.getName());
		mActiveMqNode = new PreferenceNode(ACTIVEMQ_PREFS_ID, ACTIVEMQ_PREFS_ID, null, ActiveMqPage.class.getName());

	}

	public static int editPreferences() {
		int result;

		Preferences prefs = new Preferences();
		result = prefs.openPreferences();

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final int openPreferences() {

		int result;

		// Add the nodes
		mPrefsManager.addToRoot(mDebugNode);
		mPrefsManager.addToRoot(mConnectionNode);
		mPrefsManager.addToRoot(mActiveMqNode);

		// Create the preferences dialog
		mPreferenceDialog = new PreferenceDialog(null, mPrefsManager);

		// Set the preference store
		mPreferencesStore = PreferencesStore.getPreferencesStore();
		mPreferenceDialog.setPreferenceStore(mPreferencesStore);

		Util.keepDialogForward(mPreferenceDialog);
		
		// Open the dialog
		result = mPreferenceDialog.open();

		if (result == Window.OK) {

		}

		return result;
	}
}
