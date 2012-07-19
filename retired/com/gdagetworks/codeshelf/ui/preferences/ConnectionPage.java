/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ConnectionPage.java,v 1.2 2012/07/19 06:11:34 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.gadgetworks.codeshelf.controller.ControllerABC;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;
import com.gadgetworks.codeshelf.ui.LocaleUtils;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class ConnectionPage extends PreferencePage {

	private Combo	mForceChannelNumber;

	public ConnectionPage() {
		super(Preferences.CONNECTION_PREFS_ID);
		setDescription(LocaleUtils.getStr("prefs.connection.title"));
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

		(new Label(composite, SWT.NULL)).setText(LocaleUtils.getStr("prefs.connection.force_channel"));		
		mForceChannelNumber = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);

		mForceChannelNumber.add(ControllerABC.NO_PREFERRED_CHANNEL_TEXT);
		for (int i = 0; i < ControllerABC.MAX_CHANNELS; i++) {
			mForceChannelNumber.add(Integer.toString(i));
		}

		String channel = preferenceStore.getString(PersistentProperty.FORCE_CHANNEL);
		if (channel.equals(ControllerABC.NO_PREFERRED_CHANNEL_TEXT)) {
			mForceChannelNumber.select(0);
		} else {
			mForceChannelNumber.select(Integer.valueOf(channel) + 1);
		}

		return composite;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		
		// Reset the fields to the default by selecting the first item in the list.
		mForceChannelNumber.select(0);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	public boolean performOk() {
		// Get the preference store
		IPreferenceStore preferenceStore = getPreferenceStore();

		// Save the values
		preferenceStore.setValue(PersistentProperty.FORCE_CHANNEL, mForceChannelNumber.getText());

		// Return true to allow dialog to close
		return true;
	}
}
