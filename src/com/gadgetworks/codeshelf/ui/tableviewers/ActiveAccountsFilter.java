/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ActiveAccountsFilter.java,v 1.2 2011/01/21 01:12:12 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.tableviewers;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class ActiveAccountsFilter extends ViewerFilter {
	public final boolean select(Viewer inViewer, Object inParent, Object inElement) {
		// Doesn't really do anything yet.
		//return (element instanceof Account);
		return true;
	}

}
