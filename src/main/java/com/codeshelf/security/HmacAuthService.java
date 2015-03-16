package com.codeshelf.security;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.Md5Crypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.manager.User;
import com.codeshelf.security.AuthResponse.Status;
import com.codeshelf.service.AbstractCodeshelfIdleService;
import com.codeshelf.util.StringUIConverter;
import com.google.inject.Inject;

import edu.emory.mathcs.backport.java.util.Arrays;

public class HmacAuthService extends AbstractCodeshelfIdleService implements AuthProviderService {
	private static final Logger	LOGGER								= LoggerFactory.getLogger(HmacAuthService.class);
	private static final String	HMAC_ALGORITHM						= "HmacSHA1";

	// token settings
	private static final int	TOKEN_VERSION						= 1;
	private static final String	TOKEN_DEFAULT_XOR					= "00";
	private byte[]				tokenXor;

	// session settings
	private static final int	SESSION_DEFAULT_MAX_FUTURE_SECONDS	= 30;												// do not allow timestamps significantly in the future
	private static final int	SESSION_DEFAULT_MAX_IDLE_MINUTES	= 6;
	private static final int	SESSION_DEFAULT_MIN_IDLE_MINUTES	= 5;
	private int					sessionMaxFutureSeconds;
	private int					sessionMaxIdleMinutes;
	private int					sessionMinIdleMinutes;

	// cookie settings
	private static final String	COOKIE_NAME							= "CSTOK";
	private static final int	COOKIE_DEFAULT_MAX_AGE_HOURS		= 24;
	private static final String	COOKIE_DEFAULT_DOMAIN				= "";
	private String				cookieDomain;
	private boolean				cookieSecure;
	private int					cookieMaxAgeHours;

	// reusable hash generator
	private Mac					mac;

	@Inject
	static AuthProviderService	theInstance;

	public static AuthProviderService getInstance() {
		return theInstance;
	}

	/**************************** token methods ****************************/

	@Override
	public String createToken(int id) {
		long timestamp = System.currentTimeMillis();
		byte[] rawHmac = createHmacBytes(id, timestamp);
		return encodeToken(rawHmac);
	}

	@Override
	public AuthResponse checkToken(String value) {
		AuthResponse resp = null;
		ByteBuffer hmac = ByteBuffer.wrap(decodeToken(value));
		if (hmac.remaining() > (4 + 4 + 8)) {
			int version = hmac.getInt();
			if (version == TOKEN_VERSION) {
				int id = hmac.getInt();
				long timestamp = hmac.getLong();
				byte[] matchHmac = createHmacBytes(id, timestamp);
				if (Arrays.equals(hmac.array(), matchHmac)) {
					resp = respondToValidToken(id, timestamp);
				} else {
					LOGGER.warn("Invalid HMAC for user ID {} timestamp {}", id, timestamp);
					resp = new AuthResponse(Status.INVALID_TOKEN);
				}
			} else {
				LOGGER.warn("Failed to parse auth token, bad version {}", version);
				resp = new AuthResponse(Status.INVALID_TOKEN);
			}
		} else {
			LOGGER.warn("auth token was too short, {} bytes", hmac.remaining());
			resp = new AuthResponse(Status.INVALID_TOKEN);
		}
		return resp;
	}

	private byte[] createHmacBytes(int id, long timestamp) {
		ByteBuffer hmac_data = ByteBuffer.allocate(4 + 4 + 8);
		hmac_data.putInt(TOKEN_VERSION);
		hmac_data.putInt(id);
		hmac_data.putLong(timestamp);

		byte[] hmac_signature;
		synchronized (mac) {
			hmac_signature = mac.doFinal(hmac_data.array());
		}
		ByteBuffer hmac = ByteBuffer.allocate(hmac_data.position() + hmac_signature.length);
		hmac.put(hmac_data.array());
		hmac.put(hmac_signature);
		return hmac.array();
	}

