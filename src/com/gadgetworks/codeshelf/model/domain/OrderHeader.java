/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderHeader.java,v 1.15 2012/11/19 10:48:25 jeffw Exp $
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
import com.gadgetworks.codeshelf.model.PickStrategyEnum;
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
@Table(name = "ORDERHEADER", schema = "CODESHELF")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class OrderHeader extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<OrderHeader>	DAO;

	@Singleton
	public static class OrderHeaderDao extends GenericDaoABC<OrderHeader> implements ITypedDao<OrderHeader> {
		public final Class<OrderHeader> getDaoClass() {
			return OrderHeader.class;
		}
	}

	private static final Log	LOGGER			= LogFactory.getLog(OrderHeader.class);

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

	// The pick strategy.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private PickStrategyEnum	pickStrategyEnum;

	// The parent order group.
	@Column(nullable = true)
	@ManyToOne(optional = true)
	@Getter
	@Setter
	private OrderGroup			orderGroup;

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
	private List<OrderDetail>	orderDetails	= new ArrayList<OrderDetail>();

	public OrderHeader() {
		statusEnum = OrderStatusEnum.CREATED;
		pickStrategyEnum = PickStrategyEnum.SERIAL;
	}

	public final ITypedDao<OrderHeader> getDao() {
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

	public final String getOrderId() {
		return getDomainId();
	}

	public final void setOrderId(String inOrderId) {
		setDomainId(inOrderId);
	}

	public final List<? extends IDomainObject> getChildren() {
		return getOrderDetails();
	}

	public final OrderDetail findOrderDetail(String inOrderDetailId) {
		OrderDetail result = null;

		for (OrderDetail orderDetail : getOrderDetails()) {
			if (orderDetail.getDomainId().equals(inOrderDetailId)) {
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
