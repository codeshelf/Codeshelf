/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderGroup.java,v 1.21 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
@Table(name = "order_group", schema = "codeshelf")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class OrderGroup extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<OrderGroup>	DAO;

	@Singleton
	public static class OrderGroupDao extends GenericDaoABC<OrderGroup> implements ITypedDao<OrderGroup> {
		@Inject
		public OrderGroupDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}
		
		public final Class<OrderGroup> getDaoClass() {
			return OrderGroup.class;
		}
	}

	public final static String	DEFAULT_ORDER_GROUP_DESC_PREFIX	= "Order group - ";

	private static final Logger	LOGGER							= LoggerFactory.getLogger(OrderGroup.class);

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Facility			parent;

	// The collective order status.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private OrderStatusEnum		statusEnum;

	// The description.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String				description;

	// The work sequence.
	// This is a sort of the actively working order groups in a facility.
	// Lower numbers work first.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Integer				workSequence;

	// For a network this is a list of all of the users that belong in the set.
	@OneToMany(mappedBy = "parent")
	@Getter
	private List<OrderHeader>	orderHeaders					= new ArrayList<OrderHeader>();

	public OrderGroup() {
		statusEnum = OrderStatusEnum.CREATED;
	}

	public final ITypedDao<OrderGroup> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "P";
	}

	public final Facility getParent() {
		return parent;
	}

	public final void setParent(Facility inParent) {
		parent = inParent;
	}

	public final String getOrderGroupId() {
		return getDomainId();
	}

	public final void setOrderGroupId(String inOrderGroupId) {
		setDomainId(inOrderGroupId);
	}

	public final List<? extends IDomainObject> getChildren() {
		return getOrderHeaders();
	}

	// We can only add an order to the order group if it is in the CREATED state.
	public final boolean addOrderHeader(OrderHeader inOrderHeader) {
		boolean result = false;
		if (getStatusEnum().equals(OrderStatusEnum.CREATED)) {
			orderHeaders.add(inOrderHeader);
			result = true;
		}
		return result;
	}

	// We can only remove an order to the order group if it is in the CREATED state.
	public final boolean removeOrderHeader(OrderHeader inOrderHeader) {
		boolean result = false;
		if (getStatusEnum().equals(OrderStatusEnum.CREATED)) {
			orderHeaders.remove(inOrderHeader);
			result = true;
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Release the order group to production.
	 * You can only release an order group in the CREATED state.
	 * @return
	 */
	public final Boolean release() {
		Boolean result = false;

		// We can only release order groyps that are in the new state.
		if (getStatusEnum().equals(OrderStatusEnum.CREATED)) {
			result = true;
			setStatusEnum(OrderStatusEnum.RELEASE);
			OrderGroup.DAO.store(this);
		}

		return result;
	}
}
