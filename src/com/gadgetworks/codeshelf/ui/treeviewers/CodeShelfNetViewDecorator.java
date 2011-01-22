/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeShelfNetViewDecorator.java,v 1.1 2011/01/22 01:04:39 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.treeviewers;

import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public class CodeShelfNetViewDecorator extends LabelProvider implements ILabelDecorator {
	public CodeShelfNetViewDecorator() {
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
