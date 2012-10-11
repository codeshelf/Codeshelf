/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IEdiProcessor.java,v 1.2 2012/10/11 02:42:39 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

/**
 * @author jeffw
 *
 */
public interface IEdiProcessor {

	String	EDIPROCESSOR_THREAD_NAME	= "EDI Processor";

	void startProcessor();

	void restartProcessor();

	void stopProcessor();

}
