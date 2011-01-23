/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ControlGroupAddPage.java,v 1.1 2011/01/23 07:22:45 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import com.gadgetworks.codeshelf.ui.LocaleUtils;

/**
 *  
 */
public final class ControlGroupAddPage extends WizardPage {

	public ControlGroupAddPage() {

		super(LocaleUtils.getStr("controlgroup_wizard.add_page.page_name"));

		setTitle(LocaleUtils.getStr("controlgroup_wizard.add_page.title"));
		setDescription(LocaleUtils.getStr("controlgroup_wizard.add_page.desc"));
		setPageComplete(false);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite inParent) {

		Composite composite = new Composite(inParent, SWT.NULL);
		composite.setLayout(new GridLayout(2, false));

		new Label(composite, SWT.NULL).setText(LocaleUtils.getStr("controlgroup_wizard.add_page.source_category"));

		
		
		Listener listener = new Listener() {
			public void handleEvent(Event inEvent) {
//				if (inEvent.widget.equals(mSnapNetworkCombo)) {
//
//				}
				setPageComplete(isPageComplete());
			}
		};

//		mSnapNetworkCombo.addListener(SWT.Selection, listener);
		setControl(composite);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	public boolean isPageComplete() {
		return true;
	}
}
