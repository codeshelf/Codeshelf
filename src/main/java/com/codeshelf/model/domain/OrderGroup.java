/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderGroup.java,v 1.24 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * OrderGroup
 * 
 * A collection of OrderHeaders that can be released and worked as a single unit.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "order_group")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class OrderGroup extends DomainObjectTreeABC<Facility> {
	public static final String UNDEFINED = "undefined";
	
	@Inject
	public static ITypedDao<OrderGroup>	DAO;

	@Singleton
	public static class OrderGroupDao extends GenericDaoABC<OrderGroup> implements ITypedDao<OrderGroup> {
		@Inject
		public OrderGroupDao(final TenantPersistenceService tenantPersistenceService) {
			super(tenantPersistenceService);
		}

		public final Class<OrderGroup> getDaoClass() {
			return OrderGroup.class;
		}
	}

	public final static String			DEFAULT_ORDER_GROUP_DESC_PREFIX	= "Order group - ";

	private static final Logger			LOGGER							= LoggerFactory.getLogger(OrderGroup.class);

	// The parent facility.
	@ManyToOne(optional = false, fetch=FetchType.LAZY)
	@Getter
	@Setter
	private Facility					parent;

	// The collective order status.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private OrderStatusEnum				status;

	// The description.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String						description;

	// The work sequence.
	// This is a sort of the actively working order groups in a facility.
	// Lower numbers work first.
	@Column(nullable = true,name="work_sequence")
	@Getter
	@Setter
	@JsonProperty
	private Integer						workSequence;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Boolean						active;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp					updated;

	// List of all orders belonging to this group. Some orders have no group.
	@OneToMany(mappedBy = "orderGroup")
	@MapKey(name = "domainId")
	private Map<String, OrderHeader>	orderHeaders					= new HashMap<String, OrderHeader>();

	public OrderGroup() {
		status = OrderStatusEnum.CREATED;
	}
	
	public OrderGroup(String domainId) {
		super(domainId);
		active = true;
		status = OrderStatusEnum.CREATED;
		updated = new Timestamp(System.currentTimeMillis());
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<OrderGroup> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "P";
	}

	public Facility getFacility() {
		return getParent();
	}

	public String getOrderGroupId() {
		return getDomainId();
	}

	public void setOrderGroupId(String inOrderGroupId) {
		setDomainId(inOrderGroupId);
	}

	public void addOrderHeader(OrderHeader inOrderHeader) {
		OrderGroup previousOrderGroup = inOrderHeader.getOrderGroup();
		if(previousOrderGroup == null) {
			orderHeaders.put(inOrderHeader.getDomainId(), inOrderHeader);
			inOrderHeader.setOrderGroup(this);
		} else if(!previousOrderGroup.equals(this)){
			LOGGER.error("cannot add OrderHeader "+inOrderHeader.getDomainId()+" to "+this.getDomainId()+" because it has not been removed from "+previousOrderGroup.getDomainId());
		}	
	}

	public OrderHeader getOrderHeader(String inOrderid) {
		return orderHeaders.get(inOrderid);
	}

	public void removeOrderHeader(String inOrderId) {
		OrderHeader orderHeader = this.getOrderHeader(inOrderId);
		if(orderHeader != null) {
			orderHeader.setOrderGroup(null);
			orderHeaders.remove(inOrderId);
		} else {
			LOGGER.error("cannot remove OrderHeader "+inOrderId+" from "+this.getDomainId()+" because it isn't found in my order headers");
		}
	}

	public List<OrderHeader> getOrderHeaders() {
		return new ArrayList<OrderHeader>(orderHeaders.values());
	}

	public List<? extends IDomainObject> getChildren() {
		return getOrderHeaders();
	}

	public static void setDao(OrderGroupDao inOrderGroupDao) {
		OrderGroup.DAO = inOrderGroupDao;
	}
}
