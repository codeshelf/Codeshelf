/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PickTagDeviceWizard.java,v 1.8 2011/12/29 09:15:35 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.wizards;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import com.gadgetworks.codeshelf.controller.IController;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.model.dao.DAOException;
import com.gadgetworks.codeshelf.model.persist.ControlGroup;
import com.gadgetworks.codeshelf.model.persist.PickTag;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice;
import com.gadgetworks.codeshelf.ui.LocaleUtils;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public class PickTagDeviceWizard extends Wizard {

	private static final Log		LOGGER	= LogFactory.getLog(PickTagDeviceWizard.class);

	private PickTag					mPickTag;
	private PickTagDeviceSetupPage	mPickTagDeviceSetupPage;

	// --------------------------------------------------------------------------
	/**
	 *  @param inParentPerson
	 *  @param inShell
	 *  @return
	 */
	public static PickTag createPickTagDevice(ControlGroup inControlGroup, Shell inShell, IController inController) {

		PickTag result = null;
		PickTag pickTag = new PickTag();
		pickTag.setParentControlGroup(inControlGroup);
		inControlGroup.addPickTag(pickTag);

		PickTagDeviceWizard wizard = new PickTagDeviceWizard(pickTag);
		WizardDialog dialog = new WizardDialog(inShell, wizard);
		dialog.setBlockOnOpen(true);

		int returnCode = dialog.open();
		if (returnCode == Dialog.OK) {
			result = pickTag;
			try {
				WirelessDevice.DAO.store(pickTag);
			} catch (DAOException e) {
				LOGGER.error("", e);
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inParentPerson
	 *  @param inPickTag
	 *  @param inShell
	 */
	public static void editPickTagDevice(PickTag inPickTag, IController inController, Shell inShell) {

		PickTagDeviceWizard wizard = new PickTagDeviceWizard(inPickTag);
		WizardDialog dialog = new WizardDialog(inShell, wizard);
		dialog.setBlockOnOpen(true);

		int returnCode = dialog.open();
		if (returnCode == Dialog.OK) {
			// Create the picktag
			try {
				WirelessDevice.DAO.store(inPickTag);
			} catch (DAOException e) {
				LOGGER.error("", e);
			}
		}

	}

	// --------------------------------------------------------------------------
	/**
	 */
	public PickTagDeviceWizard(final PickTag inPickTag) {

		mPickTag = inPickTag;
		mPickTagDeviceSetupPage = new PickTagDeviceSetupPage(inPickTag);

		setWindowTitle(LocaleUtils.getStr("codeshelfnet_wizard.title"));
		setNeedsProgressMonitor(true);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public final void addPages() {
		addPage(mPickTagDeviceSetupPage);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public final boolean performFinish() {

		// Update the account with the information from the dialog.
		mPickTag.setMacAddress(mPickTagDeviceSetupPage.getMacAddr());
		String macAddrString = mPickTag.getMacAddress().toString();
		String netAddrString = macAddrString.substring(12, 18);
		NetAddress networkAddress = new NetAddress("0x" + netAddrString);
		mPickTag.setId(macAddrString);
		mPickTag.setNetAddress(networkAddress);
		mPickTag.setDescription(mPickTagDeviceSetupPage.getDescription());
		mPickTag.setSerialBusPosition(Short.parseShort(mPickTagDeviceSetupPage.getSerialBusPosition()));
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performCancel()
	 */
	public final boolean performCancel() {
		boolean result = false;
		if (!mPickTagDeviceSetupPage.isPageComplete()) {
			result = true;
		} else {
			result = MessageDialog.openConfirm(getShell(), LocaleUtils.getStr("all_wizards.confirmation"), LocaleUtils.getStr("all_wizards.check_cancel"));
		}
		return result;
	}
}
