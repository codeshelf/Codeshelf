/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: ActionPaste.java,v 1.1 2011/01/21 01:08:21 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.ui.PickTagMgrWindow;

public class ActionPaste extends Action implements ISelectionChangedListener {
	private PickTagMgrWindow	mDeviceDebugWindow;

	public ActionPaste(final PickTagMgrWindow inDeviceDebugWindow) {
		mDeviceDebugWindow = inDeviceDebugWindow;
		setEnabled(false);
		setToolTipText("Paste");
		setText("Paste");
		setAccelerator(SWT.MOD1 | 'v');

		//		setImageDescriptor(ImageDescriptor.createFromURL(Util.findResource("images/add.png")));
		mDeviceDebugWindow = inDeviceDebugWindow;
	}

	public final void run() {
		//		IStructuredSelection selection = mDeviceDebugWindow.getTreeSelection();
		//		if (selection.size() == 0) {
		//		} else if (selection.size() != 1) {
		//		} else {
		//		}		
	}

	public final void selectionChanged(SelectionChangedEvent inEvent) {
		setEnabled(false);

		//		IStructuredSelection selection = mDeviceDebugWindow.getTreeSelection();
		//		if (selection.size() == 0) {
		//		} else if (selection.size() != 1) {
		//		} else {
		//		}
	}
}
