/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderGroup.java,v 1.24 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
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
@Table(name = "order_group")
@CacheStrategy(useBeanCache = true)@JsonAutoDetect(getterVisibility = Visibility.NONE)
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

	public final static String			DEFAULT_ORDER_GROUP_DESC_PREFIX	= "Order group - ";

	private static final Logger			LOGGER							= LoggerFactory.getLogger(OrderGroup.class);

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Facility					parent;

	// The collective order status.
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private OrderStatusEnum				statusEnum;

	// The description.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String						description;

	// The work sequence.
	// This is a sort of the actively working order groups in a facility.
	// Lower numbers work first.
	@Column(nullable = true)
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

	// For a network this is a list of all of the users that belong in the set.
	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	private Map<String, OrderHeader>	orderHeaders					= new HashMap<String, OrderHeader>();

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

	public final void addOrderHeader(OrderHeader inOrderHeader) {
		orderHeaders.put(inOrderHeader.getDomainId(), inOrderHeader);
	}

	public final OrderHeader getOrderHeader(String inOrderid) {
		return orderHeaders.get(inOrderid);
	}

	public final void removeOrderHeader(String inOrderId) {
		orderHeaders.remove(inOrderId);
	}

	public final List<OrderHeader> getOrderHeaders() {
		return new ArrayList<OrderHeader>(orderHeaders.values());
	}

	public final List<? extends IDomainObject> getChildren() {
		return getOrderHeaders();
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
