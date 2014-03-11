/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PersistentProperty.java,v 1.23 2013/09/18 00:40:08 jeffw Exp $
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
import lombok.ToString;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
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
@CacheStrategy(useBeanCache = true)@Table(name = "persistent_property")
@JsonAutoDetect(getterVisibility = Visibility.NONE)
//@ToString(doNotUseGetters = true)
public class PersistentProperty extends DomainObjectTreeABC<Organization> {

	@Inject
	public static ITypedDao<PersistentProperty>	DAO;

	@Singleton
	public static class PersistentPropertyDao extends GenericDaoABC<PersistentProperty> implements ITypedDao<PersistentProperty> {
		@Inject
		public PersistentPropertyDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<PersistentProperty> getDaoClass() {
			return PersistentProperty.class;
		}
	}

	public static final String	FORCE_CHANNEL			= "PREFCHAN";
	public static final String	GENERAL_INTF_LOG_LEVEL	= "GENLLOGL";
	public static final String	GATEWAY_INTF_LOG_LEVEL	= "GATELOGL";

	private static final long	serialVersionUID		= -7735810092352246641L;

	// The owning organization.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Organization		parent;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String				defaultValueStr;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String				currentValueStr;

	public PersistentProperty() {
		defaultValueStr = "";
		currentValueStr = "";
	}

	public final ITypedDao<PersistentProperty> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "";
	}

	public final Organization getParent() {
		return parent;
	}

	public final void setParent(Organization inParent) {
		parent = inParent;
	}

	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
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
