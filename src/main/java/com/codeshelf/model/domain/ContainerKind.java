/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ContainerKind.java,v 1.15 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

// --------------------------------------------------------------------------
/**
 * ContainerKind
 * 
 * Physical characteristics of a container class.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "container_kind", uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})})
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class ContainerKind extends DomainObjectTreeABC<Facility> {

	public static class ContainerKindDao extends GenericDaoABC<ContainerKind> implements ITypedDao<ContainerKind> {
		public final Class<ContainerKind> getDaoClass() {
			return ContainerKind.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger	LOGGER					= LoggerFactory.getLogger(ContainerKind.class);

	public static final String	DEFAULT_CONTAINER_KIND	= "DEFAULT";

	// The container class ID.
	@Column(nullable = false,name="class_id")
	@Getter
	@Setter
	@JsonProperty
	private String				classId;

	// Length.
	@Column(nullable = false,name="length_meters")
	@Getter
	@Setter
	@JsonProperty
	private Double				lengthMeters;

	// Width.
	@Column(nullable = false,name="width_meters")
	@Getter
	@Setter
	@JsonProperty
	private Double				widthMeters;

	// Height.
	@Column(nullable = false,name="height_meters")
	@Getter
	@Setter
	@JsonProperty
	private Double				heightMeters;
	
	@OneToMany(mappedBy = "kind", orphanRemoval=true)
	private List<Container>					containers;

	public ContainerKind() {
		classId = "";
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<ContainerKind> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<ContainerKind> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(ContainerKind.class);
	}

	public final String getDefaultDomainIdPrefix() {
		return "P";
	}

	@Override
	public Facility getFacility() {
		return getParent().getFacility();
	}

}
