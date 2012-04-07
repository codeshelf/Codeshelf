/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Aisle.java,v 1.12 2012/04/07 19:42:16 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

// --------------------------------------------------------------------------
/**
 * CodeShelfNetwork
 * 
 * The CodeShelfNetwork object holds information about how to create a standalone CodeShelf network.
 * (There may be more than one running at a facility.)
 * 
 * @author jeffw
 */

@Entity
@Table(name = "LOCATION")
@DiscriminatorValue("AISLE")
public class Aisle extends Location {

	private static final Log	LOGGER	= LogFactory.getLog(Aisle.class);

//	@ManyToOne(optional = false)
//	@JoinColumn(name = "PARENTLOCATION_PERSISTENTID")
//	@Column(nullable = false, name = "parentLocation")
//	@JsonIgnore
//	@Setter
//	@Getter
//	private Facility			parentFacility;

	public Aisle() {

	}
	
	public Facility getParentFacility() {
		return (Facility) getParentLocation();
	}
	
	public void setParentFacility(Facility inParentFacility) {
		setParentLocation(inParentFacility);
	}
}
