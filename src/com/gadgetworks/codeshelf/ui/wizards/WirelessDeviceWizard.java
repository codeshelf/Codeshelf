/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WirelessDeviceWizard.java,v 1.3 2011/01/24 07:22:42 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.wizards;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.controller.IController;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice;
import com.gadgetworks.codeshelf.ui.LocaleUtils;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public class WirelessDeviceWizard extends Wizard {

	private WirelessDevice			mWirelessDevice;
	private WirelessDeviceSetupPage	mWirelessDeviceSetupPage;

	// --------------------------------------------------------------------------
	/**
	 *  @param inParentPerson
	 *  @param inShell
	 *  @return
	 */
	public static WirelessDevice createWirelessDevice(Shell inShell, IController inController) {

		WirelessDevice result = null;
		WirelessDevice wirelessDevice = new WirelessDevice();

		WirelessDeviceWizard wizard = new WirelessDeviceWizard(wirelessDevice);
		WizardDialog dialog = new WizardDialog(inShell, wizard);
		dialog.setBlockOnOpen(true);

		int returnCode = dialog.open();
		if (returnCode == Dialog.OK) {
			result = wirelessDevice;
			Util.getSystemDAO().storeWirelessDevice(wirelessDevice);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inParentPerson
	 *  @param inWirelessDevice
	 *  @param inShell
	 */
	public static void editWirelessDevice(WirelessDevice inWirelessDevice, IController inController, Shell inShell) {

		WirelessDeviceWizard wizard = new WirelessDeviceWizard(inWirelessDevice);
		WizardDialog dialog = new WizardDialog(inShell, wizard);
		dialog.setBlockOnOpen(true);

		int returnCode = dialog.open();
		if (returnCode == Dialog.OK) {
			// Create the person's first account
			Util.getSystemDAO().storeWirelessDevice(inWirelessDevice);
		}

	}

	// --------------------------------------------------------------------------
	/**
	 */
	public WirelessDeviceWizard(final WirelessDevice inWirelessDevice) {

		mWirelessDevice = inWirelessDevice;
		mWirelessDeviceSetupPage = new WirelessDeviceSetupPage(inWirelessDevice);

		setWindowTitle(LocaleUtils.getStr("new_person_wizard.title"));
		setNeedsProgressMonitor(true);
		//setDefaultPageImageDescriptor(Util.getImageRegistry().getDescriptor(Util.WIRELESS_DEVICE_ICON));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public final void addPages() {
		addPage(mWirelessDeviceSetupPage);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public final boolean performFinish() {

		// Update the account with the information from the dialog.
		mWirelessDevice.setMacAddress(mWirelessDeviceSetupPage.getMacAddr());
		mWirelessDevice.setDescription(mWirelessDeviceSetupPage.getDescription());
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performCancel()
	 */
	public final boolean performCancel() {
		boolean result = false;
		if (!mWirelessDeviceSetupPage.isPageComplete()) {
			result = true;
		} else {
			result = MessageDialog.openConfirm(getShell(),
				LocaleUtils.getStr("all_wizards.confirmation"),
				LocaleUtils.getStr("all_wizards.check_cancel"));
		}
		return result;
	}
}
