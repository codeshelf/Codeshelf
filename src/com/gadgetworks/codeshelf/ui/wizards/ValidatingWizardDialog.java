/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: ValidatingWizardDialog.java,v 1.1 2011/01/21 01:08:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ui.wizards;

import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public class ValidatingWizardDialog extends WizardDialog implements IPageChangedListener {
	public ValidatingWizardDialog(final Shell inParentShell, final IWizard inNewWizard) {
		super(inParentShell, inNewWizard);
		addPageChangedListener(this);
	}

	protected void backPressed() {
		IWizardPage currentActivePage = getCurrentPage();
		ASSERTPageIsValidatingWizardPage(currentActivePage);

		/* notify current page if it wants to do any validation on input */
		if (!((ValidatingWizardPageABC) currentActivePage).backPressed())
			return;

		/* delegate backPressed processing to super */
		super.backPressed();
	}

	protected void nextPressed() {
		IWizardPage currentActivePage = getCurrentPage();
		ASSERTPageIsValidatingWizardPage(currentActivePage);

		/* notify current page if it wants to do any validation on input */
		if (!((ValidatingWizardPageABC) currentActivePage).nextPressed())
			return;

		/* delegate nextPressed processing to super */
		super.nextPressed();
	}

	protected void finishPressed() {
		IWizardPage currentActivePage = getCurrentPage();
		ASSERTPageIsValidatingWizardPage(currentActivePage);

		/* notify current page if it wants to do any validation on input */
		if (!((ValidatingWizardPageABC) currentActivePage).finishPressed())
			return;

		/* delegate finishPressed processing to super */
		super.finishPressed();
	}

	private void ASSERTPageIsValidatingWizardPage(IWizardPage inPage) throws IllegalArgumentException {
		if (inPage == null)
			throw new IllegalArgumentException("Internal Error: Null page referred in ExWizardDialog");

		if (!(inPage instanceof ValidatingWizardPageABC))
			throw new IllegalArgumentException("Internal Error: Page extending from ValidatingWizardPageABC can only be open with ExWizardDialog");
	}

	protected void cancelPressed() {
		IWizardPage currentActivePage = getCurrentPage();
		ASSERTPageIsValidatingWizardPage(currentActivePage);

		/* notify current page if it wants to do any validation on input */
		if (!((ValidatingWizardPageABC) currentActivePage).cancelPressed())
			return;

		/* delegate cancelPressed processing to super */
		super.cancelPressed();
	}

	public void pageChanged(PageChangedEvent inEvent) {
		IWizardPage page = (IWizardPage) inEvent.getSelectedPage();
		ASSERTPageIsValidatingWizardPage(page);

		/* notify page it has been activated */
		((ValidatingWizardPageABC) page).pageActivated();
	}
}
