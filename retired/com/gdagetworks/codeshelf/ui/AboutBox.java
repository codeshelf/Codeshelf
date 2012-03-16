/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AboutBox.java,v 1.1 2012/03/16 15:59:07 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.gadgetworks.codeshelf.application.Util;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class AboutBox {

	//	private static final int	ABOUT_WIDTH_PIXELS	= 300;
	//	private static final int	ABOUT_HEIGHT_PIXELS	= 300;
	private static final int	MAX_ALPHA_VALUE	= 255;

	private boolean				mShouldWait		= true;
	private Image				mImage;
	private Shell				mAboutShell;
	private Button				mOKButton;

	public AboutBox(final Display inDisplay) {

		mOKButton = null;
		final ImageDescriptor imgDesc = Util.getImageRegistry().getDescriptor(Util.CODESHELF_ABOUT_ICON);
		mImage = imgDesc.createImage();
		mAboutShell = new Shell(inDisplay, SWT.ON_TOP | SWT.NO_TRIM | SWT.TRANSPARENT);
		mAboutShell.setLayout(new FillLayout());

		mAboutShell.setBackgroundImage(mImage);
		//shapeAboutAbox();
		displayBasicReleaseInfo();

		Listener listener = new Listener() {
			public void handleEvent(Event inEvent) {

				if ((inEvent.type == SWT.MouseDown) || (inEvent.type == SWT.KeyDown)) {
					mShouldWait = false;
				}
			}
		};
		mAboutShell.addListener(SWT.MouseDown, listener);
		mAboutShell.addListener(SWT.KeyDown, listener);
		mAboutShell.addListener(SWT.MenuDetect, listener);
		if (mOKButton != null) {
			mOKButton.addListener(SWT.Selection, listener);
			mOKButton.addListener(SWT.Modify, listener);
		}

		mAboutShell.setSize(mImage.getBounds().width, mImage.getBounds().height);

		//mAboutShell.pack();
		Rectangle aboutRect = mAboutShell.getBounds();
		Rectangle displayRect = inDisplay.getBounds();
		int x = (displayRect.width - aboutRect.width) / 2;
		int y = (displayRect.height - aboutRect.height) / 2;
		mAboutShell.setLocation(x, y);

	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void aboutOpen() {
		mAboutShell.open();
		while (mShouldWait) {
			if (!Display.getDefault().readAndDispatch())
				Display.getDefault().sleep();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void aboutClose() {
		mAboutShell.close();
		mImage.dispose();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void displayBasicReleaseInfo() {
		mAboutShell.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent inPaintEvent) {
				Rectangle size = mImage.getBounds();
				// Draws the background image.
				inPaintEvent.gc.drawImage(mImage, 0, 0);
//				inPaintEvent.gc.setBackground(Util.getColorRegistry().get(Util.ABOUT_BGND_COLOR));
				inPaintEvent.gc.setForeground(Util.getColorRegistry().get(Util.ABOUT_FGND_COLOR));
				inPaintEvent.gc.drawText("IndutroNet " + Util.getVersionString(), 10, 150, true);
				inPaintEvent.gc.drawText("Copyright 2007-2010, Gadgetworks, LLC All Rights Reserved", 10, 165, true);
				inPaintEvent.gc.drawText("http://www.gadgetworks.com", 10, 180, true);
				inPaintEvent.gc.drawText("Click to dismiss", 10, 200, true);
			}
		});
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void displayFancyReleaseInfo() {
		final Canvas canvas = new Canvas(mAboutShell, SWT.NO_BACKGROUND);
		canvas.setBounds(mImage.getBounds());

		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent inPaintEvent) {
				Rectangle size = mImage.getBounds();
				// Draws the background image.
				inPaintEvent.gc.drawImage(mImage, 0, 0);
				inPaintEvent.gc.setBackground(Util.getColorRegistry().get(Util.ABOUT_BGND_COLOR));
				inPaintEvent.gc.setForeground(Util.getColorRegistry().get(Util.ABOUT_FGND_COLOR));
				inPaintEvent.gc.drawText("CodeShelf" + Util.getVersionString(), 10, 10);
				inPaintEvent.gc.drawText("Copyright 2007-2010, Gadgetworks, LLC All Rights Reserved", 10, 25);
				inPaintEvent.gc.drawText("Use subject to license agreement from installer", 10, 40);
				inPaintEvent.gc.drawText("http://www.gadgetworks.com", 10, 55);
			}
		});

		GC gc = new GC(canvas);
		gc.fillRectangle(mImage.getBounds());
		gc.drawImage(mImage, 0, 0);
		gc.setBackground(Util.getColorRegistry().get(Util.ABOUT_BGND_COLOR));
		gc.setForeground(Util.getColorRegistry().get(Util.ABOUT_FGND_COLOR));
		gc.drawText("CodeShelf", 10, 10);
		gc.drawText("Copyright 2007-2009, Gadgetworks, LLC All Rights Reserved", 10, 25);
		gc.drawText("Use subject to license agreement from installer", 10, 40);
		gc.drawText("http://www.gadgetworks.com", 10, 55);
		gc.dispose();

		mOKButton = new Button(canvas, SWT.PUSH);
		mOKButton.setText("OK");

		Label label = new Label(mAboutShell, SWT.NONE);
		label.setImage(mImage);
		FormLayout layout = new FormLayout();
		mAboutShell.setLayout(layout);
		FormData labelData = new FormData();
		labelData.right = new FormAttachment(100, 0);
		labelData.bottom = new FormAttachment(100, 0);
		label.setLayoutData(labelData);
		FormData koButtonData = new FormData();
		koButtonData.left = new FormAttachment(85, 5);
		koButtonData.right = new FormAttachment(100, -5);
		koButtonData.bottom = new FormAttachment(100, -5);
		mOKButton.setLayoutData(koButtonData);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void shapeAboutAbox() {
		// One way of making the about box the shape of the background picture.
		Region region = new Region();
		final ImageData imageData = mImage.getImageData();
		if (imageData.alphaData != null) {
			Rectangle pixel = new Rectangle(0, 0, 1, 1);
			for (int y = 0; y < imageData.height; y++) {
				for (int x = 0; x < imageData.width; x++) {
					if (imageData.getAlpha(x, y) == MAX_ALPHA_VALUE) {
						pixel.x = imageData.x + x;
						pixel.y = imageData.y + y;
						region.add(pixel);
					}
				}
			}
		} else {
			ImageData mask = imageData.getTransparencyMask();
			Rectangle pixel = new Rectangle(0, 0, 1, 1);
			for (int y = 0; y < mask.height; y++) {
				for (int x = 0; x < mask.width; x++) {
					if (mask.getPixel(x, y) != 0) {
						pixel.x = imageData.x + x;
						pixel.y = imageData.y + y;
						region.add(pixel);
					}
				}
			}
		}
		mAboutShell.setRegion(region);

	}

}
