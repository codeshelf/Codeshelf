/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: IPickDocumentGenerator.java,v 1.1 2013/03/19 01:19:59 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.report;

import java.util.concurrent.BlockingQueue;

/**
 * @author jeffw
 *
 */
public interface IPickDocumentGenerator {

	String	PICK_DOCUMENT_GENERATOR_THREAD_NAME	= "Pick Doc Generator";

	void startProcessor(BlockingQueue<String> inSignalQueue);

	void stopProcessor();
}
