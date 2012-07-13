/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: User.java,v 1.13 2012/07/13 21:56:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * User
 * 
 * This holds all of the information about limited-time use codes we send to prospects.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "USER")
public class User extends PersistABC {

	private static final Log	LOGGER				= LogFactory.getLog(User.class);

	private static final long	serialVersionUID	= 3001609308065821464L;

	@Singleton
	public static class UserDao extends GenericDao<User> implements ITypedDao<User> {
		public UserDao() {
			super(User.class);
		}
	}

	@Inject
	public static ITypedDao<User> DAO;

	// The hashed password
	// A User with a null hashed password is a promo user (with limited abilities).
	@Getter
	@Setter
	@Column(nullable = true)
	private String				hashedPassword;

	// Email.
	@Getter
	@Setter
	@Column(nullable = false)
	private String				email;

	// Create date.
	@Getter
	@Setter
	@Column(nullable = false)
	private Timestamp			created;

	// Is it active.
	@Getter
	@Setter
	@Column(nullable = false)
	private Boolean				active;

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parentUser")
	@Getter
	private List<UserSession>	users	= new ArrayList<UserSession>();

	// The owning facility.
	@Column(name = "parentOrganization", nullable = false)
	@ManyToOne(optional = false)
	@Getter
	@Setter
	private Organization		parentOrganization;

	public User() {
		email = "";
		created = new Timestamp(System.currentTimeMillis());
		active = true;
	}

	public final PersistABC getParent() {
		return getParentOrganization();
	}

	public final void setParent(PersistABC inParent) {
		if (inParent instanceof Organization) {
			setParentOrganization((Organization) inParent);
		}
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addUserSession(UserSession inPromoCodeUse) {
		users.add(inPromoCodeUse);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeUserSession(UserSession inPromoCodeUse) {
		users.remove(inPromoCodeUse);
	}
}
