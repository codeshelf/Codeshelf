/*******************************************************************************
 *  HoobeeNet
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: SnapNetworkWizard.java,v 1.1 2011/01/21 20:05:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ui.wizards;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import com.gadgetworks.codeshelf.model.persist.SnapNetwork;
import com.gadgetworks.codeshelf.ui.LocaleUtils;
import com.gadgetworks.hbnet.model.persist.HooBee;
import com.gadgetworks.hbnet.model.persist.Ruleset;
import com.gadgetworks.hbnet.model.persist.TemplateRuleset;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class SnapNetworkWizard extends Wizard implements IPageChangingListener {

	private SnapNetwork			mSnapNetwork;

	private SnapNetworkAddPage	mAddPage;
	private SnapNetworkEditPage	mEditPage;

	public SnapNetworkWizard(final SnapNetwork inSnapNetwork) {

		mSnapNetwork = inSnapNetwork;

		mAddPage = new SnapNetworkAddPage();
		mEditPage = new SnapNetworkEditPage(inSnapNetwork);

		setWindowTitle(LocaleUtils.getStr("hoobee_add_ruleset_wizard.title"));
		setNeedsProgressMonitor(true);
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inSnapNetwork
	 */
	public static void addSnapNetwork(SnapNetwork inSnapNetwork) {

		SnapNetworkWizard wizard = new SnapNetworkWizard(null);
		WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
		dialog.setBlockOnOpen(true);
		dialog.addPageChangingListener(wizard);

		if (dialog.open() == Dialog.OK) {

		}

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inSnapNetwork
	 */
	public static void editSnapNetwork(SnapNetwork inSnapNetwork) {

		SnapNetworkWizard wizard = new SnapNetworkWizard(inSnapNetwork);
		WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
		dialog.setBlockOnOpen(true);
		dialog.addPageChangingListener(wizard);

		if (dialog.open() == Dialog.OK) {

		}

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {

		// If there is no specified ruleset, or it's a template then add a page for selecting a ruleset.
		if (mSnapNetwork == null) {
			addPage(mAddPage);
		}
		addPage(mEditPage);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getStartingPage()
	 */
	@Override
	public IWizardPage getStartingPage() {
		IWizardPage result = null;
		if (mSnapNetwork == null) {
			result = mAddPage;
		} else {
			result = mEditPage;
		}

		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	@Override
	public IWizardPage getNextPage(IWizardPage inPage) {
		IWizardPage result = null;
		if (getContainer().getCurrentPage() == null) {
			result = mAddPage;
		} else if (getContainer().getCurrentPage() == mAddPage) {
			result = mEditPage;
		}
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	@Override
	public boolean canFinish() {
		boolean result = false;
		if (getContainer().getCurrentPage().equals(mEditPage)) {
			result = super.canFinish();
		}
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {

		// Save the values entered into the SnapNetwork.
		return mEditPage.saveSnapNetwork();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	public void handlePageChanging(PageChangingEvent inEvent) {
		if ((inEvent.getCurrentPage().equals(mAddPage)) && (inEvent.getTargetPage().equals(mEditPage))) {
			mEditPage.setSnapNetwork(mSnapNetwork);
		}
		if (inEvent.getTargetPage().equals(mEditPage)) {
			mEditPage.preparePage();
		}
	}

}
