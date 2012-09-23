/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: User.java,v 1.8 2012/09/23 03:05:42 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

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
import org.codehaus.jackson.annotate.JsonIgnore;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
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
@CacheStrategy
public class User extends DomainObjectABC {

	@Inject
	public static ITypedDao<User>	DAO;

	@Singleton
	public static class UserDao extends GenericDaoABC<User> implements ITypedDao<User> {
		public final Class<User> getDaoClass() {
			return User.class;
		}
	}

	private static final Log	LOGGER				= LogFactory.getLog(User.class);

	private static final long	serialVersionUID	= 3001609308065821464L;

	// The hashed password
	// A User with a null hashed password is a promo user (with limited abilities).
	@Getter
	@Setter
	@Column(nullable = true)
	private String					hashedPassword;

	// Email.
	@Getter
	@Setter
	@Column(nullable = false)
	private String					email;

	// Create date.
	@Getter
	@Setter
	@Column(nullable = false)
	private Timestamp				created;

	// Is it active.
	@Getter
	@Setter
	@Column(nullable = false)
	private Boolean					active;

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parent")
	@Getter
	private List<UserSession>		userSessions	= new ArrayList<UserSession>();

	// The owning facility.
	@Column(name = "parentOrganization", nullable = false)
	@ManyToOne(optional = false)
	private Organization			parent;

	public User() {
		email = "";
		created = new Timestamp(System.currentTimeMillis());
		active = true;
	}

	@JsonIgnore
	public final ITypedDao<User> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "U";
	}

	public final Organization getParentOrganization() {
		return parent;
	}

	public final void setParentOrganization(final Organization inOrganization) {
		parent = inOrganization;
	}

	public final IDomainObject getParent() {
		return getParentOrganization();
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Organization) {
			setParentOrganization((Organization) inParent);
		}
	}

	@JsonIgnore
	public final List<? extends IDomainObject> getChildren() {
		return getUserSessions();
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addUserSession(UserSession inPromoCodeUse) {
		userSessions.add(inPromoCodeUse);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeUserSession(UserSession inPromoCodeUse) {
		userSessions.remove(inPromoCodeUse);
	}
}
