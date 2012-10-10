/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IEdiProcessor.java,v 1.1 2012/10/10 22:15:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

/**
 * @author jeffw
 *
 */
public interface IEdiProcessor {

	void startProcessor();
	
	void restartProcessor();

	void stopProcessor();
	
}
