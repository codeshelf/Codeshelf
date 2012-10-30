/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderDetail.java,v 1.10 2012/10/30 15:21:34 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
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
 * OrderDetail
 * 
 * An order detail is a request for items/SKUs from the facility.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "ORDERDETAIL")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class OrderDetail extends DomainObjectABC {

	@Inject
	public static ITypedDao<OrderDetail>	DAO;

	@Singleton
	public static class OrderDetailDao extends GenericDaoABC<OrderDetail> implements ITypedDao<OrderDetail> {
		public final Class<OrderDetail> getDaoClass() {
			return OrderDetail.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(OrderDetail.class);

	// The collective order status.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private OrderStatusEnum		statusEnum;

	// The item master.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@Getter
	@Setter
	private ItemMaster			itemMaster;

	// The description.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String				description;

	// The quantity.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Integer				quantity;

	// The UoM.
	@Column(nullable = false)
	@OneToOne
	@Getter
	@Setter
	private UomMaster			uomMaster;

	// Order date.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp			orderDate;

	// The owning order header.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private OrderHeader			parent;

	public OrderDetail() {
	}

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

	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	public final String getParentOrderID() {
		return getParentOrderHeader().getDomainId();
	}
	
	public final String getUomMasterId() {
		return getUomMaster().getDomainId();
	}
	
	public final String getItemMasterId() {
		return getItemMaster().getDomainId();
	}

}
