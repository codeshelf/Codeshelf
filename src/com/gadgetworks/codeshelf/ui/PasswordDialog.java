/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PasswordDialog.java,v 1.2 2011/01/21 01:12:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ui;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public final class PasswordDialog extends Dialog {

	private String	mDefaultPassword;
	private Text	mPasswordField;
	private String	mResult;

	public PasswordDialog(String inDefaultPassword, final Shell inParentShell) {
		super(inParentShell);
		mDefaultPassword = inDefaultPassword;
	}

	public String getValue() {
		return mResult;
	}

	protected Control createDialogArea(Composite inParent) {
		Composite dlogComposite = (Composite) super.createDialogArea(inParent);

		GridLayout layout = (GridLayout) dlogComposite.getLayout();
		layout.numColumns = 2;

		Label instructionLabel = new Label(dlogComposite, SWT.LEFT);
		instructionLabel.setText(LocaleUtils.getStr("password_dlog.instructions"));

		Label passwordLabel = new Label(dlogComposite, SWT.RIGHT);
		passwordLabel.setText(LocaleUtils.getStr("password_dlog.password_prompt"));

		mPasswordField = new Text(dlogComposite, SWT.SINGLE | SWT.PASSWORD);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		mPasswordField.setLayoutData(data);
		mPasswordField.setText(mDefaultPassword);

		return dlogComposite;
	}

	protected void buttonPressed(int inButtonId) {
		if (inButtonId == IDialogConstants.OK_ID) {
			mResult = mPasswordField.getText();
		}
		super.buttonPressed(inButtonId);

	}
}
