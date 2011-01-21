/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: PickTagDeviceWizard.java,v 1.1 2011/01/21 01:08:21 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.wizards;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.controller.IController;
import com.gadgetworks.codeshelf.model.dao.DAOException;
import com.gadgetworks.codeshelf.model.persist.PickTag;
import com.gadgetworks.codeshelf.ui.LocaleUtils;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public class PickTagDeviceWizard extends Wizard {

	private static final Log		LOGGER	= LogFactory.getLog(PickTagDeviceWizard.class);

	private PickTag				mPickTag;
	private PickTagDeviceSetupPage	mPickTagDeviceSetupPage;

	// --------------------------------------------------------------------------
	/**
	 *  @param inParentPerson
	 *  @param inShell
	 *  @return
	 */
	public static PickTag createPickTagDevice(Shell inShell, IController inController) {

		PickTag result = null;
		PickTag datoBlok = new PickTag();

		PickTagDeviceWizard wizard = new PickTagDeviceWizard(datoBlok);
		WizardDialog dialog = new WizardDialog(inShell, wizard);
		dialog.setBlockOnOpen(true);

		int returnCode = dialog.open();
		if (returnCode == Dialog.OK) {
			result = datoBlok;
			Util.getSystemDAO().storeWirelessDevice(datoBlok);
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
			// Create the person's first account
			try {
				Util.getSystemDAO().storePickTag(inPickTag);
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

		setWindowTitle(LocaleUtils.getStr("datoblok_wizard.title"));
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
		mPickTag.setGUID(mPickTagDeviceSetupPage.getGUID());
		mPickTag.setDescription(mPickTagDeviceSetupPage.getDescription());
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
			result = MessageDialog.openConfirm(getShell(),
				LocaleUtils.getStr("all_wizards.confirmation"),
				LocaleUtils.getStr("all_wizards.check_cancel"));
		}
		return result;
	}
}
