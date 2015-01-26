/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ContainerUse.java,v 1.15 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * ContainerUse
 * 
 * A single use of the container in the facility
 * 
 * @author jeffw
 */

@Entity
@Table(name = "container_use")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class ContainerUse extends DomainObjectTreeABC<Container> {

	@Inject
	public static ITypedDao<ContainerUse>	DAO;

	@Singleton
	public static class ContainerUseDao extends GenericDaoABC<ContainerUse> implements ITypedDao<ContainerUse> {
		@Inject
		public ContainerUseDao(PersistenceService persistenceService) {
			super(persistenceService);
		}

		public final Class<ContainerUse> getDaoClass() {
			return ContainerUse.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ContainerUse.class);

	// The container used.
	@ManyToOne(optional = false, fetch=FetchType.LAZY)
	@Getter
	@Setter
	private Container			parent;

	// Use date.
	@Column(nullable = false,name="used_on")
	@Getter
	@Setter
	@JsonProperty
	private Timestamp			usedOn;

	// The order where we used this container.
	@OneToOne(optional = true, fetch=FetchType.LAZY)
	@JoinColumn(name="order_header_persistentid")
	@Getter
	@Setter
	private OrderHeader			orderHeader;

	// The che where we're using this container.
	@ManyToOne(optional = true, fetch=FetchType.LAZY)
	@JoinColumn(name="current_che_persistentid")
	@Getter
	@Setter
	private Che					currentChe;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Boolean				active;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp			updated;

	public ContainerUse() {
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<ContainerUse> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "USE";
	}

	public Facility getFacility() {
		return getParent().getFacility();
	}

	// used to have a lomboc annotation, but that had an infinite loop potential with CHE toString.
	public String toString() {
		// What we would want to see if logged as toString?  parent containerID for sure. Order header. Che?
		String returnString = getParent().getDomainId();
		OrderHeader theHeader = getOrderHeader();
		returnString += " forOrder:";
		if (theHeader != null)
		returnString += theHeader.getDomainId();
		Che theChe = getCurrentChe();
		if (theChe != null) {
			returnString += " on CHE:";
			returnString += theChe.getDomainId();
		}		
		return returnString;
	}

	// UI meta-field support
	public String getContainerName() {
		Container theContainer = getParent();
		if (theContainer == null)
			return "";
		else {
			return theContainer.getDomainId();
		}
	}

	public String getCheName() {
		Che theChe = getCurrentChe();
		if (theChe == null)
			return "";
		else {
			return theChe.getDomainId();
		}
	}

	public String getOrderName() {
		OrderHeader theOrder = getOrderHeader();
		if (theOrder == null)
			return "";
		else {
			return theOrder.getDomainId();
		}
	}

	// Initially for the GoodEggs case, or general cross wall. 
	// Not sure if this approach could work for outbound orders. Important. 
	// GoodEggs puts only one item per container. These meta fields assume that.
	private ItemMaster getRelevantItem() {
		OrderDetail detail = null;
		ItemMaster theItem = null;
		OrderHeader header = this.getOrderHeader();
		if (header != null) {
			if (header.getOrderType() == OrderTypeEnum.CROSS) {
				List<OrderDetail> details = header.getOrderDetails();
				if (details.size() > 0)
					detail = details.get(0); // return the first
			}
		}
		if (detail != null)
			theItem = detail.getItemMaster();
			
		return theItem;
	}

	public String getItemInCntrDescription() {
		ItemMaster  master = getRelevantItem();
		if (master != null)
			return master.getDescription();
		
		return "";
	}

	public String getItemInCntrSku() {
		ItemMaster  master = getRelevantItem();
		if (master != null)
			return master.getItemId();
		
		return "";
	}


	public String getItemInCntrPersistentId() {
		ItemMaster  master = getRelevantItem();
		if (master != null)
			return master.getPersistentId().toString();
		
		return "";
	}

	public static void setDao(ContainerUseDao inContainerUseDao) {
		ContainerUse.DAO = inContainerUseDao;
	}

}
