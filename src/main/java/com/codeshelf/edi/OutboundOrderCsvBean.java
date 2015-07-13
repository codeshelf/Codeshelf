/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderCsvBean.java,v 1.2 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.fortuna.ical4j.model.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeffw
 *
 */

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class OutboundOrderCsvBean extends ImportCsvBeanABC {

	static final Logger	LOGGER	= LoggerFactory.getLogger(OutboundOrderCsvBean.class);

	protected String	orderGroupId;

	@NotNull
	@Size(min = 1)
	protected String	orderId;
	protected String	orderDetailId;
	@NotNull
	@Size(min = 1)
	protected String	itemId;
	protected String	description;
	protected String	quantity;
	protected String	minQuantity;
	protected String	maxQuantity;
	@NotNull
	@Size(min = 1)
	protected String	uom;
	protected String	orderDate;
	protected String	dueDate;
	protected String	destinationId;
	protected String	pickStrategy;
	protected String	preAssignedContainerId;
	protected String	shipperId;
	protected String	customerId;
	protected String	workSequence;
	// new fields for location-based pick DEV-571
	protected String	operationType;
	protected String	locationId;
	protected String	cmFromLeft;
	protected String	needsScan;

	// new fields for scanable inventory DEV-642
	protected String	gtin; // use this field for upc

	public final String getGtin() {
		return strip(gtin);
	}
	
	public final String getOrderGroupId() {
		return strip(orderGroupId);
	}

	public final String getOrderId() {
		return strip(orderId);
	}

	public final String getOrderDetailsId() {
		return strip(orderDetailId);
	}

	public final String getItemId() {
		return strip(itemId);
	}

	public final String getDescription() {
		if(null == description) {
			return "null";
		}
		String theDescription = description;
		theDescription = strip(theDescription);
		// Customer can send anything. We saw non-UTF8 from GoodEggs

		CharsetEncoder charsetEncoder = StandardCharsets.US_ASCII.newEncoder();
		charsetEncoder.onMalformedInput(CodingErrorAction.REPLACE);
		charsetEncoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		ByteBuffer bytes = null; //= ByteBuffer.wrap(byte[]);
		try {
			bytes = charsetEncoder.encode(CharBuffer.wrap(theDescription));
		} catch (CharacterCodingException e1) {
			LOGGER.warn("character coding exception", e1);
		}

		// theDescription = theDescription.replaceAll( "([\\ud800-\\udbff\\udc00-\\udfff])", ""); // strips all UTF-16 pairs, but can leave a long string		
		// theDescription = theDescription.replaceAll("\\p{C}", ""); // strips valid UTF-8 which does not solve our problem. Misses UTF-16 cases

		try {
			theDescription = new String(bytes.array(), "US-ASCII");
			// This leaves the string too long if there were a lot of UTF-16 characters
		} catch (Exception e) {
			LOGGER.warn("character coding exception", e);
			theDescription = "";
		}

		// We know the database field is a varchar255, so should restrict its length and truncate.
		if (theDescription.length() > 255) {
			theDescription = theDescription.substring(0, 255);
			LOGGER.warn("truncating description to fit database column");
		}
		return theDescription;
	}
	
	public void fillDefaultDueDate(){
		if (getDueDate() == null) {
			setDueDate(new Date().toString());
		}
	}

	public final String getQuantity() {
		return strip(quantity);
	}

	public final String getMinQuantity() {
		return strip(minQuantity);
	}

	public final String getMaxQuantity() {
		return strip(maxQuantity);
	}

	public final String getUom() {
		return strip(uom);
	}

	public final String getOrderDate() {
		return strip(orderDate);
	}

	public final String getDueDate() {
		return strip(dueDate);
	}

	public final String getDestinationId() {
		return strip(destinationId);
	}

	public final String getPickStrategy() {
		return strip(pickStrategy);
	}

	public final String getPreAssignedContainerId() {
		return strip(preAssignedContainerId);
	}

	public final String getShipperId() {
		return strip(shipperId);
	}

	public final String getCustomerId() {
		return strip(customerId);
	}

	public final String getOperationType() {
		return strip(operationType);
	}

	public final String getLocationId() {
		return strip(locationId);
	}

	public final String getCmFromLeft() {
		return strip(cmFromLeft);
	}

	public String getNeedsScan() {
		return strip(needsScan);
	}

}