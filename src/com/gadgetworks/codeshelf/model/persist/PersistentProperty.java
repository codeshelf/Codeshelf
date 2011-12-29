/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PersistentProperty.java,v 1.6 2011/12/29 09:15:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.Column;
import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.dao.GenericDao;

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

	public static final GenericDao<PersistentProperty>	DAO							= new GenericDao<PersistentProperty>(PersistentProperty.class);

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

	public String toString() {
		return getId() + "val: " + getCurrentValueAsStr() + " (default: " + getDefaultValueAsStr() + ")";
	}

	// Default value methods.
	public String getDefaultValueAsStr() {
		return defaultValueStr;
	}

	public void setDefaultValueAsStr(String inValueStr) {
		defaultValueStr = inValueStr;
	}

	public void setDefaultValueAsBoolean(boolean inBoolean) {
		defaultValueStr = Boolean.toString(inBoolean);
	}

	public boolean getDefaultValueAsBoolean() {
		return Boolean.parseBoolean(defaultValueStr);
	}

	public void setDefaultValueAsInt(int inInt) {
		defaultValueStr = Integer.toString(inInt);
	}

	public int getDefaultValueAsInt() {
		return Integer.parseInt(defaultValueStr);
	}

	public void setDefaultValueAsLong(long inLong) {
		defaultValueStr = Long.toString(inLong);
	}

	public long getDefaultValueAsLong() {
		return Long.parseLong(defaultValueStr);
	}

	public void setDefaultValueAsFloat(float inFloat) {
		defaultValueStr = Float.toString(inFloat);
	}

	public float getDefaultValueAsFloat() {
		return Float.parseFloat(defaultValueStr);
	}

	public void setDefaultValueAsDouble(double inDouble) {
		defaultValueStr = Double.toString(inDouble);
	}

	public double getDefaultValueAsDouble() {
		return Double.parseDouble(defaultValueStr);
	}

	// Current value methods.
	public String getCurrentValueAsStr() {
		return currentValueStr;
	}

	public void setCurrentValueAsStr(String inValueStr) {
		currentValueStr = inValueStr;
	}

	public void setCurrentValueAsBoolean(boolean inBoolean) {
		currentValueStr = Boolean.toString(inBoolean);
	}

	public boolean getCurrentValueAsBoolean() {
		return Boolean.parseBoolean(currentValueStr);
	}

	public void setCurrentValueAsInt(int inInt) {
		currentValueStr = Integer.toString(inInt);
	}

	public int getCurrentValueAsInt() {
		if (currentValueStr.length() == 0)
			currentValueStr = "0";
		return Integer.parseInt(currentValueStr);
	}

	public void setCurrentValueAsLong(long inLong) {
		currentValueStr = Long.toString(inLong);
	}

	public long getCurrentValueAsLong() {
		if (currentValueStr.length() == 0)
			currentValueStr = "0";
		return Long.parseLong(currentValueStr);
	}

	public void setCurrentValueAsFloat(float inFloat) {
		currentValueStr = Float.toString(inFloat);
	}

	public float getCurrentValueAsFloat() {
		if (currentValueStr.length() == 0)
			currentValueStr = "0.0";
		return Float.parseFloat(currentValueStr);
	}

	public void setCurrentValueAsDouble(double inDouble) {
		currentValueStr = Double.toString(inDouble);
	}

	public double getCurrentValueAsDouble() {
		if (currentValueStr.length() == 0)
			currentValueStr = "0.0";
		return Double.parseDouble(currentValueStr);
	}

}
