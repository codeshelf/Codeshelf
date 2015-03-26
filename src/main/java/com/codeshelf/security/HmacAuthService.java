package com.codeshelf.security;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.commons.lang.CharSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.manager.User;
import com.codeshelf.security.AuthResponse.Status;
import com.codeshelf.security.SessionFlags.Flag;
import com.codeshelf.service.AbstractCodeshelfIdleService;
import com.codeshelf.util.StringUIConverter;
import com.google.inject.Inject;

import edu.emory.mathcs.backport.java.util.Arrays;

public class HmacAuthService extends AbstractCodeshelfIdleService implements AuthProviderService {
	private static final Logger	LOGGER								= LoggerFactory.getLogger(HmacAuthService.class);
	private static final String	HMAC_ALGORITHM						= "HmacSHA1";

	// token settings
	private static final int	TOKEN_VERSION						= 2;												// increment whenever token parsing changes
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

	// username/password settings
	private static final int	USERNAME_MIN_LEN					= 1;
	private static final int	USERNAME_MAX_LEN					= 254; // probably shouldn't be more than this
	private static final String	ALLOWED_SYMBOLS						= "!@#$%^&*()-_+={}[]<>:;,~`?/|\\ .";
	private static final int	PASSWORD_DEFAULT_MIN_LEN			= 6;
	private static final int	PASSWORD_DEFAULT_MAX_LEN			= 32;
	private int					passwordMinLength;
	private int					passwordMaxLength;
	private boolean				passwordRequireSymbol;																	// defaults false
	private boolean				passwordRequireMixed;

	// reusable hash generator
	private Mac					mac;

	@Inject
	static AuthProviderService	theInstance;

	public static AuthProviderService getInstance() {
		return theInstance;
	}

	public static void setInstance(AuthProviderService instance) {
		theInstance = instance; // for testing only
	}

	/**************************** token methods 
	 * @param sessionFlags 
	 * @param sessionStart ****************************/

	@Override
	public String createToken(int id, Long timestamp, Long sessionStart, SessionFlags sessionFlags) {
		if (timestamp == null)
			timestamp = System.currentTimeMillis();
		if (sessionStart == null)
			sessionStart = timestamp;
		if (sessionFlags == null)
			sessionFlags = new SessionFlags();
		byte[] rawHmac = createHmacBytes(id, timestamp, sessionStart, sessionFlags);
		return encodeToken(rawHmac);
	}
	
	@Override
	public String createToken(int id) {
		return createToken(id,null,null,null);
	}

