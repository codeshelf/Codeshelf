/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderHeader.java,v 1.10 2012/10/16 06:23:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * Order
 * 
 * A collection of OrderDetails that make up the work needed in the facility.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "ORDERHEADER")
@CacheStrategy
public class OrderHeader extends DomainObjectABC {

	@Inject
	public static ITypedDao<OrderHeader>	DAO;

	@Singleton
	public static class OrderHeaderDao extends GenericDaoABC<OrderHeader> implements ITypedDao<OrderHeader> {
		public final Class<OrderHeader> getDaoClass() {
			return OrderHeader.class;
		}
	}

	private static final Log	LOGGER			= LogFactory.getLog(OrderHeader.class);

	// The collective order status.
	@Column(nullable = false)
	@JsonIgnore
	@Getter
	@Setter
	private OrderStatusEnum		statusEnum;

	// The parent order group.
	@Column(nullable = true)
	@ManyToOne(optional = true)
	@JsonIgnore
	@Getter
	@Setter
	private OrderGroup			orderGroup;

	// The work sequence.
	// This is a sort of the actively working order groups in a facility.
	// Lower numbers work first.
	@Column(nullable = true)
	@JsonIgnore
	@Getter
	@Setter
	private Integer				workSequence;

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	private Facility			parent;

	// For a network this is a list of all of the users that belong in the set.
	@OneToMany(mappedBy = "parent")
	@JsonIgnore
	@Getter
	private List<OrderDetail>	orderDetails	= new ArrayList<OrderDetail>();

	public OrderHeader() {
		statusEnum = OrderStatusEnum.CREATED;
	}

	@JsonIgnore
	public final ITypedDao<OrderHeader> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "P";
	}

	@JsonIgnore
	public final Facility getParentFacility() {
		return parent;
	}

	public final void setParentFacility(final Facility inFacility) {
		parent = inFacility;
	}

	@JsonIgnore
	public final IDomainObject getParent() {
		return parent;
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Facility) {
			setParentFacility((Facility) inParent);
		}
	}

	@JsonIgnore
	public String getOrderId() {
		return getShortDomainId();
	}

	public final void setOrderId(String inOrderId) {
		setShortDomainId(inOrderId);
	}

	@JsonIgnore
	public final List<? extends IDomainObject> getChildren() {
		return getOrderDetails();
	}

	@JsonIgnore
	public final OrderDetail findOrderDetail(String inOrderDetailId) {
		OrderDetail result = null;

		for (OrderDetail orderDetail : getOrderDetails()) {
			if (orderDetail.getShortDomainId().equals(inOrderDetailId)) {
				result = orderDetail;
				break;
			}
		}

		return result;
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addOrderDetail(OrderDetail inOrderDetail) {
		orderDetails.add(inOrderDetail);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeOrderDetail(OrderDetail inOrderDetail) {
		orderDetails.remove(inOrderDetail);
	}
}
