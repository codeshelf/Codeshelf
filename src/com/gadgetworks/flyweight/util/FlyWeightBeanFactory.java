/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: FlyWeightBeanFactory.java,v 1.1 2013/02/20 08:28:26 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.util;


// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class FlyWeightBeanFactory {

	//private static XmlBeanFactory	mFactory;

	// --------------------------------------------------------------------------
	/**
	 * 	Create the bean factory for the Spring Framework.
	 */
	private FlyWeightBeanFactory() {
		//setupFactory();
	}

	private static void setupFactory() {
		Thread.currentThread().setContextClassLoader(FlyWeightBeanFactory.class.getClassLoader());

//		Resource res = new ClassPathResource("conf/FlyWeightContext.xml");
//		mFactory = new XmlBeanFactory(res);
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