	@Override
	public AuthResponse checkToken(String value) {
		AuthResponse resp = null;
		ByteBuffer hmac = ByteBuffer.wrap(decodeToken(value));
		if (hmac.remaining() > (4 + 4 + 8 + 8 + 1)) {
			int version = hmac.getInt();
			if (version == TOKEN_VERSION) {
				int id = hmac.getInt();
				long timestamp = hmac.getLong();
				long sessionStart = hmac.getLong();
				SessionFlags sessionFlags = new SessionFlags(hmac.get());
				byte[] matchHmac = createHmacBytes(id, timestamp, sessionStart, sessionFlags);
				if (Arrays.equals(hmac.array(), matchHmac)) {
					resp = respondToValidToken(id, timestamp, sessionStart, sessionFlags);
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

	private byte[] createHmacBytes(int id, long timestamp, Long sessionStart, SessionFlags sessionFlags) {
		ByteBuffer hmac_data = ByteBuffer.allocate(4 + 4 + 8 + 8 + 1);
		hmac_data.putInt(TOKEN_VERSION);
		hmac_data.putInt(id);
		hmac_data.putLong(timestamp);
		hmac_data.putLong(sessionStart);
		hmac_data.put(sessionFlags.getPacked());

		byte[] hmac_signature;
		synchronized (mac) {
			hmac_signature = mac.doFinal(hmac_data.array());
		}
		ByteBuffer hmac = ByteBuffer.allocate(hmac_data.position() + hmac_signature.length);
		hmac.put(hmac_data.array());
		hmac.put(hmac_signature);
		return hmac.array();
	}

	private AuthResponse respondToValidToken(int id, long timestamp, long sessionStart, SessionFlags sessionFlags) {
		AuthResponse response;
		User user = TenantManagerService.getInstance().getUser(id);
		if (user != null) {
			if (user.isLoginAllowed()) {
				long now = System.currentTimeMillis();
				long ageSeconds = (now - timestamp) / 1000L;
				if (ageSeconds > (0 - this.sessionMaxFutureSeconds)) {
					// timestamp is not in the future
					if (ageSeconds < this.sessionMaxIdleMinutes * 60) {
						String refreshToken = null;
						if (sessionFlags.get(Flag.AUTO_REFRESH_SESSION)) {
							// session is still active
							if (ageSeconds > this.sessionMinIdleMinutes * 60) {
								// if token is valid but getting old, offer an updated one
								LOGGER.info("refreshing cookie for user {}", id);
								refreshToken = this.createToken(id, now, sessionStart, sessionFlags);
							}
						}
						response = new AuthResponse(Status.ACCEPTED, user, timestamp, sessionStart, sessionFlags, refreshToken);
					} else {
						LOGGER.warn("session timed out for user {} timestamp {} sessionStart {}", id, timestamp, sessionStart);
						response = new AuthResponse(Status.SESSION_IDLE_TIMEOUT, user, timestamp, sessionStart, sessionFlags, null);
					}
				} else {
					LOGGER.error("ALERT - future timestamp {} authenticated HMAC for user {} sessionStart {}",
						timestamp,
						id,
						sessionStart);
					response = new AuthResponse(Status.INVALID_TIMESTAMP, user, timestamp, sessionStart, sessionFlags, null);
				}
			} else {
				LOGGER.error("ALERT - login not allowed for user {}", id);
				response = new AuthResponse(Status.LOGIN_NOT_ALLOWED, user);
			}
		} else {
			LOGGER.error("ALERT - invalid user id {} with authenticated HMAC", id);
			response = new AuthResponse(Status.INVALID_USER_ID, null);
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

	@Override
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

	@Override
	public Cookie createAuthCookie(String token) {
		Cookie cookie = new Cookie(COOKIE_NAME, token);
		cookie.setPath("/");
		cookie.setDomain(this.cookieDomain);
		cookie.setVersion(0);
		cookie.setMaxAge(this.cookieMaxAgeHours * 60 * 60);
		cookie.setSecure(this.cookieSecure);
		return cookie;
	}

	/**************************** password hash methods ****************************/

	@Override
	public AuthResponse authenticate(String username, String password) {
		AuthResponse response = null;
		User user = TenantManagerService.getInstance().getUser(username);
		if (user != null) {
			boolean passwordValid = checkPassword(password, user.getHashedPassword());
			if (user.getTenant().isActive()) {
				if (user.isLoginAllowed()) {
					if (passwordValid) {
						LOGGER.debug("Password valid for user {}", user);
						long timestamp = System.currentTimeMillis();
						String token = this.createToken(user.getId(), timestamp, timestamp, null); // create default token for new session
						response = new AuthResponse(Status.ACCEPTED,
							user,
							timestamp, 
							timestamp, 
							null, // no flags specified
							token); // offer token
					} else {
						LOGGER.info("Invalid password for user {}", user);
						response = new AuthResponse(Status.BAD_CREDENTIALS, user);
					}
				} else {
					LOGGER.warn("user {} attempted login, login not allowed (password correct = {})", user, passwordValid);
					response = new AuthResponse(Status.LOGIN_NOT_ALLOWED, user);
				}
			} else {
				LOGGER.warn("User of {} inactive tenant {} attempted login, password correct = {}", user, user.getTenant()
					.getName(), passwordValid);
				response = new AuthResponse(Status.LOGIN_NOT_ALLOWED, user);
			}
		} else {
			LOGGER.info("User not found {}", user);
			response = new AuthResponse(Status.BAD_CREDENTIALS, null);
		}
		
		return response; // never null 
	}

	@Override
	public String hashPassword(final String password) {
		return Md5Crypt.apr1Crypt(password);
	}

	private boolean checkPassword(final String password, final String hash) {
		return Md5Crypt.apr1Crypt(password, hash).equals(hash);
	}

	@Override
	public boolean hashIsValid(String hash) {
		return hash.startsWith("$apr1$");
	}

	@Override
	public String describePasswordRequirements() {
		return String.format("Password must be between %d and %d characters long, consisting of letters, numbers and punctuation.%s%s",
			this.passwordMinLength,
			this.passwordMaxLength,
			this.passwordRequireMixed ? " It must contain a mix of upper case, lower case and/or numbers." : "",
			this.passwordRequireSymbol ? " It must contain at least one of these symbols: " + ALLOWED_SYMBOLS : "");
	}

	@Override
	public boolean passwordMeetsRequirements(String password) {
		if (password == null)
			return false;
		if (password.isEmpty())
			return false;
		if (password.length() < this.passwordMinLength || password.length() > this.passwordMaxLength)
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
			boolean symbol = ALLOWED_SYMBOLS.indexOf(ch) >= 0;
			if (!lowercase && !uppercase && !number && !symbol)
				return false;
			hasLowercase |= lowercase;
			hasUppercase |= uppercase;
			hasSymbol |= symbol;
			hasNumber |= number;
		}
		if (this.passwordRequireMixed) {
			int mix = (hasUppercase ? 1 : 0) + (hasLowercase ? 1 : 0) + (hasNumber ? 1 : 0);
			if (mix < 2)
				return false;
		}
		if (this.passwordRequireSymbol && !hasSymbol)
			return false;

		return true;
	}

	@Override
	public boolean usernameMeetsRequirements(String username) {
		if (username == null)
			return false;
		if (username.isEmpty())
			return false;
		if (username.length() < USERNAME_MIN_LEN || username.length() > USERNAME_MAX_LEN)
			return false;
		for (int i = 0; i < username.length(); i++) {
			char ch = username.charAt(i);
			if (!CharSet.ASCII_ALPHA.contains(ch) 
					&& !CharSet.ASCII_NUMERIC.contains(ch) 
					&& ALLOWED_SYMBOLS.indexOf(ch) < 0)
				return false;
		}
		return true;
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
		mac = null;
		tokenXor = null;
	}

}
