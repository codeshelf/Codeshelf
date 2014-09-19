/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: User.java,v 1.23 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
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
@Table(name = "user")
@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class User extends DomainObjectTreeABC<Organization> {

	@Inject
	public static ITypedDao<User>	DAO;

	@Singleton
	public static class UserDao extends GenericDaoABC<User> implements ITypedDao<User> {
		@Inject
		public UserDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}
		
		public final Class<User> getDaoClass() {
			return User.class;
		}
	}

	private static final Logger	LOGGER				= LoggerFactory.getLogger(User.class);

	public static final String	PBKDF2_ALGORITHM	= "PBKDF2WithHmacSHA1";

	// The following constants may be changed without breaking existing hashes.
	public static final int		SALT_BYTES			= 24;
	public static final int		HASH_BYTES			= 24;
	public static final int		PBKDF2_ITERATIONS	= 1000;

	public static final int		ITERATION_INDEX		= 0;
	public static final int		SALT_INDEX			= 1;
	public static final int		PBKDF2_INDEX		= 2;

	// The owning organization.
	@Column(name = "parentOrganization", nullable = false)
	@ManyToOne(optional = false)
	private Organization		parent;

	// The hash salt.
	@Column(nullable = false)
	@NonNull
	@Getter
	@Setter
	//@JsonProperty
	private String				hashSalt;

	// The hash iterations.
	@Column(nullable = false)
	@NonNull
	@Getter
	@Setter
	//@JsonProperty
	private Integer				hashIterations;

	// The hashed password.
	@Column(nullable = false)
	@NonNull
	// It's not safe to expose these values outside this object!
	//	@Getter
	//	@Setter
	//@JsonProperty
	private String				hashedPassword;

	// Site controller - if present, this user is linked to a site controller 
	@Column(nullable = true)
	@ManyToOne(optional = true)
	@Getter
	@Setter
	private SiteController		siteController;

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
		created = new Timestamp(System.currentTimeMillis());
		active = true;
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<User> getDao() {
		return DAO;
	}
	
	public final static void setDao(ITypedDao<User> dao) {
		User.DAO = dao;
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

	public final void setPassword(final String inPassword) {
		try {
			// Hash the password
			byte[] salt = generateSalt();
			setHashSalt(toHex(salt
				));
			hashedPassword = hashPassword(inPassword, salt, PBKDF2_ITERATIONS);
			hashIterations = PBKDF2_ITERATIONS;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			LOGGER.error("", e);
		}
	}
	
	private static byte[] generateSalt() {
		SecureRandom random = new SecureRandom();
		byte[] salt = new byte[SALT_BYTES];
		random.nextBytes(salt);
		return salt;

	}
	
	public static String hashPassword(String inPlainTextPassword, byte[] salt, int iterations) throws NoSuchAlgorithmException, InvalidKeySpecException {
		// Hash the password
		byte[] hash = pbkdf2(inPlainTextPassword.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BYTES);
		return toHex(hash);
	}

	/**
	 * Validates a password using a hash.
	 *
	 * @param   password    the password to check
	 * @param   inGoodHash    the hash of the valid password
	 * @return              true if the password is correct, false if not
	 */
	public final boolean isPasswordValid(final String inPassword) {
		boolean result = false;

		try {
			// Compute the hash of the provided password, using the same salt, iteration count, and hash length
			byte[] testHash = pbkdf2(inPassword.toCharArray(), fromHex(hashSalt), hashIterations, fromHex(hashedPassword).length);
			// Compare the hashes in constant time. The password is correct if both hashes match.
			result = slowEquals(fromHex(hashedPassword), testHash);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	/**
	 * Compares two byte arrays in length-constant time. This comparison method
	 * is used so that password hashes cannot be extracted from an on-line
	 * system using a timing attack and then attacked off-line.
	 *
	 * @param   inArray1       the first byte array
	 * @param   inArray2       the second byte array
	 * @return          true if both byte arrays are the same, false if not
	 */
	private static boolean slowEquals(byte[] inArray1, byte[] inArray2) {
		int diff = inArray1.length ^ inArray2.length;
		for (int i = 0; i < inArray1.length && i < inArray2.length; i++) {
			diff |= inArray1[i] ^ inArray2[i];
		}
		return diff == 0;
	}

	/**
	 *  Computes the PBKDF2 hash of a password.
	 *
	 * @param   inPassword    the password to hash.
	 * @param   inSalt        the salt
	 * @param   inIterations  the iteration count (slowness factor)
	 * @param   inBytes       the length of the hash to compute in bytes
	 * @return              the PBDKF2 hash of the password
	 */
	private static byte[] pbkdf2(char[] inPassword, byte[] inSalt, int inIterations, int inBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
		PBEKeySpec spec = new PBEKeySpec(inPassword, inSalt, inIterations, inBytes * 8);
		SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
		return skf.generateSecret(spec).getEncoded();
	}

	/**
	 * Converts a string of hexadecimal characters into a byte array.
	 *
	 * @param   inHex         the hex string
	 * @return              the hex string decoded into a byte array
	 */
	private static byte[] fromHex(String inHex) {
		byte[] binary = new byte[inHex.length() / 2];
		for (int i = 0; i < binary.length; i++) {
			binary[i] = (byte) Integer.parseInt(inHex.substring(2 * i, 2 * i + 2), 16);
		}
		return binary;
	}

	/**
	 * Converts a byte array into a hexadecimal string.
	 *
	 * @param   inArray       the byte array to convert
	 * @return              a length*2 character string encoding the byte array
	 */
	private static String toHex(byte[] inArray) {
		BigInteger bi = new BigInteger(1, inArray);
		String hex = bi.toString(16);
		int paddingLength = (inArray.length * 2) - hex.length();
		if (paddingLength > 0)
			return String.format("%0" + paddingLength + "d", 0) + hex;
		else
			return hex;
	}
	
	/**
	 * Generate a sql statement that can be executed at the database to chance a user's password.  
	 * 
	 * TODO this is expected to be shortlived until there is a remote admin function
	 * 
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchAlgorithmException 
	 * 
	 */
	public static String generatePasswordUpdateSql(String inOrganizationName, String inEmail, String inPassword) throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] salt = generateSalt();
		int hashIterations = PBKDF2_ITERATIONS;
		String passwordOut = hashPassword(inPassword, salt, PBKDF2_ITERATIONS);
		String sql = "UPDATE codeshelf.\"user\"" +
				" SET " +
				"hash_salt='"+toHex(salt)+"', hashed_password='"+ passwordOut +"', hash_iterations="+ hashIterations +
				" WHERE parent_persistentid = (Select persistentid from codeshelf.organization where domainId = '" + inOrganizationName + "') AND domainId = '" + inEmail + "';";
		return sql;
	}

}
