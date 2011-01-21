/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: SystemBeanFactory.java,v 1.2 2011/01/21 01:12:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.application;


// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class SystemBeanFactory {

	//private static ClassPathXmlApplicationContext	mFactory;

	// --------------------------------------------------------------------------
	/**
	 * 	Create the bean factory for the Spring Framework.
	 */
	private SystemBeanFactory() {
		setupFactory();
	}

	private static void setupFactory() {
		Thread.currentThread().setContextClassLoader(SystemBeanFactory.class.getClassLoader());

//		URL escURL = SystemBeanFactory.class.getClassLoader().getResource("conf/CodeShelfContext.xml");
//		URL flyweightURL = SystemBeanFactory.class.getClassLoader().getResource("conf/FlyWeightContext.xml");
//		mFactory = new ClassPathXmlApplicationContext(new String[] { escURL.toString(), flyweightURL.toString() });
	}

	// --------------------------------------------------------------------------
	/**
	 *  Return the bean factory reference.
	 *  @return	The bean factory that builds all of the beans for the system.
	 */
	//	public static XmlBeanFactory getXmlBeanFactory() {
	//		if (mFactory == null)
	//			setupFactory();
	//
	//		return mFactory;
	//	}
	// --------------------------------------------------------------------------
	/**
	 *  The primary mode of getting beans for the system.
	 *  @param inBeanName	The bean name from the Spring Framework configuration.
	 *  @return	The bean created by Spring Framework.
	 */
//	public static Object getBean(String inBeanName) {
//
//		if (mFactory == null)
//			setupFactory();
//
//		return mFactory.getBean(inBeanName);
//	}
}
