/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PromoCodeUse.java,v 1.1 2012/01/11 18:13:15 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.dao.GenericDao;

// --------------------------------------------------------------------------
/**
 * PromoCodeUse
 * 
 * Information gathered with each use of the promo code.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "PROMOCODEUSE")
public class PromoCodeUse extends PersistABC {

	public static final GenericDao<PromoCodeUse>	DAO					= new GenericDao<PromoCodeUse>(PromoCodeUse.class);

	private static final Log						LOGGER				= LogFactory.getLog(PromoCodeUse.class);

	private static final long						serialVersionUID	= 3001609308065821464L;

	// The owning CodeShelf network.
	@Column(name = "parentPromoCode", nullable = false)
	@ManyToOne
	private PromoCode								parentPromoCode;

	// Create date.
	@Getter
	@Setter
	@Column(nullable = false)
	private Timestamp								created;

	// Activity note.
	@Getter
	@Setter
	@Column(nullable = false)
	private String									note;

	public PromoCodeUse() {
		parentPromoCode = null;
		created = new Timestamp(System.currentTimeMillis());
	}

	public final PromoCode getParentPromoCode() {
		// Yes, this is weird, but we MUST always return the same instance of these persistent objects.
		if (parentPromoCode != null) {
			parentPromoCode = PromoCode.DAO.loadByPersistentId(parentPromoCode.getPersistentId());
		}
		return parentPromoCode;
	}

	public final void setParentPromoCode(PromoCode inParentPromoCode) {
		parentPromoCode = inParentPromoCode;
	}
}
