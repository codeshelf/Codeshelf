/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: OwnerBoxDecorator.java,v 1.1 2011/01/21 01:08:21 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.tableviewers;

import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public class OwnerBoxDecorator extends LabelProvider implements ILabelDecorator {
	public OwnerBoxDecorator() {
		super();
	}

	// Method to decorate Image 
	public final Image decorateImage(Image inImage, Object inObject) {
		// Return null to specify no decoration
		return null;
	}

	// Method to decorate Text
	public final String decorateText(String inLabel, Object inObject) {
		// return null to specify no decoration
		return null;
	}
}
