/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: User.java,v 1.11 2012/10/31 16:55:08 jeffw Exp $
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
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

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
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class User extends DomainObjectTreeABC<Organization> {

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

	// The owning facility.
	@Column(name = "parentOrganization", nullable = false)
	@ManyToOne(optional = false)
	private Organization		parent;

	// The hashed password
	// A User with a null hashed password is a promo user (with limited abilities).
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private String				hashedPassword;

	// Email.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String				email;

	// Create date.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp			created;

	// Is it active.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Boolean				active;

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parent")
	@Getter
	private List<UserSession>	userSessions		= new ArrayList<UserSession>();

	public User() {
		email = "";
		created = new Timestamp(System.currentTimeMillis());
		active = true;
	}

	public final ITypedDao<User> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "U";
	}

	public final Organization getParent() {
		return parent;
	}

	public final void setParent(Organization inParent) {
		parent = inParent;
	}

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
