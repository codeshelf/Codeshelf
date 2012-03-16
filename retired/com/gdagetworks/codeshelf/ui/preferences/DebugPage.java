/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: DebugPage.java,v 1.1 2012/03/16 15:59:10 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ui.preferences;

import org.apache.log4j.Level;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.gadgetworks.codeshelf.model.persist.PersistentProperty;
import com.gadgetworks.codeshelf.ui.LocaleUtils;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class DebugPage extends PreferencePage {

	private Button	mShowConsoleButton;
	private Button	mShowConnectDebugButton;
	private Combo	mGeneralLogLvlCombo;
	private Combo	mGatewayLogLvlCombo;

	public DebugPage() {
		super(Preferences.DEBUG_PREFS_ID);
		setDescription(LocaleUtils.getStr("prefs.debug.title"));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite inParent) {
		Composite composite = new Composite(inParent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		// Get the preference store
		IPreferenceStore preferenceStore = getPreferenceStore();

		(new Label(composite, SWT.NULL)).setText(LocaleUtils.getStr("prefs.debug.show_console_button"));
		mShowConsoleButton = new Button(composite, SWT.CHECK);
		mShowConsoleButton.setSelection(preferenceStore.getBoolean(PersistentProperty.SHOW_CONSOLE_PREF));

		(new Label(composite, SWT.NULL)).setText(LocaleUtils.getStr("prefs.debug.show_connection_debug_button"));
		mShowConnectDebugButton = new Button(composite, SWT.CHECK);
		mShowConnectDebugButton.setSelection(preferenceStore.getBoolean(PersistentProperty.SHOW_CONSOLE_PREF));

		(new Label(composite, SWT.NULL)).setText(LocaleUtils.getStr("prefs.debug.general_log_level"));
		mGeneralLogLvlCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);

		mGeneralLogLvlCombo.add(Level.FATAL.toString());
		mGeneralLogLvlCombo.add(Level.ERROR.toString());
		mGeneralLogLvlCombo.add(Level.WARN.toString());
		mGeneralLogLvlCombo.add(Level.INFO.toString());
		mGeneralLogLvlCombo.add(Level.DEBUG.toString());

		String logLevel = preferenceStore.getString(PersistentProperty.GENERAL_INTF_LOG_LEVEL);
		if (logLevel.equals(Level.FATAL.toString())) {
			mGeneralLogLvlCombo.select(0);
		} else if (logLevel.equals(Level.ERROR.toString())) {
			mGeneralLogLvlCombo.select(1);
		} else if (logLevel.equals(Level.WARN.toString())) {
			mGeneralLogLvlCombo.select(2);
		} else if (logLevel.equals(Level.INFO.toString())) {
			mGeneralLogLvlCombo.select(3);
		} else if (logLevel.equals(Level.DEBUG.toString())) {
			mGeneralLogLvlCombo.select(4);
		}

		(new Label(composite, SWT.NULL)).setText(LocaleUtils.getStr("prefs.debug.gateway_log_level"));
		mGatewayLogLvlCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);

		mGatewayLogLvlCombo.add(Level.FATAL.toString());
		mGatewayLogLvlCombo.add(Level.ERROR.toString());
		mGatewayLogLvlCombo.add(Level.WARN.toString());
		mGatewayLogLvlCombo.add(Level.INFO.toString());
		mGatewayLogLvlCombo.add(Level.DEBUG.toString());

		logLevel = preferenceStore.getString(PersistentProperty.GATEWAY_INTF_LOG_LEVEL);
		if (logLevel.equals(Level.FATAL.toString())) {
			mGatewayLogLvlCombo.select(0);
		} else if (logLevel.equals(Level.ERROR.toString())) {
			mGatewayLogLvlCombo.select(1);
		} else if (logLevel.equals(Level.WARN.toString())) {
			mGatewayLogLvlCombo.select(2);
		} else if (logLevel.equals(Level.INFO.toString())) {
			mGatewayLogLvlCombo.select(3);
		} else if (logLevel.equals(Level.DEBUG.toString())) {
			mGatewayLogLvlCombo.select(4);
		}

		return composite;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		// Get the preference store
		IPreferenceStore preferenceStore = getPreferenceStore();

		// Reset the fields to the defaults
		mShowConsoleButton.setSelection(preferenceStore.getDefaultBoolean(PersistentProperty.SHOW_CONSOLE_PREF));
		mShowConnectDebugButton.setSelection(preferenceStore.getDefaultBoolean(PersistentProperty.SHOW_CONNECTION_DEBUG_PREF));
		mGeneralLogLvlCombo.select(3);
		mGatewayLogLvlCombo.select(3);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	public boolean performOk() {
		// Get the preference store
		IPreferenceStore preferenceStore = getPreferenceStore();

		// Save the values
		preferenceStore.setValue(PersistentProperty.SHOW_CONSOLE_PREF, mShowConsoleButton.getSelection());
		preferenceStore.setValue(PersistentProperty.SHOW_CONNECTION_DEBUG_PREF, mShowConnectDebugButton.getSelection());

		preferenceStore.setValue(PersistentProperty.GENERAL_INTF_LOG_LEVEL, mGeneralLogLvlCombo.getText());
		preferenceStore.setValue(PersistentProperty.GATEWAY_INTF_LOG_LEVEL, mGatewayLogLvlCombo.getText());

		// Return true to allow dialog to close
		return true;
	}
}
