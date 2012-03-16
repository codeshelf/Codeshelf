/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ActionConsole.java,v 1.1 2012/03/16 15:59:10 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.action;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;

import com.gadgetworks.codeshelf.ui.Console;

public final class ActionConsole extends Action implements ShellListener {

	private Console	mConsole;

	public ActionConsole(final Console inConsole) {
		mConsole = inConsole;

		if (inConsole.isOpen()) {
			setText("Hide Console");
		} else {
			setText("Show Console");
		}
		setEnabled(true);
		setToolTipText("Show/Hide Console");
		if (inConsole.isOpen()) {
			setText("&Close Console");
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		if (mConsole.isOpen()) {
			mConsole.closeConsole();
			setText("Show Console");
		} else {
			mConsole.openConsole();
			setText("Hide Console");
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.swt.events.ShellListener#shellActivated(org.eclipse.swt.events.ShellEvent)
	 */
	public void shellActivated(ShellEvent inEvent) {
		setText("Hide Console");
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.swt.events.ShellListener#shellClosed(org.eclipse.swt.events.ShellEvent)
	 */
	public void shellClosed(ShellEvent inEvent) {
		setText("Show Console");
		inEvent.doit = false;
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

}
