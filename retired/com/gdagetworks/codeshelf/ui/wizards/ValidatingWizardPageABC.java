/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ValidatingWizardPageABC.java,v 1.1 2012/03/16 15:59:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ui.wizards;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public abstract class ValidatingWizardPageABC extends WizardPage {
	/**
	* Font metrics to use for determining pixel sizes.
	*/
	private FontMetrics	mfontMetrics	= null;

	protected ValidatingWizardPageABC(final String inPageName) {
		super(inPageName);
	}

	protected ValidatingWizardPageABC(final String inPageName, final String inTitle, final ImageDescriptor inTitleImage) {
		super(inPageName, inTitle, inTitleImage);
	}

	public void createControl(Composite parent) {
		Composite pageComposite = new Composite(parent, SWT.NONE);
		setControl(pageComposite);
		initializeDialogUnits(pageComposite);

		//set layout for page composite 
		GridLayout layout = new GridLayout();
		setStandardDialogMarginAndSpacing(layout);
		pageComposite.setLayout(layout);

		//create page contents 
		createPageContents(pageComposite);

		//set dialog font to composite and all of its children
		Dialog.applyDialogFont(pageComposite);
	}

	protected void setStandardDialogMarginAndSpacing(GridLayout inLayout) {
		inLayout.marginWidth = Dialog.convertHorizontalDLUsToPixels(mfontMetrics, IDialogConstants.HORIZONTAL_MARGIN);
		inLayout.marginHeight = Dialog.convertVerticalDLUsToPixels(mfontMetrics, IDialogConstants.VERTICAL_MARGIN);
		inLayout.horizontalSpacing = Dialog.convertHorizontalDLUsToPixels(mfontMetrics, IDialogConstants.HORIZONTAL_SPACING);
		inLayout.verticalSpacing = Dialog.convertVerticalDLUsToPixels(mfontMetrics, IDialogConstants.VERTICAL_SPACING);
	}

	protected void initializeDialogUnits(Control inControl) {
		// Compute and store a font metric
		GC gc = new GC(inControl);
		gc.setFont(JFaceResources.getDialogFont());
		mfontMetrics = gc.getFontMetrics();
		gc.dispose();
	}

	protected abstract void createPageContents(Composite inPageComposite);

	protected abstract void pageActivated();

	/**
	* Clients can validate current active page here before moving to previous page.
	* @return TODO
	*/
	protected boolean backPressed() {
		return true;
	}

	/**
	* Clients can validate current active page here before moving to next page.
	* @return TODO
	*/
	protected boolean nextPressed() {
		return true;
	}

	/**
	* Clients can validate current active page here before finishing.
	* @return TODO
	*/
	protected boolean finishPressed() {
		return true;
	}

	/**
	* Clients can validate current active page here before cancelling
	* @return TODO
	*/
	protected boolean cancelPressed() {
		return true;
	}
}