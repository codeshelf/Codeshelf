/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ControlGroupWizard.java,v 1.3 2011/12/23 23:21:32 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ui.wizards;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;
import com.gadgetworks.codeshelf.model.persist.ControlGroup;
import com.gadgetworks.codeshelf.ui.LocaleUtils;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class ControlGroupWizard extends Wizard implements IPageChangingListener {

	private ControlGroupEditPage	mEditPage;

	public ControlGroupWizard(final ControlGroup inControlGroup) {

		mEditPage = new ControlGroupEditPage(inControlGroup);

		setWindowTitle(LocaleUtils.getStr("controlgroup_add_wizard.title"));
		setNeedsProgressMonitor(true);
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inCodeShelfNetwork
	 */
	public static void addControlGroup(CodeShelfNetwork inCodeShelfNetwork) {

		ControlGroup controlGroup = new ControlGroup();
		controlGroup.setId(inCodeShelfNetwork.getId().toString());
		controlGroup.setParentCodeShelfNetwork(inCodeShelfNetwork);

		inCodeShelfNetwork.addControlGroup(controlGroup);

		ControlGroupWizard wizard = new ControlGroupWizard(controlGroup);
		WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
		dialog.setBlockOnOpen(true);
		dialog.addPageChangingListener(wizard);

		if (dialog.open() == Dialog.OK) {

		}

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inControlGroup
	 */
	public static void editControlGroup(ControlGroup inControlGroup) {

		ControlGroupWizard wizard = new ControlGroupWizard(inControlGroup);
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
		addPage(mEditPage);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getStartingPage()
	 */
	@Override
	public IWizardPage getStartingPage() {
		IWizardPage result = null;
		result = mEditPage;
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	@Override
	public IWizardPage getNextPage(IWizardPage inPage) {
		IWizardPage result = null;
		result = mEditPage;
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

		// Save the values entered into the CodeShelfNetwork.
		return mEditPage.saveCodeShelfNetwork();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	public void handlePageChanging(PageChangingEvent inEvent) {
		if (inEvent.getTargetPage().equals(mEditPage)) {
			mEditPage.preparePage();
		}
	}

}
