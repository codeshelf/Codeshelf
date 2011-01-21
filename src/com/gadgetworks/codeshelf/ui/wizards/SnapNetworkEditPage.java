/*******************************************************************************
 *  HoobeeNet
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: SnapNetworkEditPage.java,v 1.1 2011/01/21 20:05:35 jeffw Exp $
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
import com.gadgetworks.codeshelf.model.dao.DAOException;
import com.gadgetworks.codeshelf.model.dao.IDAOListener;
import com.gadgetworks.codeshelf.model.persist.SnapNetwork;
import com.gadgetworks.codeshelf.ui.LocaleUtils;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class SnapNetworkEditPage extends WizardPage implements IDoubleClickListener, IDAOListener {

	private static final Log	LOGGER			= LogFactory.getLog(SnapNetworkEditPage.class);

	private static final int	VALUE_COL_WIDTH	= 500;

	private SnapNetwork			mSnapNetwork;
	private Button				mIsActiveButton;
	private Label				mDescriptionLabel;
	private Text				mDescriptionField;
	private Listener			mEditListener;
	private Composite			mEditComposite;

	public SnapNetworkEditPage(final SnapNetwork inSnapNetwork) {
		super(LocaleUtils.getStr("snapnetwork_wizard.edit_page.page_name"));

		mSnapNetwork = inSnapNetwork;

		if (mSnapNetwork != null) {
			setTitle(mSnapNetwork.getDescription());
		} else {
			setTitle(LocaleUtils.getStr("snapnetwork_wizard.edit_page.title"));
		}
		setDescription(LocaleUtils.getStr("snapnetwork_wizard.edit_page.desc"));
		setPageComplete(false);
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inSnapNetwork
	 */
	public final void setSnapNetwork(SnapNetwork inSnapNetwork) {
		mSnapNetwork = inSnapNetwork;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite inParent) {

		GridData gridData;

		Util.getSystemDAO().registerDAOListener(this);

		Composite mEditComposite = new Composite(inParent, SWT.NULL);
		mEditComposite.setLayout(new GridLayout(1, false));

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
		if (mSnapNetwork != null) {
			setTitle(mSnapNetwork.getDescription());

			// Add the "is active" button.
			mIsActiveButton = new Button(mEditComposite, SWT.CHECK);
			mIsActiveButton.setSelection(mSnapNetwork.getIsActive());
			mIsActiveButton.setText(LocaleUtils.getStr("snapnetwork_wizard.edit_page.isactive"));
			GridData gridData = new GridData(SWT.LEFT, SWT.TOP, false, false, 10, 1);
			mIsActiveButton.setLayoutData(gridData);

			// Description field.
			mDescriptionLabel = new Label(mEditComposite, SWT.RIGHT);
			mDescriptionField = new Text(mEditComposite, SWT.SINGLE | SWT.BORDER);
			String labelStr = LocaleUtils.getStr("snapnetwork_wizard.edit_page.desc_field");
			createInputField(mDescriptionLabel, mDescriptionField, mEditComposite, labelStr, 1, 4, 5);
			mDescriptionField.setText(mSnapNetwork.getDescription());
			mDescriptionField.addListener(SWT.Modify, mEditListener);

			mEditListener = new Listener() {
				public void handleEvent(Event inEvent) {
					setPageComplete(isPageComplete());
				}
			};

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
	public boolean saveSnapNetwork() {

		boolean result = true;

		Text field;

		if ((result == true) && (mSnapNetwork != null)) {

			mSnapNetwork.setDescription(mDescriptionField.getText());
			mSnapNetwork.setIsActive(mIsActiveButton.getSelection());

			// Save the Snap Network.
			try {
				Util.getSystemDAO().storeSnapNetwork(mSnapNetwork);
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
			if (inObject instanceof SnapNetwork) {

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
			if (inObject instanceof SnapNetwork) {

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
			if (inObject instanceof SnapNetwork) {

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
		if (inObject instanceof SnapNetwork) {
			SnapNetwork snapNetwork = (SnapNetwork) inObject;
			if (snapNetwork.equals(mSnapNetwork)) {
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
