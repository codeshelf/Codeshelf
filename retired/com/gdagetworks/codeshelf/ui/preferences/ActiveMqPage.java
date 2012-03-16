/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ActiveMqPage.java,v 1.1 2012/03/16 15:59:10 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ui.preferences;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.gadgetworks.codeshelf.model.persist.PersistentProperty;
import com.gadgetworks.codeshelf.ui.LocaleUtils;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class ActiveMqPage extends PreferencePage {

	private Button	mUseActiveMqButton;
	private Text	mUserIdField;
	private Text	mPasswordField;
	private Text	mJmsPortNumField;
	private Text	mStompPortNumField;

	public ActiveMqPage() {
		super(Preferences.ACTIVEMQ_PREFS_ID);
		setDescription(LocaleUtils.getStr("prefs.activemq.title"));
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

		(new Label(composite, SWT.NULL)).setText(LocaleUtils.getStr("prefs.activemq.run_activemq_button"));
		mUseActiveMqButton = new Button(composite, SWT.CHECK);
		mUseActiveMqButton.setSelection(preferenceStore.getBoolean(PersistentProperty.ACTIVEMQ_RUN));

		(new Label(composite, SWT.NULL)).setText(LocaleUtils.getStr("prefs.activemq.userid_label"));
		mUserIdField = new Text(composite, SWT.BORDER);
		mUserIdField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mUserIdField.setText(preferenceStore.getString(PersistentProperty.ACTIVEMQ_USERID));

		(new Label(composite, SWT.NULL)).setText(LocaleUtils.getStr("prefs.activemq.password_label"));
		mPasswordField = new Text(composite, SWT.BORDER);
		mPasswordField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mPasswordField.setText(preferenceStore.getString(PersistentProperty.ACTIVEMQ_PASSWORD));

		(new Label(composite, SWT.NULL)).setText(LocaleUtils.getStr("prefs.activemq.jms_port_label"));
		mJmsPortNumField = new Text(composite, SWT.BORDER);
		mJmsPortNumField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mJmsPortNumField.setText(preferenceStore.getString(PersistentProperty.ACTIVEMQ_JMS_PORTNUM));

		(new Label(composite, SWT.NULL)).setText(LocaleUtils.getStr("prefs.activemq.stomp_port_label"));
		mStompPortNumField = new Text(composite, SWT.BORDER);
		mStompPortNumField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mStompPortNumField.setText(preferenceStore.getString(PersistentProperty.ACTIVEMQ_STOMP_PORTNUM));

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
		mUseActiveMqButton.setSelection(preferenceStore.getDefaultBoolean(PersistentProperty.ACTIVEMQ_RUN));
		mUserIdField.setText(preferenceStore.getDefaultString(PersistentProperty.ACTIVEMQ_USERID));
		mPasswordField.setText(preferenceStore.getDefaultString(PersistentProperty.ACTIVEMQ_PASSWORD));
		mJmsPortNumField.setText(preferenceStore.getDefaultString(PersistentProperty.ACTIVEMQ_JMS_PORTNUM));
		mStompPortNumField.setText(preferenceStore.getDefaultString(PersistentProperty.ACTIVEMQ_STOMP_PORTNUM));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	public boolean performOk() {
		boolean result = true;

		// If activeMQ is active, then the pw must be non-blank.
		if ((mUseActiveMqButton.getSelection()) && (mPasswordField.getText().length() == 0)) {
			result = false;
			MessageDialog.openError(getShell(),
				LocaleUtils.getStr("prefs.activemq.invalid_pw.title"),
				LocaleUtils.getStr("prefs.activemq.invalid_pw.message"));
		} else {

			// Get the preference store
			IPreferenceStore preferenceStore = getPreferenceStore();

			// Save the values
			preferenceStore.setValue(PersistentProperty.ACTIVEMQ_RUN, mUseActiveMqButton.getSelection());
			preferenceStore.setValue(PersistentProperty.ACTIVEMQ_USERID, mUserIdField.getText());
			preferenceStore.setValue(PersistentProperty.ACTIVEMQ_PASSWORD, mPasswordField.getText());
			preferenceStore.setValue(PersistentProperty.ACTIVEMQ_JMS_PORTNUM, mJmsPortNumField.getText());
			preferenceStore.setValue(PersistentProperty.ACTIVEMQ_STOMP_PORTNUM, mStompPortNumField.getText());
		}
		// Return true to allow dialog to close
		return result;
	}
}
