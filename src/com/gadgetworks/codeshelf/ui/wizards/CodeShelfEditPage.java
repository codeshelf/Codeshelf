/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeShelfEditPage.java,v 1.4 2011/01/24 07:22:42 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ui.wizards;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.controller.NetworkId;
import com.gadgetworks.codeshelf.model.dao.DAOException;
import com.gadgetworks.codeshelf.model.dao.IDAOListener;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;
import com.gadgetworks.codeshelf.ui.LocaleUtils;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class CodeShelfEditPage extends WizardPage implements IDoubleClickListener, IDAOListener {

	private static final Log	LOGGER			= LogFactory.getLog(CodeShelfEditPage.class);

	private static final int	VALUE_COL_WIDTH	= 500;

	private CodeShelfNetwork	mCodeShelfNetwork;
	private Label				mNetworkIdLabel;
	private Text				mNetworkIdField;
	private Label				mDescriptionLabel;
	private Text				mDescriptionField;
	private Button				mIsActiveButton;
	private Listener			mEditListener;
	private Composite			mEditComposite;

	public CodeShelfEditPage(final CodeShelfNetwork inCodeShelfNetwork) {
		super(LocaleUtils.getStr("codeshelfnet_wizard.edit_page.page_name"));

		mCodeShelfNetwork = inCodeShelfNetwork;

		if (mCodeShelfNetwork != null) {
			setTitle(mCodeShelfNetwork.getDescription());
		} else {
			setTitle(LocaleUtils.getStr("codeshelfnet_wizard.edit_page.title"));
		}
		setDescription(LocaleUtils.getStr("codeshelfnet_wizard.edit_page.desc"));
		setPageComplete(false);
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inCodeShelfNetwork
	 */
	public final void setCodeShelfNetwork(CodeShelfNetwork inCodeShelfNetwork) {
		mCodeShelfNetwork = inCodeShelfNetwork;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite inParent) {

		GridData gridData;

		Util.getSystemDAO().registerDAOListener(this);

		mEditComposite = new Composite(inParent, SWT.NULL);
		mEditComposite.setLayout(new GridLayout(5, false));

		setControl(mEditComposite);

		preparePage();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IDoubleClickListener#doubleClick(org.eclipse.jface.viewers.DoubleClickEvent)
	 */
	public void doubleClick(DoubleClickEvent inEvent) {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inLabel
	 *  @param inText
	 *  @param inParentComposite
	 *  @param inLabelText
	 *  @param inLabelColumns
	 *  @param inTextColumns
	 */
	private void createInputField(Label inLabel,
		Text inText,
		Composite inParentComposite,
		String inLabelText,
		int inLabelColumns,
		int inTextColumns,
		int inBlankColumns) {

		GridData gridData;

		inLabel.setText(inLabelText);
		gridData = new GridData(SWT.RIGHT, SWT.TOP, false, false, inLabelColumns, 1);
		inLabel.setLayoutData(gridData);

		gridData = new GridData(SWT.FILL, SWT.TOP, true, false, inTextColumns, 1);
		inText.setLayoutData(gridData);

		Label label = new Label(inParentComposite, SWT.RIGHT);
		gridData = new GridData(SWT.FILL, SWT.TOP, true, false, inBlankColumns, 1);
		label.setLayoutData(gridData);
	}

	public void preparePage() {
		if (mCodeShelfNetwork != null) {
			setTitle(mCodeShelfNetwork.getDescription());

			// Id field.
			mNetworkIdLabel = new Label(mEditComposite, SWT.RIGHT);
			mNetworkIdField = new Text(mEditComposite, SWT.SINGLE | SWT.BORDER);
			String labelStr = LocaleUtils.getStr("codeshelfnet_wizard.edit_page.id_field");
			createInputField(mNetworkIdLabel, mNetworkIdField, mEditComposite, labelStr, 1, 4, 5);
			mNetworkIdField.setText(mCodeShelfNetwork.getId().toString());

			// Description field.
			mDescriptionLabel = new Label(mEditComposite, SWT.RIGHT);
			mDescriptionField = new Text(mEditComposite, SWT.SINGLE | SWT.BORDER);
			labelStr = LocaleUtils.getStr("codeshelfnet_wizard.edit_page.desc_field");
			createInputField(mDescriptionLabel, mDescriptionField, mEditComposite, labelStr, 1, 4, 5);
			mDescriptionField.setText(mCodeShelfNetwork.getDescription());

			// Add the "is active" button.
			mIsActiveButton = new Button(mEditComposite, SWT.CHECK);
			mIsActiveButton.setSelection(mCodeShelfNetwork.getIsActive());
			mIsActiveButton.setText(LocaleUtils.getStr("codeshelfnet_wizard.edit_page.isactive"));
			GridData gridData = new GridData(SWT.LEFT, SWT.TOP, false, false, 10, 1);
			mIsActiveButton.setLayoutData(gridData);

			mEditListener = new Listener() {
				public void handleEvent(Event inEvent) {
					setPageComplete(isPageComplete());
				}
			};

			mDescriptionField.addListener(SWT.Modify, mEditListener);

			mEditComposite.pack();
			mEditComposite.getParent().layout();
		}

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	public boolean isPageComplete() {

		boolean result = true;

		// If there is no description, then the page is not complete.
		if ((mDescriptionField == null) || (mDescriptionField.getText().length() == 0)) {
			result = false;
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public boolean saveCodeShelfNetwork() {

		boolean result = true;

		Text field;

		if ((result == true) && (mCodeShelfNetwork != null)) {

			NetworkId networkId = new NetworkId(mNetworkIdField.getText());
			mCodeShelfNetwork.setId(networkId);
			mCodeShelfNetwork.setDescription(mDescriptionField.getText());
			mCodeShelfNetwork.setIsActive(mIsActiveButton.getSelection());

			// Save the CodeShelf Network.
			try {
				Util.getSystemDAO().storeCodeShelfNetwork(mCodeShelfNetwork);
			} catch (DAOException e) {
				LOGGER.error("", e);
			}
		}
		return result;
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.hbnet.model.dao.IDAOListener#objectAdded(java.lang.Object)
	 */
	public void objectAdded(Object inObject) {
		if (doesDAOChangeApply(inObject)) {
			if (inObject instanceof CodeShelfNetwork) {

			}
		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.hbnet.model.dao.IDAOListener#objectDeleted(java.lang.Object)
	 */
	public void objectDeleted(Object inObject) {
		if (doesDAOChangeApply(inObject)) {
			if (inObject instanceof CodeShelfNetwork) {

			}
		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.hbnet.model.dao.IDAOListener#objectUpdated(java.lang.Object)
	 */
	public void objectUpdated(Object inObject) {
		if (doesDAOChangeApply(inObject)) {
			if (inObject instanceof CodeShelfNetwork) {

			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inObject
	 *  @return
	 */
	private boolean doesDAOChangeApply(Object inObject) {
		boolean result = false;

		// We only want to update if it's an account or buddy update.
		if (inObject instanceof CodeShelfNetwork) {
			CodeShelfNetwork codeShelfNetwork = (CodeShelfNetwork) inObject;
			if (codeShelfNetwork.equals(mCodeShelfNetwork)) {
				result = true;
			}
		}

		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	@Override
	public void dispose() {
		Util.getSystemDAO().unregisterDAOListener(this);
		super.dispose();
	}
}
