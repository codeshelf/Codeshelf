/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderDetail.java,v 1.5 2012/10/03 06:39:02 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
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
 * OrderDetail
 * 
 * An order detail is a request for items/SKUs from the facility.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "ORDERDETAIL")
@CacheStrategy
public class OrderDetail extends DomainObjectABC {

	@Inject
	public static OrderDetailDao	DAO;

	@Singleton
	public static class OrderDetailDao extends GenericDaoABC<OrderDetail> implements ITypedDao<OrderDetail> {
		public final Class<OrderDetail> getDaoClass() {
			return OrderDetail.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(OrderDetail.class);

	// The collective order status.
	@Column(nullable = false)
	@JsonIgnore
	@Getter
	@Setter
	private OrderStatusEnum		statusEnum;

	// The item master.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@Getter
	@Setter
	private ItemMaster			itemMaster;

	// The description.
	@Getter
	@Setter
	@Column(nullable = false)
	private String				description;

	// The quantity.
	@Getter
	@Setter
	@Column(nullable = false)
	private Integer				quantity;

	// The UoM.
	@Getter
	@Setter
	@Column(nullable = false)
	private String				uomId;

	// Order date.
	@Getter
	@Setter
	@Column(nullable = false)
	private Timestamp			orderDate;

	// The owning order header.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	private OrderHeader			parent;

	public OrderDetail() {
	}

	@JsonIgnore
	public final ITypedDao<OrderDetail> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "PS";
	}

	public final OrderHeader getParentOrderHeader() {
		return parent;
	}

	public final void setParentOrderHeader(final OrderHeader inOrder) {
		parent = inOrder;
	}

	@JsonIgnore
	public String getOrderDetailId() {
		return getDomainId();
	}

	public final void setOrderDetailId(String inOrderDetailId) {
		setDomainId(inOrderDetailId);
	}

	public final IDomainObject getParent() {
		return getParentOrderHeader();
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof OrderHeader) {
			setParentOrderHeader((OrderHeader) inParent);
		}
	}

	@JsonIgnore
	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	public final String getParentOrderID() {
		return getParentOrderHeader().getDomainId();
	}

}
