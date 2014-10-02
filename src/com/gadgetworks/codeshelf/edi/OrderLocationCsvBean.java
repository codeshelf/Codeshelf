/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderCsvBean.java,v 1.2 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeffw
 *
 */

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class OrderLocationCsvBean extends ImportCsvBeanABC implements Comparable<OrderLocationCsvBean> {

	static final Logger	LOGGER	= LoggerFactory.getLogger(OrderLocationCsvBean.class);

	@NotNull
	@Size(min = 1)
	protected String	orderId;
	@NotNull
	@Size(min = 1)
	protected String	locationId;
	
	public final String getOrderId() {
		return strip(orderId);
	}
	
	public final String getLocationId() {
		return strip(locationId);
	}

	@Override
	public int compareTo(OrderLocationCsvBean other) {
		int result = nullSafeStringComparator(this.orderId, other.orderId);
		if (result != 0) {
			return result;
		}
		return nullSafeStringComparator(this.locationId, other.locationId);
	}
	
	private static int nullSafeStringComparator(final String one, final String two) {
	    if (one == null ^ two == null) {
	        return (one == null) ? -1 : 1;
	    }

	    if (one == null && two == null) {
	        return 0;
	    }

	    return one.compareToIgnoreCase(two); // silly FindBugs, this is fine
	}
}
