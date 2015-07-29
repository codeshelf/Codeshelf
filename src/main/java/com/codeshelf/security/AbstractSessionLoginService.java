package com.codeshelf.security;

import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.commons.lang.CharSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.User;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.security.TokenSession.Status;

public abstract class AbstractSessionLoginService extends AbstractHmacTokenService {
	static final Logger	LOGGER								= LoggerFactory.getLogger(AbstractSessionLoginService.class);


	abstract int getUsernameMinLength();
	abstract int getUsernameMaxLength();
	abstract int getPasswordMinLength();
	abstract int getPasswordMaxLength();
	abstract boolean isPasswordRequireMixed();
	abstract boolean isPasswordRequireSymbol();
	static final String	ALLOWED_PASSWORD_SYMBOLS	= "!@#$%^&*()-_+={}[]<>:;,~`?/|\\ .";
	public AbstractSessionLoginService() {
		super();
	}

	/**************************** password hash methods ****************************/
	public TokenSession authenticate(String username, String password) {
		return authenticate(username,password,null);
	}

	public TokenSession authenticate(String username, String password, String version) {
		TokenSession response = null;
		User user = TenantManagerService.getInstance().getUser(username);
		if (user != null) {
			boolean passwordValid = checkPassword(password, user.getHashedPassword());
			Tenant tenant = user.getTenant();
			if (tenant.isActive()) {
				if (user.isLoginAllowed()) {
					if (passwordValid) {
						// set last authenticated before checking client version
						user.setLastAuthenticated();
						TenantManagerService.getInstance().updateUser(user);

						if(version == null || tenant.clientVersionIsCompatible(version)) {
							LOGGER.info("Creating token for user {}", user);
							long timestamp = System.currentTimeMillis();

							String token = this.createToken(user.getId(), tenant.getId(), timestamp, timestamp, null); // create default token for new session
							response = new TokenSession(Status.ACTIVE_SESSION,
								user,
								tenant,
								timestamp,
								timestamp,
								null, // no flags specified
								token); // offer token
						} else {
							LOGGER.info("Incompatible client version {} for user {}", version, user);
							response = new TokenSession(Status.INCOMPATIBLE_VERSION, user);
						}
					} else {
						LOGGER.info("Invalid password for user {}", user);
						response = new TokenSession(Status.BAD_CREDENTIALS, user);
					}
				} else {
					LOGGER.warn("user {} attempted login, login not allowed (password correct = {})", user, passwordValid);
					response = new TokenSession(Status.LOGIN_NOT_ALLOWED, user);
				}
			} else {
				LOGGER.warn("User of {} inactive tenant {} attempted login, password correct = {}", user, user.getTenant()
					.getName(), passwordValid);
				response = new TokenSession(Status.LOGIN_NOT_ALLOWED, user);
			}
		} else {
			LOGGER.info("User not found {}", user);
			response = new TokenSession(Status.BAD_CREDENTIALS, null);
		}
		
		return response; // never null 
	}

	public String hashPassword(final String password) {
		return Md5Crypt.apr1Crypt(password);
	}

	private boolean checkPassword(final String password, final String hash) {
		return Md5Crypt.apr1Crypt(password, hash).equals(hash);
	}
	
	public boolean hashIsValid(String hash) {
		return hash.startsWith("$apr1$");
	}

	public String describePasswordRequirements() {
		return String.format("Password must be between %d and %d characters long, consisting of letters, numbers and punctuation.%s%s",
			this.getPasswordMinLength(),
			this.getPasswordMaxLength(),
			this.isPasswordRequireMixed() ? " It must contain a mix of upper case, lower case and/or numbers." : "",
			this.isPasswordRequireSymbol() ? " It must contain at least one of these symbols: " + ALLOWED_PASSWORD_SYMBOLS : "");
	}

	public boolean passwordMeetsRequirements(String password) {
		if (password == null)
			return false;
		if (password.isEmpty())
			return false;
		if (password.length() < this.getPasswordMinLength() || password.length() > this.getPasswordMaxLength())
			return false;
		boolean hasSymbol = false;
		boolean hasLowercase = false;
		boolean hasUppercase = false;
		boolean hasNumber = false;
		for (int i = 0; i < password.length(); i++) {
			char ch = password.charAt(i);
			boolean lowercase = CharSet.ASCII_ALPHA_LOWER.contains(ch);
			boolean uppercase = CharSet.ASCII_ALPHA_UPPER.contains(ch);
			boolean number = CharSet.ASCII_NUMERIC.contains(ch);
			boolean symbol = ALLOWED_PASSWORD_SYMBOLS.indexOf(ch) >= 0;
			if (!lowercase && !uppercase && !number && !symbol)
				return false;
			hasLowercase |= lowercase;
			hasUppercase |= uppercase;
			hasSymbol |= symbol;
			hasNumber |= number;
		}
		if (this.isPasswordRequireMixed()) {
			int mix = (hasUppercase ? 1 : 0) + (hasLowercase ? 1 : 0) + (hasNumber ? 1 : 0);
			if (mix < 2)
				return false;
		}
		if (this.isPasswordRequireSymbol() && !hasSymbol)
			return false;
	
		return true;
	}

	public boolean usernameMeetsRequirements(String username) {
		if (username == null)
			return false;
		if (username.isEmpty())
			return false;
		if (username.length() < getUsernameMinLength() || username.length() > getUsernameMaxLength())
			return false;
		for (int i = 0; i < username.length(); i++) {
			char ch = username.charAt(i);
			if (!CharSet.ASCII_ALPHA.contains(ch) 
					&& !CharSet.ASCII_NUMERIC.contains(ch) 
					&& ALLOWED_PASSWORD_SYMBOLS.indexOf(ch) < 0)
				return false;
		}
		return true;
	}

}