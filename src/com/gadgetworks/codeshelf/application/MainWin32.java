/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: MainWin32.java,v 1.1 2011/01/21 01:08:21 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;


// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class MainWin32 extends MainABC {

	// --------------------------------------------------------------------------
	/**
	 * Default constructor hidden for static class.
	 */
	public MainWin32() {

	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inArgs
	 */
	public static void main(String[] inArgs) {

//		System.out.println("user.dir = " + System.getProperty("user.dir"));
//		System.out.println("java.class.path = " + System.getProperty("java.class.path"));
//		System.out.println("java.library.path = " + System.getProperty("java.library.path"));

		MainABC main = new MainWin32();
		main.mainStart();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void platformSetup() {
	}

}