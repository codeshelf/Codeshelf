/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: MainGTK.java,v 1.1 2012/03/16 15:59:10 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;


// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class MainGTK extends MainABC {

	// --------------------------------------------------------------------------
	/**
	 * Default constructor hidden for static class.
	 */
	public MainGTK() {

	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inArgs
	 */
	public static void main(String[] inArgs) {

		System.out.println("user.dir = " + System.getProperty("user.dir"));
		System.out.println("java.class.path = " + System.getProperty("java.class.path"));
		System.out.println("java.library.path = " + System.getProperty("java.library.path"));

		MainABC main = new MainGTK();
		main.mainStart();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void platformSetup() {
	}

}