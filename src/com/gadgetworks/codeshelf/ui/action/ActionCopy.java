/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: ActionCopy.java,v 1.1 2011/01/21 01:08:21 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import com.gadgetworks.codeshelf.ui.PickTagMgrWindow;

public class ActionCopy extends Action implements ISelectionChangedListener {
	private PickTagMgrWindow	mDeviceDebugWindow;

	public ActionCopy(final PickTagMgrWindow inDeviceDebugWindow) {
		mDeviceDebugWindow = inDeviceDebugWindow;
		setEnabled(false);
		setToolTipText("Copy");
		setText("&Copy");
		setAccelerator(SWT.MOD1 | 'c');
		
		//		setImageDescriptor(ImageDescriptor.createFromURL(Util.findResource("images/add.png")));
		mDeviceDebugWindow = inDeviceDebugWindow;
	}

	public final void run() {
		Clipboard clipboard = new Clipboard(Display.getCurrent());
		TextTransfer textTransfer = TextTransfer.getInstance();

		StringBuffer stringBuffer = new StringBuffer();

		//		IStructuredSelection selection = mDeviceDebugWindow.getTreeSelection();
		//		for (Object object : selection.toArray()) {
		//			stringBuffer.append(object.toString() + "\n");
		//		}
		clipboard.setContents(new Object[] { stringBuffer.toString() }, new Transfer[] { textTransfer });
		clipboard.dispose();
	}

	public final void selectionChanged(SelectionChangedEvent inEvent) {
		setEnabled(false);

		//		IStructuredSelection selection = mDeviceDebugWindow.getTreeSelection();
		//		if (selection.size() > 0) {
		//			setEnabled(true);
		//		}
	}
}
