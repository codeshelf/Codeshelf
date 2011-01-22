/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeShelfNetViewDecoratedLabelProvider.java,v 1.2 2011/01/22 07:58:31 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.ui.treeviewers;

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.controller.NetworkDeviceStateEnum;
import com.gadgetworks.codeshelf.model.persist.ControlGroup;
import com.gadgetworks.codeshelf.model.persist.PickTag;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class CodeShelfNetViewDecoratedLabelProvider extends DecoratingLabelProvider implements
	ITableLabelProvider,
	ITableColorProvider {

	public static final int	ID_COL		= 0;
	public static final int	COLOR_COL	= 0;
	public static final int	DESC_COL	= 1;
	public static final int	DETAILS_COL	= 2;

	private Tree			mTree;

	// --------------------------------------------------------------------------
	/**
	 *  @param inProvider
	 *  @param inDecorator
	 */
	public CodeShelfNetViewDecoratedLabelProvider(final Tree inTree,
		final ILabelProvider inProvider,
		final ILabelDecorator inDecorator) {
		super(inProvider, inDecorator);

		mTree = inTree;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
	 */
	public Image getColumnImage(final Object inElement, final int inColumnIndex) {
		Image result = null;

		TreeColumn column = mTree.getColumn(inColumnIndex);

		if (column.getData().equals(ID_COL)) {
			if (inElement instanceof PickTag) {
				PickTag pickTag = (PickTag) inElement;
				if (pickTag.getNetworkDeviceState() != NetworkDeviceStateEnum.STARTED) {
					result = Util.getImageRegistry().get(Util.PICKTAG_ICON_QUARTER_ALPHA);
				} else {
					result = Util.getImageRegistry().get(Util.PICKTAG_ICON_GREEN);
				}
			}
		}

		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	public String getColumnText(final Object inElement, final int inColumnIndex) {

		String displayStr = "";
		TreeColumn column = mTree.getColumn(inColumnIndex);

		if (inElement instanceof CodeShelfNetwork) {
			CodeShelfNetwork codeShelfNetwork = (CodeShelfNetwork) inElement;
			if (column.getData().equals(ID_COL)) {
				displayStr = codeShelfNetwork.getNetworkId().toString();
			} else if (column.getData().equals(DESC_COL)) {
				displayStr = codeShelfNetwork.getDescription();
			} else if (column.getData().equals(DETAILS_COL)) {
				//				StringBuffer buf = new StringBuffer();
				//				for (SearchParameter parameter : ruleset.getSearchParameters()) {
				//					buf.append(parameter.getId() + "->" + parameter.getValue() + " ");
				//				}
				//				displayStr = buf.toString();
				displayStr = "";
			}
		} else if (inElement instanceof ControlGroup) {
			ControlGroup controlGroup = (ControlGroup) inElement;
			if (column.getData().equals(ID_COL)) {
				displayStr = controlGroup.getDescription();
			} else if (column.getData().equals(DESC_COL)) {
				displayStr = "";
			} else if (column.getData().equals(DETAILS_COL)) {
				//				StringBuffer buf = new StringBuffer();
				//				for (SearchParameter parameter : ruleset.getSearchParameters()) {
				//					buf.append(parameter.getId() + "->" + parameter.getValue() + " ");
				//				}
				//				displayStr = buf.toString();
				displayStr = "";
			}
		} else if (inElement instanceof PickTag) {
			PickTag pickTag = (PickTag) inElement;
			if (column.getData().equals(ID_COL)) {
				displayStr = pickTag.getGUID();
			} else if (column.getData().equals(DESC_COL)) {
				displayStr = pickTag.getDescription();
			} else if (column.getData().equals(DETAILS_COL)) {
				displayStr = "firmware=" + pickTag.getSWRevision() + " hw=" + pickTag.getHWDesc();
			}
		}
		
		return displayStr;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableColorProvider#getForeground(java.lang.Object, int)
	 */
	public Color getForeground(Object inElement, int inColumnIndex) {

		Color color = Display.getCurrent().getSystemColor(SWT.COLOR_LIST_FOREGROUND);

		if (inElement instanceof PickTag) {
			if (((PickTag) inElement).getNetworkDeviceState() != NetworkDeviceStateEnum.STARTED) {
				color = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
			}
		} else if (inElement instanceof CodeShelfNetwork) {
			CodeShelfNetwork codeShelfNetwork = (CodeShelfNetwork) inElement;
			if (!codeShelfNetwork.getIsActive()) {
				color = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
			}
		} else if (inElement instanceof ControlGroup) {
			ControlGroup controlGroup = (ControlGroup) inElement;
			if (!controlGroup.getIsActive()) {
				color = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
			}
		}

		return color;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableColorProvider#getBackground(java.lang.Object, int)
	 */
	public Color getBackground(Object inElement, int inColumnIndex) {
		Color result = Util.getColorRegistry().get(Util.BACKGROUND_COLOR);
		//		if ((inColumnIndex == COLOR_COL) && (inElement instanceof PickTagModule)) {
		//			PickTagModule ruleset = (PickTagModule) inElement;
		//			result = new Color(Display.getCurrent(), ruleset.getPickTagBehavior().getRedValue(), ruleset.getPickTagBehavior()
		//				.getGreenValue(), ruleset.getPickTagBehavior().getBlueValue());
		//		}
		return result;
	}

}
