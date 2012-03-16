/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Console.java,v 1.1 2012/03/16 15:59:07 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Decorations;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.gadgetworks.codeshelf.application.CodeShelfApplication;
import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.ui.action.ActionConsole;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class Console implements ShellListener, IStructuredContentProvider {

	private static final Log	LOGGER					= LogFactory.getLog(Console.class);

	private static final int	UPDATE_SLEEP_MILLIS		= 500;
	private static final Point	DEFAULT_SIZE			= new Point(1024, 200);
	private static final Point	DEFAULT_LOCATION		= new Point(20, 400);
	private static final String	CONSOLE_THREAD_NAME		= "Console Background";
	private static final long	CONSOLE_SLEEP_MILLIS	= 1000;
	private static final int	CONSOLE_MAX_LINES		= 250;
	private static final int	CONSOLE_MAX_BYTES		= 500;

	private Shell				mShell;
	private Action				mCutAction;
	private Action				mCopyAction;
	private Action				mPasteAction;
	private ActionConsole		mConsoleAction;
	//	private Action				mPrefsAction;
	//	private Action				mAboutAction;
	//	private Action				mExitAction;
	private Thread				mUpdateThread;
	private File				mFile;
	private Reader				mFileReader;
	private BufferedReader		mBufferedFileReader;
	private ListViewer			mListViewer;
	private List<String>		mConsoleLines;
	private boolean				mShouldRun				= true;
	private boolean				mConsoleOpen;

	// --------------------------------------------------------------------------
	/**
	 *  @param inParentShell
	 */
	public Console(final Shell inParentShell, final CodeShelfApplication inCodeShelfApplication) {

		LOGGER.debug("Creating console");

		mConsoleOpen = false;
		mShell = new Shell(SWT.SHELL_TRIM);
		mShell.setEnabled(true);
		mShell.setSize(DEFAULT_SIZE);
		mShell.setLocation(DEFAULT_LOCATION);
		mShell.setText(LocaleUtils.getStr("console.window.name"));
		mShell.setLayout(new FillLayout(SWT.HORIZONTAL));

		mConsoleLines = new ArrayList<String>();

		mListViewer = new ListViewer(mShell, SWT.H_SCROLL | SWT.V_SCROLL);
		mListViewer.setLabelProvider(new LabelProvider());
		mListViewer.setContentProvider(this);
		mListViewer.setInput(mConsoleLines);

		//		mPrefsAction = new Action("&Prefs\tCtrl+,") {
		//			public void run() {
		//				inCodeShelfApplication.editPreferences();
		//			}
		//		};
		//		mAboutAction = new Action("&About\t") {
		//			public void run() {
		//				Util.showAbout();
		//			}
		//		};
		//		mExitAction = new Action("&Exit\tCtrl+X") {
		//			public void run() {
		//				mShell.close();
		//			}
		//		};
		
		// Cut action.
		mCutAction = new Action("Cut") {
			public void run() {

			}
		};
		mCutAction.setEnabled(false);
		mCutAction.setAccelerator(SWT.MOD1 | 'x');

		// Copy action.
		mCopyAction = new Action("Copy") {
			public void run() {

			}
		};
		mCopyAction.setEnabled(true);
		mCopyAction.setAccelerator(SWT.MOD1 | 'c');

		// Past action.
		mPasteAction = new Action("Paste") {
			public void run() {

			}
		};
		mPasteAction.setAccelerator(SWT.MOD1 | 'v');
		mPasteAction.setEnabled(false);

		mConsoleAction = new ActionConsole(this);
		this.addShellListener(mConsoleAction);

		mShell.setMenuBar(createMenuManager().createMenuBar((Decorations) mShell));

	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void startConsole() {
		String appLogFilePath = Util.getApplicationLogDirPath() + System.getProperty("file.separator") + "codeshelf.log";
		mFile = new File(appLogFilePath);
		try {
			mFileReader = new FileReader(mFile);
			mBufferedFileReader = new BufferedReader(mFileReader);
			long skipBytes = mFile.length() - CONSOLE_MAX_BYTES;
			if (skipBytes < 0)
				skipBytes = 0;
			mBufferedFileReader.skip(skipBytes);
			mConsoleLines.clear();
		} catch (IOException e) {
			LOGGER.error("", e);
		}

		mUpdateThread = new Thread(new Runnable() {
			public void run() {
				updateConsole();
				// This gets called after stopConsole().
				mFile = null;
				mFileReader = null;
				mBufferedFileReader = null;
				mConsoleLines.clear();
			}
		}, CONSOLE_THREAD_NAME);
		mUpdateThread.setDaemon(true);
		mUpdateThread.start();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void stopConsole() {
		mShouldRun = false;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void openConsole() {
		if (!mShell.isDisposed()) {
//			try {
//				mFileReader = new FileReader(mFile);
//				mBufferedFileReader = new BufferedReader(mFileReader);
//				mBufferedFileReader.skip(mFile.length() - CONSOLE_MAX_BYTES);
//				mConsoleLines.clear();
//			} catch (IOException e) {
//				LOGGER.error("", e);
//			}
			startConsole();
			mShell.open();
			mShell.addShellListener(this);
			mShell.setVisible(true);
			mConsoleOpen = true;
			mListViewer.setInput(mConsoleLines);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void closeConsole() {
		mConsoleOpen = false;
		if (!mShell.isDisposed()) {
			mShell.setVisible(false);
			stopConsole();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public boolean isOpen() {
		if (mShell == null) {
			return false;
		} else {
			return mShell.isVisible();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inShellListener
	 */
	public void addShellListener(ShellListener inShellListener) {
		if (mShell != null)
			mShell.addShellListener(inShellListener);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void updateConsole() {

		final Display display = Display.getDefault();
		try {
			while (mShouldRun) {
				if (!mConsoleOpen) {
					Thread.sleep(UPDATE_SLEEP_MILLIS);
				} else {
					String updateStr = mBufferedFileReader.readLine();
					if (updateStr != null) {
						mConsoleLines.add(updateStr);
						if (mConsoleLines.size() > CONSOLE_MAX_LINES) {
							mConsoleLines.remove(0);
						}

						display.syncExec(new Runnable() {
							public void run() {
								mListViewer.refresh();
							}
						});
					} else {
						Thread.sleep(CONSOLE_SLEEP_MILLIS);
					}
				}

			}
			mBufferedFileReader.close();
		} catch (IOException inException) {
			LOGGER.error("", inException);
		} catch (InterruptedException inException) {
			Thread.currentThread().interrupt();
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.window.ApplicationWindow#createMenuManager()
	 */
	protected MenuManager createMenuManager() {
		MenuManager barMenu = new MenuManager("");

		//		MenuManager fileMenu = new MenuManager(LocaleUtils.getStr("menu.file.text"));
		MenuManager editMenu = new MenuManager(LocaleUtils.getStr("menu.edit.text"));
		MenuManager accountsMenu = new MenuManager(LocaleUtils.getStr("menu.accounts.text"));
		MenuManager debugMenu = new MenuManager(LocaleUtils.getStr("menu.debug.text"));
		MenuManager helpMenu = new MenuManager(LocaleUtils.getStr("menu.help.text"));

		//		barMenu.add(fileMenu);
		barMenu.add(editMenu);
		barMenu.add(accountsMenu);
		barMenu.add(debugMenu);
		barMenu.add(helpMenu);

		// Setup the file menu.
		if (!Util.isOSX()) {
			//			fileMenu.add(mExitAction);
		}
		debugMenu.add(mConsoleAction);

		// Setup the edit menu.
		editMenu.add(mCopyAction);
		editMenu.add(mCutAction);
		editMenu.add(mPasteAction);
		if (!Util.isOSX()) {
			//			editMenu.add(mPrefsAction);
		}

		// Setup the help menu
		if (!Util.isOSX()) {
			//			helpMenu.add(mAboutAction);
		}

		return barMenu;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.swt.events.ShellListener#shellActivated(org.eclipse.swt.events.ShellEvent)
	 */
	public void shellActivated(ShellEvent inEvent) {
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.swt.events.ShellListener#shellClosed(org.eclipse.swt.events.ShellEvent)
	 */
	public void shellClosed(ShellEvent inEvent) {
		this.closeConsole();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.swt.events.ShellListener#shellDeactivated(org.eclipse.swt.events.ShellEvent)
	 */
	public void shellDeactivated(ShellEvent inEvent) {
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.swt.events.ShellListener#shellDeiconified(org.eclipse.swt.events.ShellEvent)
	 */
	public void shellDeiconified(ShellEvent inEvent) {
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.swt.events.ShellListener#shellIconified(org.eclipse.swt.events.ShellEvent)
	 */
	public void shellIconified(ShellEvent inEvent) {
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inInputElement) {
		return mConsoleLines.toArray();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer inViewer, Object inOldInput, Object inNewInput) {
	}

}
