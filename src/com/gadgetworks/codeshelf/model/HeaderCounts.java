/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Codeshelf, Inc., All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

/**
 * Just a class to return some out parameters
 * 
 */
public class HeaderCounts {
	public int mTotalHeaders;
	public int mActiveHeaders;
	public int mActiveDetails;
	public int mActiveCntrUses;
	public int mInactiveDetailsOnActiveOrders;
	public int mInactiveCntrUsesOnActiveOrders;
	public HeaderCounts() {
		mTotalHeaders = 0;
		mActiveHeaders = 0;
		mActiveDetails = 0;
		mActiveCntrUses = 0;
		mInactiveDetailsOnActiveOrders = 0;
		mInactiveCntrUsesOnActiveOrders = 0;
	}

}
