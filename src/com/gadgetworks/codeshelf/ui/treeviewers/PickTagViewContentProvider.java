/*******************************************************************************
 *  HoobeeNet
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PickTagViewContentProvider.java,v 1.2 2011/01/21 01:12:11 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.treeviewers;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.model.persist.PickTag;
import com.gadgetworks.codeshelf.model.persist.PickTagModule;

public class PickTagViewContentProvider implements ITreeContentProvider {

	public static final String	DATOBLOK_ROOT	= "datoblokroot";

	public PickTagViewContentProvider() {

	}

	public final Object[] getElements(Object inElement) {

		Object[] result = new Object[0];

		if (DATOBLOK_ROOT.equals(inElement)) {
			result = Util.getSystemDAO().getPickTags().toArray();
		} else if (inElement instanceof PickTag) {
			result = ((PickTag) inElement).getPickTagModules().toArray();
		} else if (inElement instanceof PickTagModule) {
		}
		return result;
	}

	public void dispose() {
	}

	public void inputChanged(Viewer inViewer, Object inOldObject, Object inNewObject) {
	}

	public final Object[] getChildren(Object inParentElement) {

		Object[] result = null;

		if (inParentElement == DATOBLOK_ROOT) {
			result = Util.getSystemDAO().getPickTags().toArray();
		} else if (inParentElement instanceof PickTag) {
			result = ((PickTag) inParentElement).getPickTagModules().toArray();
		} else if (inParentElement instanceof PickTagModule) {
		}

		return result;
	}

	public final Object getParent(Object inElement) {
		Object result = null;

		if (inElement instanceof PickTag) {
			result = null;
		} else if (inElement instanceof PickTagModule) {
			result = ((PickTagModule) inElement).getParentPickTag();
		}
		return result;
	}

	public final boolean hasChildren(Object inElement) {
		boolean result = false;

		if (inElement == DATOBLOK_ROOT) {
			result = true;
		} else if (inElement instanceof PickTag) {
			result = ((PickTag) inElement).getPickTagModules().size() > 0;
		} else if (inElement instanceof PickTagModule) {
		}

		return result;
	}

}
