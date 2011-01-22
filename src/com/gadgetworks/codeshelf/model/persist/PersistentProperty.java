/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PersistentProperty.java,v 1.3 2011/01/22 07:58:31 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.Entity;

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

	public static final String	SHOW_CONSOLE_PREF			= "SHOWCONS";
	public static final String	SHOW_CONNECTION_DEBUG_PREF	= "CONNDBUG";
	public static final String	FORCE_CHANNEL				= "PREFCHAN";
	public static final String	GENERAL_INTF_LOG_LEVEL		= "GENLLOGL";
	public static final String	GATEWAY_INTF_LOG_LEVEL		= "GATELOGL";
	public static final String	ACTIVEMQ_RUN				= "ACTMQRUN";
	public static final String	ACTIVEMQ_USERID				= "ACTMQUID";
	public static final String	ACTIVEMQ_PASSWORD			= "ACTMQPWD";
	public static final String	ACTIVEMQ_JMS_PORTNUM		= "ACTMQJMS";
	public static final String	ACTIVEMQ_STOMP_PORTNUM		= "ACTMQSTM";

	private String				mPropertyId;
	private String				mDefaultValueStr;
	private String				mCurrentValueStr;

	public PersistentProperty() {
		mPropertyId = "";
		mDefaultValueStr = "";
		mCurrentValueStr = "";
	}

	public String toString() {
		return mPropertyId + "val: " + getCurrentValueAsStr() + " (default: " + getDefaultValueAsStr() + ")";
	}

	public String getId() {
		return mPropertyId;
	}

	public void setId(String inPropertyId) {
		mPropertyId = inPropertyId;
	}

	// Default value methods.
	public String getDefaultValueAsStr() {
		return mDefaultValueStr;
	}

	public void setDefaultValueAsStr(String inValueStr) {
		mDefaultValueStr = inValueStr;
	}

	public void setDefaultValueAsBoolean(boolean inBoolean) {
		mDefaultValueStr = Boolean.toString(inBoolean);
	}

	public boolean getDefaultValueAsBoolean() {
		return Boolean.parseBoolean(mDefaultValueStr);
	}

	public void setDefaultValueAsInt(int inInt) {
		mDefaultValueStr = Integer.toString(inInt);
	}

	public int getDefaultValueAsInt() {
		return Integer.parseInt(mDefaultValueStr);
	}

	public void setDefaultValueAsLong(long inLong) {
		mDefaultValueStr = Long.toString(inLong);
	}

	public long getDefaultValueAsLong() {
		return Long.parseLong(mDefaultValueStr);
	}

	public void setDefaultValueAsFloat(float inFloat) {
		mDefaultValueStr = Float.toString(inFloat);
	}

	public float getDefaultValueAsFloat() {
		return Float.parseFloat(mDefaultValueStr);
	}

	public void setDefaultValueAsDouble(double inDouble) {
		mDefaultValueStr = Double.toString(inDouble);
	}

	public double getDefaultValueAsDouble() {
		return Double.parseDouble(mDefaultValueStr);
	}

	// Current value methods.
	public String getCurrentValueAsStr() {
		return mCurrentValueStr;
	}

	public void setCurrentValueAsStr(String inValueStr) {
		mCurrentValueStr = inValueStr;
	}

	public void setCurrentValueAsBoolean(boolean inBoolean) {
		mCurrentValueStr = Boolean.toString(inBoolean);
	}

	public boolean getCurrentValueAsBoolean() {
		return Boolean.parseBoolean(mCurrentValueStr);
	}

	public void setCurrentValueAsInt(int inInt) {
		mCurrentValueStr = Integer.toString(inInt);
	}

	public int getCurrentValueAsInt() {
		if (mCurrentValueStr.length() == 0)
			mCurrentValueStr = "0";
		return Integer.parseInt(mCurrentValueStr);
	}

	public void setCurrentValueAsLong(long inLong) {
		mCurrentValueStr = Long.toString(inLong);
	}

	public long getCurrentValueAsLong() {
		if (mCurrentValueStr.length() == 0)
			mCurrentValueStr = "0";
		return Long.parseLong(mCurrentValueStr);
	}

	public void setCurrentValueAsFloat(float inFloat) {
		mCurrentValueStr = Float.toString(inFloat);
	}

	public float getCurrentValueAsFloat() {
		if (mCurrentValueStr.length() == 0)
			mCurrentValueStr = "0.0";
		return Float.parseFloat(mCurrentValueStr);
	}

	public void setCurrentValueAsDouble(double inDouble) {
		mCurrentValueStr = Double.toString(inDouble);
	}

	public double getCurrentValueAsDouble() {
		if (mCurrentValueStr.length() == 0)
			mCurrentValueStr = "0.0";
		return Double.parseDouble(mCurrentValueStr);
	}

}
