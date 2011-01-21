/*******************************************************************************
 *  HoobeeNet
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: SnapNetworkAddPage.java,v 1.1 2011/01/21 20:05:36 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.wizards;

import java.util.Arrays;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import com.gadgetworks.codeshelf.ui.LocaleUtils;

/**
 *  
 */
public final class SnapNetworkAddPage extends WizardPage {

	private Combo	mSnapNetworkCombo;

	public SnapNetworkAddPage() {

		super(LocaleUtils.getStr("ruleset_wizard.add_page.page_name"));

		setTitle(LocaleUtils.getStr("ruleset_wizard.add_page.title"));
		setDescription(LocaleUtils.getStr("ruleset_wizard.add_page.desc"));
		setPageComplete(false);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite inParent) {

		Composite composite = new Composite(inParent, SWT.NULL);
		composite.setLayout(new GridLayout(2, false));

		// Rule
		new Label(composite, SWT.NULL).setText(LocaleUtils.getStr("ruleset_wizard.add_page.source_category"));

		Composite comboComposite = new Composite(composite, SWT.NULL);
		comboComposite.setLayout(new RowLayout());
		mSnapNetworkCombo = new Combo(comboComposite, SWT.BORDER | SWT.READ_ONLY);

		String[] items = mSnapNetworkCombo.getItems();
		Arrays.sort(items);
		mSnapNetworkCombo.setItems(items);
		mSnapNetworkCombo.setVisibleItemCount(items.length);
		mSnapNetworkCombo.select(0);

		Listener listener = new Listener() {
			public void handleEvent(Event inEvent) {
				if (inEvent.widget.equals(mSnapNetworkCombo)) {

				}
				setPageComplete(isPageComplete());
			}
		};

		mSnapNetworkCombo.addListener(SWT.Selection, listener);
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
