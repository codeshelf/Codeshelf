/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PromoCode.java,v 1.1 2012/01/11 18:13:15 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;

// --------------------------------------------------------------------------
/**
 * PromoCode
 * 
 * This holds all of the information about limited-time use codes we send to prospects.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "PROMOCODE")
public class PromoCode extends PersistABC {

	public static final GenericDao<PromoCode>	DAO					= new GenericDao<PromoCode>(PromoCode.class);

	private static final Log					LOGGER				= LogFactory.getLog(PromoCode.class);

	private static final long					serialVersionUID	= 3001609308065821464L;

	// The Promo code.
	@Getter
	@Setter
	@Column(nullable = false)
	private String								promoCode;

	// The browser cookie use for the code code.
	@Getter
	@Setter
	@Column(nullable = false)
	private String								cookie;

	// Email.
	@Getter
	@Setter
	@Column(nullable = false)
	private String								email;

	// Create date.
	@Getter
	@Setter
	@Column(nullable = false)
	private Timestamp							created;

	// Is it active.
	@Getter
	@Setter
	@Column(nullable = false)
	private Boolean								active;

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parentPromoCode")
	private List<PromoCodeUse>					uses				= new ArrayList<PromoCodeUse>();

	public PromoCode() {
		promoCode = "";
		email = "";
		created = new Timestamp(System.currentTimeMillis());
		active = true;
	}

	// We always need to return the object cached in the DAO.
	public final List<PromoCodeUse> getPromoCodeUses() {
		if (IGenericDao.USE_DAO_CACHE) {
			List<PromoCodeUse> result = new ArrayList<PromoCodeUse>();
			if (!PromoCodeUse.DAO.isObjectPersisted(this)) {
				result = uses;
			} else {
				for (PromoCodeUse promoCodeUse : PromoCodeUse.DAO.getAll()) {
					if (promoCodeUse.getParentPromoCode().equals(this)) {
						result.add(promoCodeUse);
					}
				}
			}
			return result;
		} else {
			return uses;
		}
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addPromoCodeUse(PromoCodeUse inPromoCodeUse) {
		uses.add(inPromoCodeUse);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removePromoCodeUse(PromoCodeUse inPromoCodeUse) {
		uses.remove(inPromoCodeUse);
	}
}
