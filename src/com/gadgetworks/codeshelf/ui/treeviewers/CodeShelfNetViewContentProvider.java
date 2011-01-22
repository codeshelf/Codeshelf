/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeShelfNetViewContentProvider.java,v 1.1 2011/01/22 01:04:39 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.treeviewers;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.model.persist.ControlGroup;
import com.gadgetworks.codeshelf.model.persist.PickTag;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;

public class CodeShelfNetViewContentProvider implements ITreeContentProvider {

	public static final String	PICKTAGVIEW_ROOT	= "picktagroot";

	public CodeShelfNetViewContentProvider() {

	}

	public final Object[] getElements(Object inElement) {

		Object[] result = new Object[0];

		if (PICKTAGVIEW_ROOT.equals(inElement)) {
			result = Util.getSystemDAO().getCodeShelfNetworks().toArray();
		} else if (inElement instanceof CodeShelfNetwork) {
			result = ((CodeShelfNetwork) inElement).getControlGroups().toArray();
		} else if (inElement instanceof ControlGroup) {
			result = ((ControlGroup) inElement).getPickTags().toArray();
		}
		return result;
	}

	public void dispose() {
	}

	public void inputChanged(Viewer inViewer, Object inOldObject, Object inNewObject) {
	}

	public final Object[] getChildren(Object inParentElement) {

		Object[] result = null;

		if (inParentElement == PICKTAGVIEW_ROOT) {
			result = Util.getSystemDAO().getCodeShelfNetworks().toArray();
		} else if (inParentElement instanceof CodeShelfNetwork) {
			result = ((CodeShelfNetwork) inParentElement).getControlGroups().toArray();
		} else if (inParentElement instanceof ControlGroup) {
			result = ((ControlGroup) inParentElement).getPickTags().toArray();
		}

		return result;
	}

	public final Object getParent(Object inElement) {
		Object result = null;

		if (inElement instanceof CodeShelfNetwork) {
			result = null;
		} else if (inElement instanceof ControlGroup) {
			result = ((ControlGroup) inElement).getParentCodeShelfNetwork();
		} else if (inElement instanceof PickTag) {
			result = ((PickTag) inElement).getParentControlGroup();
		}
		return result;
	}

	public final boolean hasChildren(Object inElement) {
		boolean result = false;

		if (inElement == PICKTAGVIEW_ROOT) {
			result = true;
		} else if (inElement instanceof CodeShelfNetwork) {
			result = ((CodeShelfNetwork) inElement).getControlGroups().size() > 0;
		} else if (inElement instanceof ControlGroup) {
			result = ((ControlGroup) inElement).getPickTags().size() > 0;
		}

		return result;
	}

}
