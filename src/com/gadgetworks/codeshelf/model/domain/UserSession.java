/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: UserSession.java,v 1.14 2013/03/10 08:58:43 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * UserSession
 * 
 * Information gathered as part of a user's session with the system.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "USERSESSION", schema = "CODESHELF")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class UserSession extends DomainObjectTreeABC<User> {

	@Inject
	public static ITypedDao<UserSession>	DAO;

	@Singleton
	public static class UserSessionDao extends GenericDaoABC<UserSession> implements ITypedDao<UserSession> {
		public final Class<UserSession> getDaoClass() {
			return UserSession.class;
		}
	}

	private static final Logger	LOGGER				= LoggerFactory.getLogger(UserSession.class);

	private static final long	serialVersionUID	= 3001609308065821464L;

	// The owning CodeShelf network.
	@Column(name = "parentUser", nullable = false)
	@ManyToOne(optional = false)
	protected User				parent;

	// Create date.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	protected Timestamp			created;

	// Activity note.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	protected String			note;

	public UserSession() {
		created = new Timestamp(System.currentTimeMillis());
	}

	public final ITypedDao<UserSession> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "US";
	}

	public final User getParent() {
		return parent;
	}

	public final void setParent(User inParent) {
		parent = inParent;
	}

	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}
}
