/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WirelessDeviceSetupPage.java,v 1.2 2012/07/19 06:11:33 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.gadgetworks.codeshelf.controller.NetMacAddress;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice;
import com.gadgetworks.codeshelf.ui.LocaleUtils;

/**
 *  
 */
public class WirelessDeviceSetupPage extends WizardPage {

	private WirelessDevice	mWirelessDevice;
	private Text			mMacAddr;
	private Text			mDescription;

	public WirelessDeviceSetupPage(final WirelessDevice inWirelessDevice) {

		super(LocaleUtils.getStr("network_device_wizard.setup_page.page_name"));

		mWirelessDevice = inWirelessDevice;

		setTitle(LocaleUtils.getStr("network_device_wizard.setup_page.page_title"));
		setDescription(LocaleUtils.getStr("network_device_wizard.setup_page.page_desc"));
		setPageComplete(false);
	}

	public final NetMacAddress getMacAddr() {
		return new NetMacAddress(mMacAddr.getText());
	}

	public final String getDescription() {
		return mDescription.getText();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public final void createControl(Composite inParent) {

		Composite composite = new Composite(inParent, SWT.NULL);
		composite.setLayout(new GridLayout(2, false));

		// MacAddr
		new Label(composite, SWT.NULL).setText(LocaleUtils.getStr("network_device_wizard.setup_page.guid"));
		mMacAddr = new Text(composite, SWT.SINGLE | SWT.BORDER);
		mMacAddr.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mMacAddr.setText(mWirelessDevice.getMacAddress().toString());

		// Description
		new Label(composite, SWT.NULL).setText(LocaleUtils.getStr("network_device_wizard.setup_page.description"));
		mDescription = new Text(composite, SWT.SINGLE | SWT.BORDER);
		mDescription.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mDescription.setText(mWirelessDevice.getDescription());

		Listener listener = new Listener() {
			public void handleEvent(Event inEvent) {

				if ((mMacAddr.getText().length() > 0) && (mDescription.getText().length() > 0)) {
					setPageComplete(true);
				} else {
					setPageComplete(false);
				}
			}
		};

		mMacAddr.addListener(SWT.Modify, listener);
		mDescription.addListener(SWT.Modify, listener);

		setControl(composite);
	}

}
