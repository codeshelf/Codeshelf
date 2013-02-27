/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WirelessDeviceABC.java,v 1.1 2013/02/27 01:17:02 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetMacAddress;
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
@MappedSuperclass
@CacheStrategy
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "WIRELESSDEVICE", schema = "CODESHELF")
@DiscriminatorColumn(name = "DTYPE", discriminatorType = DiscriminatorType.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
@ToString
//public abstract class LocationABC<P extends IDomainObject> extends DomainObjectTreeABC<P> {

public abstract class WirelessDeviceABC<P extends IDomainObject> extends DomainObjectTreeABC<P> {

	public static final int			MAC_ADDR_BYTES		= 8;
	public static final int			PUBLIC_KEY_BYTES	= 8;

	@Inject
	private static ITypedDao<WirelessDeviceABC>	DAO;

	@Singleton
	public static class WirelessDeviceDao extends GenericDaoABC<WirelessDeviceABC> implements ITypedDao<WirelessDeviceABC> {
		public final Class<WirelessDeviceABC> getDaoClass() {
			return WirelessDeviceABC.class;
		}
	}

	private static final Log		LOGGER				= LogFactory.getLog(WirelessDeviceABC.class);

//	// The owning network.
//	@Column(nullable = false)
//	@ManyToOne(optional = false)
//	private CodeshelfNetwork		parent;

	@Column(nullable = false)
	private byte[]					macAddress;

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

	//@Transient
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private NetworkDeviceStateEnum	networkDeviceStatus;

	//@Transient
	@Column(nullable = false)
	@Setter
	@JsonProperty
	private Long					lastContactTime;

	// The network address last assigned to this wireless device.
	@Transient
	@Column(nullable = false)
	@JsonProperty
	private byte					networkAddress;

	public WirelessDeviceABC() {
		macAddress = new byte[NetMacAddress.NET_MACADDR_BYTES];
		publicKey = "";
		description = "";
		lastBatteryLevel = 0;
	}

	public final NetMacAddress getMacAddress() {
		return new NetMacAddress(macAddress);
	}

	public final void setMacAddress(NetMacAddress inMacAddress) {
		macAddress = inMacAddress.getParamValueAsByteArray();
	}

	public final void setNetAddress(NetAddress inNetworkAddress) {
		networkAddress = (byte) inNetworkAddress.getValue();
	}

	@JsonProperty
	public final NetAddress getNetAddress() {
		return new NetAddress(networkAddress);
	}

	public final boolean doesMatch(NetMacAddress inMacAddr) {
		return Arrays.equals(macAddress, inMacAddr.getParamValueAsByteArray());
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
		LOGGER.debug(Arrays.toString(macAddress) + " state changed: " + networkDeviceStatus + "->" + inState);
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
