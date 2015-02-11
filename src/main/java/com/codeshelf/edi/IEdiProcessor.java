/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IEdiProcessor.java,v 1.3 2013/03/19 01:19:59 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.util.concurrent.BlockingQueue;

/**
 * @author jeffw
 *
 */
public interface IEdiProcessor {

	String	EDIPROCESSOR_THREAD_NAME	= "EDI Processor";

	void startProcessor(BlockingQueue<String> inEdiSignalQueue);

	void stopProcessor();

}
