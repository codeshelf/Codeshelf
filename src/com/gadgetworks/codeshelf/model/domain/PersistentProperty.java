/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PersistentProperty.java,v 1.7 2012/09/23 03:05:42 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * PersistentProperty
 * 
 * The PersistentProperty object holds persistent property information.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "PERSISTENTPROPERTY")
@CacheStrategy
public class PersistentProperty<T extends DomainObjectABC> extends DomainObjectABC {

	@Inject
	public static ITypedDao<PersistentProperty>	DAO;

	@Singleton
	public static class PersistentPropertyDao extends GenericDaoABC<PersistentProperty> implements ITypedDao<PersistentProperty> {
		public final Class<PersistentProperty> getDaoClass() {
			return PersistentProperty.class;
		}
	}

	//	public static final String	SHOW_CONSOLE_PREF			= "SHOWCONS";
	//	public static final String	SHOW_CONNECTION_DEBUG_PREF	= "CONNDBUG";
	public static final String	FORCE_CHANNEL			= "PREFCHAN";
	public static final String	GENERAL_INTF_LOG_LEVEL	= "GENLLOGL";
	public static final String	GATEWAY_INTF_LOG_LEVEL	= "GATELOGL";
	//	public static final String	ACTIVEMQ_RUN				= "ACTMQRUN";
	//	public static final String	ACTIVEMQ_USERID				= "ACTMQUID";
	//	public static final String	ACTIVEMQ_PASSWORD			= "ACTMQPWD";
	//	public static final String	ACTIVEMQ_JMS_PORTNUM		= "ACTMQJMS";
	//	public static final String	ACTIVEMQ_STOMP_PORTNUM		= "ACTMQSTM";

	private static final long	serialVersionUID		= -7735810092352246641L;

	@Column(nullable = false)
	@Getter
	@Setter
	private String				defaultValueStr;
	@Column(nullable = false)
	@Getter
	@Setter
	private String				currentValueStr;

	// The owning organization.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	private Organization		parent;

	public PersistentProperty() {
		defaultValueStr = "";
		currentValueStr = "";
	}

	public final ITypedDao<PersistentProperty> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "PP";
	}

	public final Organization getParentOrganization() {
		return parent;
	}

	public final void setParentOrganization(final Organization inOrganization) {
		parent = inOrganization;
	}

	public final IDomainObject getParent() {
		return getParentOrganization();
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Organization) {
			setParentOrganization((Organization) inParent);
		}
	}

	@JsonIgnore
	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	public final String toString() {
		return getDomainId() + "val: " + getCurrentValueAsStr() + " (default: " + getDefaultValueAsStr() + ")";
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
