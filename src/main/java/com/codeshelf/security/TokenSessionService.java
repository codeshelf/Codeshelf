package com.codeshelf.security;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.util.StringUIConverter;
import com.google.inject.Inject;

public class TokenSessionService extends AbstractCookieSessionService {
	static final Logger	LOGGER								= LoggerFactory.getLogger(TokenSessionService.class);
	private static final String	TOKEN_DEFAULT_XOR					= "00";
	@Getter
	byte[]				tokenXor;

	// session settings
	private static final int	SESSION_DEFAULT_MAX_FUTURE_SECONDS	= 30;												// do not allow timestamps significantly in the future
	private static final int	SESSION_DEFAULT_MAX_IDLE_MINUTES	= 6;
	private static final int	SESSION_DEFAULT_MIN_IDLE_MINUTES	= 5;
	@Getter
	int					sessionMaxFutureSeconds;
	@Getter
	int					sessionMaxIdleMinutes;
	@Getter
	int					sessionMinIdleMinutes;

	// cookie settings
	static final String	COOKIE_NAME							= "CSTOK";
	private static final int	COOKIE_DEFAULT_MAX_AGE_HOURS		= 24;
	private static final String	COOKIE_DEFAULT_DOMAIN				= "";
	@Getter
	String				cookieDomain;
	@Getter
	boolean				cookieSecure;
	@Getter
	int					cookieMaxAgeHours;

	// username/password settings
	static final int	USERNAME_MIN_LEN					= 1;
	static final int	USERNAME_MAX_LEN					= 254; // probably shouldn't be more than this
	private static final int	PASSWORD_DEFAULT_MIN_LEN			= 6;
	private static final int	PASSWORD_DEFAULT_MAX_LEN			= 32;
	@Getter
	int					passwordMinLength;
	@Getter
	int					passwordMaxLength;
	@Getter
	boolean				passwordRequireSymbol;																	// defaults false
	@Getter
	boolean				passwordRequireMixed;

	@Inject
	static TokenSessionService	theInstance;

	public static TokenSessionService getInstance() {
		return theInstance;
	}

	public static void setInstance(TokenSessionService instance) {
		theInstance = instance; // for testing only
	}

	/**************************** service methods ****************************/

	public TokenSessionService initialize() {
		// set up XOR applied to token
		String xorString = System.getProperty("auth.token.xor");
		if (xorString != null && xorString.length() > 1) {
			try {
				this.tokenXor = StringUIConverter.hexStringToBytes(xorString);
			} catch (NumberFormatException e) {
				LOGGER.error("could not parse auth.cookie.xor value: {}", xorString);
			}
		} else {
			this.tokenXor = StringUIConverter.hexStringToBytes(TOKEN_DEFAULT_XOR);
		}

		// initialize Message Authentication Code
		String secret = System.getProperty("auth.token.secret");
		this.initializeHmac(secret);

		// session settings
		this.sessionMaxFutureSeconds = Integer.getInteger("auth.session.maxfutureseconds", SESSION_DEFAULT_MAX_FUTURE_SECONDS);
		this.sessionMaxIdleMinutes = Integer.getInteger("auth.session.maxidleminutes", SESSION_DEFAULT_MAX_IDLE_MINUTES);
		this.sessionMinIdleMinutes = Integer.getInteger("auth.session.minidleminutes", SESSION_DEFAULT_MIN_IDLE_MINUTES);

		// cookie settings
		String cookieDomain = System.getProperty("auth.cookie.domain");
		if (cookieDomain != null)
			this.cookieDomain = cookieDomain;
		else
			this.cookieDomain = COOKIE_DEFAULT_DOMAIN;
		this.cookieSecure = Boolean.getBoolean("auth.cookie.secure");
		this.cookieMaxAgeHours = Integer.getInteger("auth.cookie.maxagehours", COOKIE_DEFAULT_MAX_AGE_HOURS);

		// password settings
		this.passwordMinLength = Integer.getInteger("auth.password.length.min", PASSWORD_DEFAULT_MIN_LEN);
		this.passwordMaxLength = Integer.getInteger("auth.password.length.max", PASSWORD_DEFAULT_MAX_LEN);
		this.passwordRequireSymbol = Boolean.getBoolean("auth.password.require.symbol");
		this.passwordRequireMixed = Boolean.getBoolean("auth.password.require.mixedcase");

		return this;
	}

	@Override
	protected void startUp() throws Exception {
		initialize();
	}

	@Override
	protected void shutDown() throws Exception {
	}

	@Override
	String getCookieName() {
		return COOKIE_NAME;
	}

	@Override
	int getUsernameMinLength() {
		return USERNAME_MIN_LEN;
	}

	@Override
	int getUsernameMaxLength() {
		return USERNAME_MAX_LEN;
	}

	public String testCreateToken(int userId, int tenantId) {
		// only for test use
		return createToken(userId,tenantId,null,null,null);
	}

}
