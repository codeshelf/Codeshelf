/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: UserSession.java,v 1.8 2012/04/10 08:01:19 jeffw Exp $
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

// --------------------------------------------------------------------------
/**
 * PromoCodeUse
 * 
 * Information gathered with each use of the promo code.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "USERSESSION")
public class UserSession extends PersistABC {

	private static final Log					LOGGER				= LogFactory.getLog(UserSession.class);

	private static final long					serialVersionUID	= 3001609308065821464L;

	// The owning CodeShelf network.
	@Column(name = "parentUser", nullable = false)
	@ManyToOne(optional = false)
	private User								parentUser;

	// Create date.
	@Getter
	@Setter
	@Column(nullable = false)
	private Timestamp							created;

	// Activity note.
	@Getter
	@Setter
	@Column(nullable = false)
	private String								note;

	public UserSession() {
		parentUser = null;
		created = new Timestamp(System.currentTimeMillis());
	}

	public final PersistABC getParent() {
		return getParentUser();
	}

	public final void setParent(PersistABC inParent) {
		if (inParent instanceof User) {
			setParentUser((User) inParent);
		}
	}
	
	public final User getParentUser() {
		// Yes, this is weird, but we MUST always return the same instance of these persistent objects.
		if (parentUser != null) {
//			parentUser = User.DAO.loadByPersistentId(parentUser.getPersistentId());
		}
		return parentUser;
	}

	public final void setParentUser(User inParentUser) {
		parentUser = inParentUser;
	}
}
