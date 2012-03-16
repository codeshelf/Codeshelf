/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ActiveAccountsFilter.java,v 1.1 2012/03/16 15:59:08 jeffw Exp $
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
