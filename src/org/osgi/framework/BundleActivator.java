/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: BundleActivator.java,v 1.1 2011/01/21 01:08:16 jeffw Exp $
 *******************************************************************************/
package org.osgi.framework;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface BundleActivator {
	/**
	 * Called when this bundle is started so the Framework can perform the
	 * bundle-specific activities necessary to start this bundle. This method
	 * can be used to register services or to allocate any resources that this
	 * bundle needs.
	 * 
	 * <p>
	 * This method must complete and return to its caller in a timely manner.
	 * 
	 * @param context The execution context of the bundle being started.
	 * @throws java.lang.Exception If this method throws an exception, this
	 *         bundle is marked as stopped and the Framework will remove this
	 *         bundle's listeners, unregister all services registered by this
	 *         bundle, and release all services used by this bundle.
	 */
	public void start(BundleContext context) throws Exception;

	/**
	 * Called when this bundle is stopped so the Framework can perform the
	 * bundle-specific activities necessary to stop the bundle. In general, this
	 * method should undo the work that the <code>BundleActivator.start</code>
	 * method started. There should be no active threads that were started by
	 * this bundle when this bundle returns. A stopped bundle must not call any
	 * Framework objects.
	 * 
	 * <p>
	 * This method must complete and return to its caller in a timely manner.
	 * 
	 * @param context The execution context of the bundle being stopped.
	 * @throws java.lang.Exception If this method throws an exception, the
	 *         bundle is still marked as stopped, and the Framework will remove
	 *         the bundle's listeners, unregister all services registered by the
	 *         bundle, and release all services used by the bundle.
	 */
	public void stop(BundleContext context) throws Exception;
}
