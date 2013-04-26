/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ItemDdcGroup.java,v 1.1 2013/04/26 03:26:04 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * ItemDdcGroup
 * 
 * Information about a collection of items in a location that share a DDC.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "item_ddc_group", schema = "codeshelf")
@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class ItemDdcGroup extends DomainObjectTreeABC<ILocation> {

	@Inject
	public static ITypedDao<ItemDdcGroup>	DAO;

	@Singleton
	public static class ItemDdcGroupDao extends GenericDaoABC<ItemDdcGroup> implements ITypedDao<ItemDdcGroup> {
		@Inject
		public ItemDdcGroupDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<ItemDdcGroup> getDaoClass() {
			return ItemDdcGroup.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(ItemDdcGroup.class);

	// The parent location.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private LocationABC			parent;

	// The start position of this DDC group along the pick path.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Double				startPosAlongPath;

	// The end position of this DDC group along the pick path.
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Double				endPosAlongPath;

	public ItemDdcGroup() {

	}

	public final ITypedDao<ItemDdcGroup> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "DDC";
	}

	public final ILocation getParent() {
		return (ILocation) parent;
	}
	
	public final void setDdcGroupId(final String inDdcGroupId) {
		setDomainId(inDdcGroupId);
	}
	
	public final String getDdcGroupId() {
		return getDomainId();
	}

	public final void setParent(ILocation inParent) {
		parent = (LocationABC) inParent;
	}
}
