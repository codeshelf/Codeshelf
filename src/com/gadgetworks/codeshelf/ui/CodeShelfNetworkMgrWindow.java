/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeShelfNetworkMgrWindow.java,v 1.3 2011/12/29 09:15:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.gadgetworks.codeshelf.application.CodeShelfApplication;
import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.model.dao.DaoManager;
import com.gadgetworks.codeshelf.model.dao.IDAOListener;
import com.gadgetworks.codeshelf.ui.action.ActionConsole;
import com.gadgetworks.codeshelf.ui.treeviewers.CodeShelfNetView;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */

public final class CodeShelfNetworkMgrWindow extends ApplicationWindow implements IDAOListener {

	public static final Point		DEFAULT_SIZE		= new Point(640, 320);
	public static final Point		DEFAULT_LOCATION	= new Point(10, 350);

	private static final String		SHELL_NAME			= "Device Debug";

	private static final Log		LOGGER				= LogFactory.getLog(CodeShelfNetworkMgrWindow.class);

	private CodeShelfApplication	mApplication;
	private CodeShelfNetView		mCondeShelfNetworkView;
	private Shell					mShell;

	private ActionConsole			mConsoleAction;
	private Action					mPrefsAction;
	private Action					mAboutAction;
	private Action					mExitAction;

	// --------------------------------------------------------------------------
	/**
	 */
	public CodeShelfNetworkMgrWindow(final Shell inShell, final CodeShelfApplication inApplication) {

		super(null);

		mApplication = inApplication;

		mPrefsAction = new Action("&Prefs\tCtrl+,") {
			public void run() {
				mApplication.editPreferences();
			}
		};
		mAboutAction = new Action("&About\t") {
			public void run() {
				Util.showAbout();
			}
		};
		mExitAction = new Action("&Exit\tCtrl+X") {
			public void run() {
				DaoManager.gDaoManager.unregisterDAOListener(mCondeShelfNetworkView);
				mApplication.stopApplication();
			}
		};
		//		mCutAction = new ActionCut(this);
		//		mCopyAction = new ActionCopy(this);
		//		mPasteAction = new ActionPaste(this);

		mConsoleAction = new ActionConsole(Util.getConsole());
		Util.getConsole().addShellListener(mConsoleAction);

		//		mStartChatAction = new ActionStartChat(this);
		//		mStartCallAction = new ActionStartCall(this);

		inShell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent inEvent) {
				// Perform a system shutdown.
				//mApplication.stopApplication();
			}
		});

		//addStatusLine();
		addMenuBar();
		//addToolBar(SWT.FLAT | SWT.WRAP);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite inParentShell) {

		mShell = (Shell) inParentShell;
		mShell.setText(LocaleUtils.getStr("codeshelfview.window.title"));
		mShell.setSize(DEFAULT_SIZE);

		Composite composite = new Composite(mShell, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

		mCondeShelfNetworkView = new CodeShelfNetView(composite, mApplication.getController(), SWT.BORDER);
		DaoManager.gDaoManager.registerDAOListener(mCondeShelfNetworkView);

		return composite;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.window.ApplicationWindow#createMenuManager()
	 */
	protected MenuManager createMenuManager() {
		MenuManager barMenu = new MenuManager("");

		MenuManager fileMenu = new MenuManager(LocaleUtils.getStr("menu.file.text"));
		MenuManager editMenu = new MenuManager(LocaleUtils.getStr("menu.edit.text"));
		MenuManager debugMenu = new MenuManager(LocaleUtils.getStr("menu.debug.text"));
		MenuManager helpMenu = new MenuManager(LocaleUtils.getStr("menu.help.text"));

		barMenu.add(fileMenu);
		barMenu.add(editMenu);
		barMenu.add(debugMenu);
		barMenu.add(helpMenu);

		// Setup the file menu.
		if (!Util.isOSX()) {
			fileMenu.add(mExitAction);
		}
		debugMenu.add(mConsoleAction);

		// Setup the edit menu.
		if (!Util.isOSX()) {
			editMenu.add(new Separator());
			editMenu.add(mPrefsAction);
		}

		// Setup the help menu
		if (!Util.isOSX()) {
			helpMenu.add(mAboutAction);
		}

		return barMenu;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.window.ApplicationWindow#createToolBarManager(int)
	 */
	protected ToolBarManager createToolBarManager(int inStyle) {

		ToolBarManager toolbarMgr = new ToolBarManager(inStyle);

		//toolbarMgr.add(mStartChatAction);
		//		toolbarMgr.add(mStartCallAction);
		//		toolbarMgr.add(mStartChatAction);
		toolbarMgr.add(mPrefsAction);
		//		toolbarMgr.add(mHelpAction);

		return toolbarMgr;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.window.ApplicationWindow#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	protected void configureShell(Shell inShell) {
		super.configureShell(inShell);
		inShell.setText(SHELL_NAME);
	}

	/* --------------------------------------------------------------------------
	/**
	 * 
	 */
	//	public void handleEvents() {
	//		while (!mShell.isDisposed()) {
	//			try {
	//				if (!mDisplay.readAndDispatch()) {
	//					mDisplay.sleep();
	//					try {
	//						Thread.sleep(10);
	//					} catch (InterruptedException e) {
	//						LOGGER.error("", e);
	//					}
	//				} else {
	//
	//				}
	//			} catch (SWTException inSWTException) {
	//				LOGGER.error("Caught SWT exception", inSWTException);
	//			} catch (RuntimeException inRuntimeException) {
	//				LOGGER.error("Caught runtime exception", inRuntimeException);
	//			}
	//		}
	//	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#handleShellCloseEvent()
	 */
	protected void handleShellCloseEvent() {

		// Perform a system shutdown.
		//stopApplication();

		DaoManager.gDaoManager.unregisterDAOListener(mCondeShelfNetworkView);

		//mShell.setMinimized(true);

		// Then do the normal close event behavior.
		super.handleShellCloseEvent();

	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inObject
	 *  @return
	 */
	private boolean doesDAOObjectApply(final Object inObject) {
		boolean result = false;

		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IDAOListener#objectAdded(java.lang.Object)
	 */
	public void objectAdded(Object inObject) {
		if (doesDAOObjectApply(inObject)) {
			LOGGER.error("The application owner account was added!");
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IDAOListener#objectDeleted(java.lang.Object)
	 */
	public void objectDeleted(Object inObject) {
		if (doesDAOObjectApply(inObject)) {
			LOGGER.error("The application owner account was delete!");
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.dao.IDAOListener#objectUpdated(java.lang.Object)
	 */
	public void objectUpdated(Object inObject) {
		if (doesDAOObjectApply(inObject)) {
		}
	}

}
