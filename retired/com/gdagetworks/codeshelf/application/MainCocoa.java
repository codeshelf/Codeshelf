/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: MainCocoa.java,v 1.1 2012/03/16 15:59:10 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.C;
import org.eclipse.swt.internal.Callback;
import org.eclipse.swt.internal.cocoa.NSApplication;
import org.eclipse.swt.internal.cocoa.NSMenu;
import org.eclipse.swt.internal.cocoa.NSMenuItem;
import org.eclipse.swt.internal.cocoa.NSString;
import org.eclipse.swt.internal.cocoa.OS;

import com.gadgetworks.codeshelf.ui.LocaleUtils;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class MainCocoa extends MainABC {

	private static final int			ABOUT_MENUITEM			= 0;
	private static final int			PREFS_MENUITEM			= 2;
	private static final int			SERVICE_MENUITEM		= 4;
	private static final int			HIDE_APP_MENUITEM		= 6;
	private static final int			QUIT_MENUITEM			= 10;

	private static Callback				proc3Args;
	private static final String			SWT_OBJECT				= "SWT_OBJECT";
	private static final int			TOOLBAR_BUTTON_CLICKED	= OS.sel_registerName("toolbarButtonClicked:");
	private static final int			QUIT_MENU_SELECTED		= OS.sel_registerName("quitMenuItemSelected:");
	private static final int			PREFS_MENU_SELECTED		= OS.sel_registerName("preferencesMenuItemSelected:");
	private static final int			ABOUT_MENU_SELECETED	= OS.sel_registerName("aboutMenuItemSelected:");

	static {
		String className = "SWTCocoaEnhancerDelegate"; //$NON-NLS-1$

		// TODO: These should either move out of Display or be accessible to this class.
		String types = "*";
		int size = C.PTR_SIZEOF, align = C.PTR_SIZEOF == 4 ? 2 : 3;

		Class<MainCocoa> clazz = MainCocoa.class;

		proc3Args = new Callback(clazz, "actionProc", 3);
		int proc3 = proc3Args.getAddress();
		if (proc3 == 0)
			SWT.error(SWT.ERROR_NO_MORE_CALLBACKS);

		int cls = OS.objc_allocateClassPair(OS.class_NSObject, className, 0);
		OS.class_addIvar(cls, SWT_OBJECT.getBytes(), size, (byte) align, types.getBytes());

		// Add the action callback
		OS.class_addMethod(cls, TOOLBAR_BUTTON_CLICKED, proc3, "@:@");
		OS.class_addMethod(cls, QUIT_MENU_SELECTED, proc3, "@:@");
		OS.class_addMethod(cls, PREFS_MENU_SELECTED, proc3, "@:@");
		OS.class_addMethod(cls, ABOUT_MENU_SELECETED, proc3, "@:@");

		OS.objc_registerClassPair(cls);
	}

	private static final String			RESOURCE_BUNDLE			= MainCocoa.class.getPackage().getName() + ".Messages"; //$NON-NLS-1$

	private SWTCocoaEnhancerDelegate	mDelegate;
	private String						mAboutActionName;
	private String						mQuitActionName;
	private String						mHideActionName;

	private int							mDelegateJniRef;

	public MainCocoa() {

	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inArgs
	 */
	public static void main(String[] inArgs) {
		MainABC main = new MainCocoa();
		main.mainStart();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void platformSetup() {
		mDelegate = new SWTCocoaEnhancerDelegate();
		mDelegate.alloc().init();
		mDelegateJniRef = OS.NewGlobalRef(MainCocoa.this);
		if (mDelegateJniRef == 0)
			SWT.error(SWT.ERROR_NO_HANDLES);
		OS.object_setInstanceVariable(mDelegate.id, SWT_OBJECT.getBytes(), mDelegateJniRef);

		String productName = "CodeShelf";
		String format = LocaleUtils.getStr("AboutAction.format");

		if (format != null)
			mAboutActionName = MessageFormat.format(format, new Object[] { productName });

		if (mAboutActionName == null)
			mAboutActionName = LocaleUtils.getStr("AboutAction.name");

		if (mAboutActionName == null)
			mAboutActionName = "About";

		// prime the format Hide <app name>
		format = LocaleUtils.getStr("HideAction.format");
		if (format != null)
			mHideActionName = MessageFormat.format(format, new Object[] { productName });

		// prime the format Quit <app name>
		format = LocaleUtils.getStr("QuitAction.format");
		if (format != null)
			mQuitActionName = MessageFormat.format(format, new Object[] { productName });

		hookApplicationMenu();
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inDisplay
	 *  @param inAboutName
	 */
	void hookApplicationMenu() {
		// create About Eclipse menu command
		NSMenu mainMenu = NSApplication.sharedApplication().mainMenu();
		NSMenu appMenu = mainMenu.itemAtIndex(0).submenu();

		// add the about action
		NSMenuItem aboutMenuItem = appMenu.itemAtIndex(ABOUT_MENUITEM);
		aboutMenuItem.setTitle(NSString.stringWith(mAboutActionName));

		// rename the hide action if we have an override string
		if (mHideActionName != null) {
			NSMenuItem hideMenuItem = appMenu.itemAtIndex(HIDE_APP_MENUITEM);
			hideMenuItem.setTitle(NSString.stringWith(mHideActionName));
		}

		// rename the quit action if we have an override string
		if (mQuitActionName != null) {
			NSMenuItem quitMenuItem = appMenu.itemAtIndex(QUIT_MENUITEM);
			quitMenuItem.setTitle(NSString.stringWith(mQuitActionName));
		}

		// enable quit menu
		appMenu.itemAtIndex(QUIT_MENUITEM).setEnabled(true);

		// enable pref menu
		appMenu.itemAtIndex(PREFS_MENUITEM).setEnabled(true);

		// disable services menu
		appMenu.itemAtIndex(SERVICE_MENUITEM).setEnabled(false);

		// Register as a target on the prefs and quit items.
		appMenu.itemAtIndex(QUIT_MENUITEM).setTarget(mDelegate);
		appMenu.itemAtIndex(QUIT_MENUITEM).setAction(QUIT_MENU_SELECTED);
		appMenu.itemAtIndex(PREFS_MENUITEM).setTarget(mDelegate);
		appMenu.itemAtIndex(PREFS_MENUITEM).setAction(PREFS_MENU_SELECTED);
		appMenu.itemAtIndex(ABOUT_MENUITEM).setTarget(mDelegate);
		appMenu.itemAtIndex(ABOUT_MENUITEM).setAction(ABOUT_MENU_SELECETED);
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inID
	 *  @param inSelection
	 *  @param inArg0
	 *  @return
	 */
	static int actionProc(int inID, int inSelection, int inArg0) {
		int[] jniRef = new int[1];
		OS.object_getInstanceVariable(inID, SWT_OBJECT.getBytes(), jniRef);
		if (jniRef[0] == 0)
			return 0;

		MainCocoa delegate = (MainCocoa) OS.JNIGetObject(jniRef[0]);

		if (inSelection == TOOLBAR_BUTTON_CLICKED) {
			//NSControl source = new NSControl(arg0);
			//delegate.toolbarButtonClicked(source);
		} else if (inSelection == QUIT_MENU_SELECTED) {
			delegate.quitMenuItemSelected();
		} else if (inSelection == PREFS_MENU_SELECTED) {
			delegate.preferencesMenuItemSelected();
		} else if (inSelection == ABOUT_MENU_SELECETED) {
			delegate.aboutMenuItemSelected();
		}

		return 0;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	void quitMenuItemSelected() {
		// We don't want the user to be able to quit the application.
		mApplication.stopApplication();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	void preferencesMenuItemSelected() {
		mApplication.editPreferences();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	void aboutMenuItemSelected() {
		Util.showAbout();
	}

}