	private AuthResponse respondToValidToken(int id, long timestamp) {
		AuthResponse response;
		User user = TenantManagerService.getInstance().getUser(id);
		if (user != null) {
			if (user.isLoginAllowed()) {
				long ageSeconds = (System.currentTimeMillis() - timestamp) / 1000L;
				if (ageSeconds > (0 - this.sessionMaxFutureSeconds)) {
					// timestamp is not in the future
					if (ageSeconds < this.sessionMaxIdleMinutes* 60) {
						// session is still active
						String refreshToken = null;
						if (ageSeconds > this.sessionMinIdleMinutes * 60) {
							// if token is valid but getting old, offer an updated one
							LOGGER.info("refreshing cookie for user {}", id);
							refreshToken = this.createToken(id);
						}
						response = new AuthResponse(Status.ACCEPTED, user, timestamp, refreshToken);
					} else {
						LOGGER.warn("session timed out for user {}", id);
						response = new AuthResponse(Status.SESSION_IDLE_TIMEOUT, user, timestamp, null);
					}
				} else {
					LOGGER.error("ALERT - future timestamp {} authenticated HMAC for user {}", timestamp, id);
					response = new AuthResponse(Status.INVALID_TIMESTAMP, user, timestamp, null);
				}
			} else {
				LOGGER.error("ALERT - login not allowed for user {}", id);
				response = new AuthResponse(Status.LOGIN_NOT_ALLOWED, user);
			}
		} else {
			LOGGER.error("ALERT - invalid user id {} with authenticated HMAC", id);
			response = new AuthResponse(Status.BAD_CREDENTIALS, null);
		}
		return response;
	}

	private String encodeToken(byte[] rawHmac) {
		xor(rawHmac);
		return new String(Base64.encodeBase64(rawHmac));
	}

	private byte[] decodeToken(String cookieValue) {
		byte[] rawHmac = Base64.decodeBase64(cookieValue);
		xor(rawHmac);
		return rawHmac;
	}

	private void xor(byte[] buf) {
		int xi = 0;
		for (int i = 0; i < buf.length; i++) {
			buf[i] ^= tokenXor[xi];
			xi = ((xi + 1) % tokenXor.length);
		}
	}

	/**************************** cookie methods ****************************/

	public String getCookieName() {
		return COOKIE_NAME;
	}

	@Override
	public AuthResponse checkAuthCookie(Cookie[] cookies) {
		if (cookies == null)
			return null;

		Cookie match = null;
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(COOKIE_NAME)) {
				if (match == null) {
					match = cookie;
				} else {
					LOGGER.warn("more than one auth cookie found");
					return null;
				}
			}
		}
		if (match != null)
			return checkToken(match.getValue());
		//else
		return null;
	}

	public Cookie createAuthCookie(String token) {
		Cookie cookie = new Cookie(COOKIE_NAME, token);
		cookie.setPath("/");
		cookie.setDomain(this.cookieDomain);
		cookie.setVersion(0);
		cookie.setMaxAge(this.cookieMaxAgeHours * 60 * 60);
		cookie.setSecure(this.cookieSecure);
		return cookie;
	}

	public Cookie createAuthCookie(int id) {
		String hmacToken = createToken(id);
		return createAuthCookie(hmacToken);
	}

	/**************************** password hash methods ****************************/

	public String hashPassword(final String password) {
		return Md5Crypt.apr1Crypt(password);
	}

	public boolean checkPassword(final String password, final String hash) {
		return Md5Crypt.apr1Crypt(password, hash).equals(hash);
	}

	public boolean passwordMeetsRequirements(String password) {
		if (password == null)
			return false;
		if (password.isEmpty())
			return false;

		return true;
	}

	public boolean hashIsValid(String hash) {
		return hash.startsWith("$apr1$");
	}

	/**************************** service methods ****************************/

	public AuthProviderService initialize() {
		// initialize Message Authentication Code
		String secret = System.getProperty("auth.token.secret");
		SecretKeySpec hmacKey = new SecretKeySpec(secret.getBytes(), HMAC_ALGORITHM);
		try {
			mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(hmacKey);
		} catch (InvalidKeyException | NoSuchAlgorithmException e) {
			throw new RuntimeException("Unexpected exception creating HMAC", e);
		}

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

		return this;
	}

	@Override
	protected void startUp() throws Exception {
		initialize();
	}

	@Override
	protected void shutDown() throws Exception {
		mac = null;
		tokenXor = null;
	}

}
