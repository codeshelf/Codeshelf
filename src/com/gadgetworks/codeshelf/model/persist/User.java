/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: User.java,v 1.3 2012/03/17 09:07:03 jeffw Exp $
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
import com.gadgetworks.codeshelf.model.dao.IGenericDao;

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
	
	public interface IUserDao extends IGenericDao<User> {
		
	}
	
	public static class UserDao extends GenericDao<User> implements IUserDao {
		public UserDao() {
			super(User.class);
		}
	}
	
	//public static final GenericDao<User>	DAO					= new GenericDao<User>(User.class);

	private static final Log				LOGGER				= LogFactory.getLog(User.class);

	private static final long				serialVersionUID	= 3001609308065821464L;

	// The hashed password
	// A User with a null hashed password is a promo user (with limited abilities).
	@Getter
	@Setter
	@Column(nullable = true)
	private String							hashedPassword;

	// Email.
	@Getter
	@Setter
	@Column(nullable = false)
	private String							email;

	// Create date.
	@Getter
	@Setter
	@Column(nullable = false)
	private Timestamp						created;

	// Is it active.
	@Getter
	@Setter
	@Column(nullable = false)
	private Boolean							active;

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parentUser")
	private List<UserSession>				uses				= new ArrayList<UserSession>();

	// The owning facility.
	@Column(name = "parentOrganization", nullable = false)
	@ManyToOne
	private Organization						parentOrganization;

	public User() {
		email = "";
		created = new Timestamp(System.currentTimeMillis());
		active = true;
	}

	public final Organization getParentOrganization() {
		// Yes, this is weird, but we MUST always return the same instance of these persistent objects.
		if (parentOrganization != null) {
			parentOrganization = Organization.DAO.loadByPersistentId(parentOrganization.getPersistentId());
		}
		return parentOrganization;
	}

	public final void setparentOrganization(Organization inparentOrganization) {
		parentOrganization = inparentOrganization;
	}

	// We always need to return the object cached in the DAO.
	public final List<UserSession> getUserSessions() {
		if (IGenericDao.USE_DAO_CACHE) {
			List<UserSession> result = new ArrayList<UserSession>();
			if (!UserSession.DAO.isObjectPersisted(this)) {
				result = uses;
			} else {
				for (UserSession promoCodeUse : UserSession.DAO.getAll()) {
					if (promoCodeUse.getParentUser().equals(this)) {
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
	public final void addUserSession(UserSession inPromoCodeUse) {
		uses.add(inPromoCodeUse);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeUserSession(UserSession inPromoCodeUse) {
		uses.remove(inPromoCodeUse);
	}
}
