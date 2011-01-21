/*******************************************************************************
 *  HoobeeNet
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: PickTagViewSorter.java,v 1.1 2011/01/21 01:08:22 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.treeviewers;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import com.gadgetworks.codeshelf.model.persist.PickTag;

public class PickTagViewSorter extends ViewerComparator {
	
	public final String DATOBLOK_GUID_PROPERTY = "GUIDPROP";

	public final int compare(Viewer inViewer, Object inElement1, Object inElement2) {

		int result = 0;

		if (inElement1 instanceof PickTag) {
			PickTag datoBlok1 = (PickTag) inElement1;
			if (inElement2 instanceof PickTag) {
				PickTag datoBlok2 = (PickTag) inElement2;
				result = datoBlok1.getDescription().compareTo(datoBlok2.getDescription());
			} else {
				result = 1;
			}
		} else {
			result = 0;
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
		
		if (inProperty.equals(DATOBLOK_GUID_PROPERTY)) {
			result = true;
		}

		return result;
	}
}
