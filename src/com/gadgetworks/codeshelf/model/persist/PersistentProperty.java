/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PersistentProperty.java,v 1.9 2012/03/18 04:12:26 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.Column;
import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.google.inject.Inject;

// --------------------------------------------------------------------------
/**
 * PersistentProperty
 * 
 * The PersistentProperty object holds persistent property information.
 * 
 * @author jeffw
 */

@Entity
public class PersistentProperty extends PersistABC {

	public static final String							SHOW_CONSOLE_PREF			= "SHOWCONS";
	public static final String							SHOW_CONNECTION_DEBUG_PREF	= "CONNDBUG";
	public static final String							FORCE_CHANNEL				= "PREFCHAN";
	public static final String							GENERAL_INTF_LOG_LEVEL		= "GENLLOGL";
	public static final String							GATEWAY_INTF_LOG_LEVEL		= "GATELOGL";
	public static final String							ACTIVEMQ_RUN				= "ACTMQRUN";
	public static final String							ACTIVEMQ_USERID				= "ACTMQUID";
	public static final String							ACTIVEMQ_PASSWORD			= "ACTMQPWD";
	public static final String							ACTIVEMQ_JMS_PORTNUM		= "ACTMQJMS";
	public static final String							ACTIVEMQ_STOMP_PORTNUM		= "ACTMQSTM";

	public interface IPersistentPropertyDao extends IGenericDao<PersistentProperty> {		
	}
	
	private static final long							serialVersionUID			= -7735810092352246641L;

	@Column(nullable = false)
	@Getter
	@Setter
	private String										defaultValueStr;
	@Column(nullable = false)
	@Getter
	@Setter
	private String										currentValueStr;

	public PersistentProperty() {
		defaultValueStr = "";
		currentValueStr = "";
	}

	public final String toString() {
		return getId() + "val: " + getCurrentValueAsStr() + " (default: " + getDefaultValueAsStr() + ")";
	}

	// Default value methods.
	public final String getDefaultValueAsStr() {
		return defaultValueStr;
	}

	public final void setDefaultValueAsStr(String inValueStr) {
		defaultValueStr = inValueStr;
	}

	public final void setDefaultValueAsBoolean(boolean inBoolean) {
		defaultValueStr = Boolean.toString(inBoolean);
	}

	public final boolean getDefaultValueAsBoolean() {
		return Boolean.parseBoolean(defaultValueStr);
	}

	public final void setDefaultValueAsInt(int inInt) {
		defaultValueStr = Integer.toString(inInt);
	}

	public final int getDefaultValueAsInt() {
		return Integer.parseInt(defaultValueStr);
	}

	public final void setDefaultValueAsLong(long inLong) {
		defaultValueStr = Long.toString(inLong);
	}

	public final long getDefaultValueAsLong() {
		return Long.parseLong(defaultValueStr);
	}

	public final void setDefaultValueAsFloat(float inFloat) {
		defaultValueStr = Float.toString(inFloat);
	}

	public final float getDefaultValueAsFloat() {
		return Float.parseFloat(defaultValueStr);
	}

	public final void setDefaultValueAsDouble(double inDouble) {
		defaultValueStr = Double.toString(inDouble);
	}

	public final double getDefaultValueAsDouble() {
		return Double.parseDouble(defaultValueStr);
	}

	// Current value methods.
	public final String getCurrentValueAsStr() {
		return currentValueStr;
	}

	public final void setCurrentValueAsStr(String inValueStr) {
		currentValueStr = inValueStr;
	}

	public final void setCurrentValueAsBoolean(boolean inBoolean) {
		currentValueStr = Boolean.toString(inBoolean);
	}

	public final boolean getCurrentValueAsBoolean() {
		return Boolean.parseBoolean(currentValueStr);
	}

	public final void setCurrentValueAsInt(int inInt) {
		currentValueStr = Integer.toString(inInt);
	}

	public final int getCurrentValueAsInt() {
		if (currentValueStr.length() == 0)
			currentValueStr = "0";
		return Integer.parseInt(currentValueStr);
	}

	public final void setCurrentValueAsLong(long inLong) {
		currentValueStr = Long.toString(inLong);
	}

	public final long getCurrentValueAsLong() {
		if (currentValueStr.length() == 0)
			currentValueStr = "0";
		return Long.parseLong(currentValueStr);
	}

	public final void setCurrentValueAsFloat(float inFloat) {
		currentValueStr = Float.toString(inFloat);
	}

	public final float getCurrentValueAsFloat() {
		if (currentValueStr.length() == 0)
			currentValueStr = "0.0";
		return Float.parseFloat(currentValueStr);
	}

	public final void setCurrentValueAsDouble(double inDouble) {
		currentValueStr = Double.toString(inDouble);
	}

	public final double getCurrentValueAsDouble() {
		if (currentValueStr.length() == 0)
			currentValueStr = "0.0";
		return Double.parseDouble(currentValueStr);
	}

}
