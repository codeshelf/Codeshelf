/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeShelfNetViewSorter.java,v 1.2 2011/01/24 07:22:42 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.treeviewers;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;
import com.gadgetworks.codeshelf.model.persist.ControlGroup;
import com.gadgetworks.codeshelf.model.persist.PickTag;

public class CodeShelfNetViewSorter extends ViewerComparator {

	public static final String	PICKTAG_MACADDR_PROPERTY	= "MACADDRPROP";

	public final int compare(Viewer inViewer, Object inElement1, Object inElement2) {

		int result = 0;

		if ((inElement1 instanceof CodeShelfNetwork) && (inElement2 instanceof CodeShelfNetwork)) {
			CodeShelfNetwork network1 = (CodeShelfNetwork) inElement1;
			CodeShelfNetwork network2 = (CodeShelfNetwork) inElement2;
			result = network1.getId().toString().compareTo(network2.getId().toString());
		} else if ((inElement1 instanceof ControlGroup) && (inElement2 instanceof ControlGroup)) {
			ControlGroup group1 = (ControlGroup) inElement1;
			ControlGroup group2 = (ControlGroup) inElement2;
			result = group1.getId().toString().compareTo(group2.getId().toString());
		} else if ((inElement1 instanceof PickTag) && (inElement2 instanceof PickTag)) {
			PickTag pickTag1 = (PickTag) inElement1;
			PickTag pickTag2 = (PickTag) inElement2;
			result = pickTag1.getMacAddress().toString().compareTo(pickTag2.getMacAddress().toString());
		} else if (inElement1 instanceof CodeShelfNetwork) {
			if (inElement2 instanceof ControlGroup) {
				result = 1;
			} else if (inElement2 instanceof PickTag) {
				result = 1;
			}
		} else if (inElement1 instanceof ControlGroup) {
			if (inElement2 instanceof CodeShelfNetwork) {
				result = -1;
			} else if (inElement2 instanceof PickTag) {
				result = 1;
			}
		} else if (inElement1 instanceof PickTag) {
			if (inElement2 instanceof CodeShelfNetwork) {
				result = -1;
			} else if (inElement2 instanceof ControlGroup) {
				result = -1;
			}
		} else {
			return 0;
		}

		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerComparator#isSorterProperty(java.lang.Object, java.lang.String)
	 */
	public final boolean isSorterProperty(Object inElement, String inProperty) {
		// For now we accept that all property updates will cause a sort.
		boolean result = false;

		if (inProperty.equals(PICKTAG_MACADDR_PROPERTY)) {
			result = true;
		}

		return result;
	}
}
