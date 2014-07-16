/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WirelessDeviceABC.java,v 1.9 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.NetworkDeviceStateEnum;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * WirelessDevice
 * 
 * A WirelessDevice is a base class that holds information and behavior that's common to all devices
 * attached to the network.
 * 
 * @author jeffw
 */

@Entity
@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
@ToString(doNotUseGetters = true, callSuper = true)
public abstract class WirelessDeviceABC extends DomainObjectTreeABC<CodeshelfNetwork> {

	public static final int						MAC_ADDR_BYTES		= 8;
	public static final int						PUBLIC_KEY_BYTES	= 8;

	@Inject
	private static ITypedDao<WirelessDeviceABC>	DAO;

	@Singleton
	public static class WirelessDeviceDao extends GenericDaoABC<WirelessDeviceABC> implements ITypedDao<WirelessDeviceABC> {
		@Inject
		public WirelessDeviceDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}
		
		public final Class<WirelessDeviceABC> getDaoClass() {
			return WirelessDeviceABC.class;
		}
	}

	private static final Logger		LOGGER	= LoggerFactory.getLogger(WirelessDeviceABC.class);

	// The owning network.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private CodeshelfNetwork		parent;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private byte[]					deviceGuid;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String					publicKey;

	// The description.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String					description;

	// The last seen battery level.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private short					lastBatteryLevel;

	@Transient
	@Column(nullable = true)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private NetworkDeviceStateEnum	networkDeviceStatus;

	@Transient
	@Column(nullable = true)
	@Setter
	@JsonProperty
	private Long					lastContactTime;

	// The network address last assigned to this wireless device.
	@Transient
	@Column(nullable = true)
	@JsonProperty
	private byte					networkAddress;

	public WirelessDeviceABC() {
		deviceGuid = new byte[NetGuid.NET_GUID_BYTES];
		publicKey = "";
		description = "";
		lastBatteryLevel = 0;
	}

	public final CodeshelfNetwork getParent() {
		return parent;
	}

	public final void setParent(CodeshelfNetwork inParent) {
		parent = inParent;
	}

	public final NetGuid getDeviceNetGuid() {
		return new NetGuid(deviceGuid);
	}

	public final void setDeviceNetGuid(NetGuid inGuid) {
		deviceGuid = inGuid.getParamValueAsByteArray();
	}

	@JsonIgnore
	public final String getDeviceGuidStr() {
		return new NetGuid(deviceGuid).toString();
	}

	public final void setNetAddress(NetAddress inNetworkAddress) {
		networkAddress = (byte) inNetworkAddress.getValue();
	}

	public final NetAddress getNetAddress() {
		return new NetAddress(networkAddress);
	}

	public final boolean doesMatch(NetGuid inGuid) {
		return Arrays.equals(deviceGuid, inGuid.getParamValueAsByteArray());
	}

	public final long getLastContactTime() {
		long result = 0;
		Long longVal = lastContactTime;
		if (longVal != null) {
			result = longVal.longValue();
		}
		return result;
	}

	public final NetworkDeviceStateEnum getNetworkDeviceState() {
		NetworkDeviceStateEnum result = networkDeviceStatus;
		if (result == null) {
			result = NetworkDeviceStateEnum.getNetworkDeviceStateEnum(0); //INVALID;
		}
		return result;
	}

	public final void setNetworkDeviceState(NetworkDeviceStateEnum inState) {
		LOGGER.debug(Arrays.toString(deviceGuid) + " state changed: " + networkDeviceStatus + "->" + inState);
		networkDeviceStatus = inState;
	}

	/*
	 * These methods are needed, because it's not currentyl possible (as of v1.1.0) to have
	 * abstract super classes in an EBean hierarchy that are also entity classes since EBean tries to create
	 * an instance of every entity class.  If you make this super class a non-entity class then EBean
	 * doesn't create a functional inheritance info tree (one of the parent refs in the tree is null), thus
	 * causing the EBean server to not start.
	 * 
	 */

	public void commandReceived(final String inCommandStr) {
		// See above note.
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.INetworkDevice#setDesc(java.lang.String)
	 */
	public final void setDesc(String inDeviceDescription) {
		//		mDeviceDesc = inDeviceDescription;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.INetworkDevice#setDeviceType(short)
	 */
	public final void setDeviceType(short inDeviceType) {
		//		mDeviceType = inDeviceType;
	}

}
