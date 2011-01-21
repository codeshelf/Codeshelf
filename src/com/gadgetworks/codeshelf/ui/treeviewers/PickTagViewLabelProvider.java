/*******************************************************************************
 *  HoobeeNet
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PickTagViewLabelProvider.java,v 1.2 2011/01/21 01:12:11 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.treeviewers;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.model.persist.PickTag;

public class PickTagViewLabelProvider extends LabelProvider {

	public final String getText(Object inElement) {
		if (inElement instanceof PickTag) {
			return ((PickTag) inElement).getGUID();
		} else {
			return "";
		}
	}

	public final Image getImage(Object inElement) {
		Image result;

		if (inElement instanceof PickTag) {
			if (((PickTag) inElement).getNetworkDeviceState() == NetworkDeviceStateEnum.STARTED) {
				result = Util.getImageRegistry().get(Util.PERSON_ICON);
			} else {
				result = Util.getImageRegistry().get(Util.PERSON_ICON_QUARTER_ALPHA);
			}
		} else {
			result = null;
		}

		return result;
	}
}
