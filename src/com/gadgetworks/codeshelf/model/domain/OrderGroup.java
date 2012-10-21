/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderGroup.java,v 1.9 2012/10/21 02:02:17 jeffw Exp $
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
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
@Table(name = "ORDERGROUP")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class OrderGroup extends DomainObjectABC {

	@Inject
	public static ITypedDao<OrderGroup>	DAO;

	@Singleton
	public static class OrderGroupDao extends GenericDaoABC<OrderGroup> implements ITypedDao<OrderGroup> {
		public final Class<OrderGroup> getDaoClass() {
			return OrderGroup.class;
		}
	}

	private static final Log	LOGGER			= LogFactory.getLog(OrderGroup.class);

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
	private List<OrderHeader>	orderHeaders	= new ArrayList<OrderHeader>();

	public OrderGroup() {
		statusEnum = OrderStatusEnum.CREATED;
	}

	public final ITypedDao<OrderGroup> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "P";
	}

	public final Facility getParentFacility() {
		return parent;
	}

	public final void setParentFacility(final Facility inFacility) {
		parent = inFacility;
	}

	public final IDomainObject getParent() {
		return parent;
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Facility) {
			setParentFacility((Facility) inParent);
		}
	}

	public String getOrderGroupId() {
		return getShortDomainId();
	}

	public final void setOrderGroupId(String inOrderGroupId) {
		setShortDomainId(inOrderGroupId);
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
			setStatusEnum(OrderStatusEnum.RELEASED);
			OrderGroup.DAO.store(this);
		}

		return result;
	}
}
