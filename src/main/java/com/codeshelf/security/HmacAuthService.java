package com.codeshelf.security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.ws.rs.core.NewCookie;

import lombok.Getter;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.commons.lang.mutable.MutableLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.service.AbstractCodeshelfIdleService;
import com.google.inject.Inject;

import edu.emory.mathcs.backport.java.util.Arrays;

public class HmacAuthService extends AbstractCodeshelfIdleService implements AuthProviderService {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(HmacAuthService.class);

	private static final String	COOKIE_NAME							= "CSAUTHTOKEN";
	private static final int	COOKIE_VERSION_NUMBER				= 1;
	private static final char 	HMAC_TOKEN_SEPARATOR				= ':';
	private static final String HMAC_ALGORITHM						= "HmacSHA1";

	@Getter
	public int		defaultCookieExpirationSeconds	= 60 * 60 * 24 * 7;	// keep login cookie for a week
	@Getter
	public int		minCookieExpirationSeconds		= 60;
	@Getter
	public int		maxCookieExpirationSeconds		= 60 * 60 * 24 * 365;

	private String domain;
	private boolean	secureCookies;
	private Mac mac;

	@Inject
	static AuthProviderService theInstance;
	
	public static AuthProviderService getInstance() {
		return theInstance;
	}
	
	public HmacAuthService() { // for testing
	}

	private byte[] createHmac(int id,long timestamp) {
		byte[] value = (Long.toString(id) + HMAC_TOKEN_SEPARATOR + Long.toString(timestamp) + HMAC_TOKEN_SEPARATOR).getBytes();
		String result = "";

		byte[] hmacBytes; 
		synchronized(mac) {
			hmacBytes = mac.doFinal(value);			
		}
		return concat(value, hmacBytes);
	}

	private static byte[] concat(byte[] a1, byte[] a2) {
		byte[] result = Arrays.copyOf(a1, a1.length + a2.length);
		for (int i = 0; i < a2.length; i++) {
			result[i + a1.length] = a2[i];
		}
		return result;
	}

	public AuthCookieContents checkAuthCookie(Cookie[] cookies) {
		Cookie match = null;
		for (Cookie cookie : cookies) {
			if(cookie.getName().equals(COOKIE_NAME)) {
				if(match == null) {
					match = cookie;
				} else {
					LOGGER.warn("more than one auth cookie found");
					return null;
				}
			}
		}
		if(match != null) 
			return checkAuthCookie(match);
		//else
		return null;
	}

	public AuthCookieContents checkAuthCookie(Cookie cookie) {
		AuthCookieContents authCookieContents = null;
		if (cookie.getName().equals(COOKIE_NAME)) {
			if(cookie.getVersion() == COOKIE_VERSION_NUMBER) {
				byte[] raw = Base64.decodeBase64(cookie.getValue());
				MutableLong id = new MutableLong(0L);
				int offset = scanLong(id,raw,0);
				if(offset>0) {
					if(id.longValue() < Integer.MAX_VALUE && id.longValue() >=0) {
						MutableLong timestamp = new MutableLong(0L);
						offset = scanLong(timestamp,raw,offset);
						if(offset>0) {
							//byte[] requestHmac = Arrays.copyOfRange(raw,offset,raw.length);
							byte[] matchHmac = createHmac(id.intValue(),timestamp.longValue());
							if(Arrays.equals(raw, matchHmac)) {
								authCookieContents = new AuthCookieContents(id.intValue(),timestamp.longValue());
							} else {
								LOGGER.warn("Invalid HMAC for user ID {} timestamp {}",id,timestamp);
							}
						} else {
							LOGGER.warn("Failed to parse auth token (2) for user ID {}",id);
						}
					} else {
						LOGGER.warn("invalid user ID {}",id);
					}
				} else {
					LOGGER.warn("Failed to parse auth token");
				}
			} else {
				LOGGER.warn("Wrong version auth token (got {} expected {})",cookie.getVersion(),COOKIE_VERSION_NUMBER);
			}
		}
		return authCookieContents;
	}

	private int scanLong(MutableLong id,byte[] raw,int offset) {
		// TODO: wrap in ByteBuffer instead of using MutableLong and pass/return offset - forgot this was Java and wrote C
		
		if(offset+2 > raw.length) 
			return -1;
		while(offset<raw.length && raw[offset]>='0' && raw[offset]<='9') {
			if(id.longValue() >= (Long.MAX_VALUE / 10)) {
				return -1;
			}
			id.setValue(id.longValue() * 10 + (raw[offset]-'0'));
			offset++;
		}
		if(offset<raw.length && raw[offset] == HMAC_TOKEN_SEPARATOR) {
			return offset+1;			
		}
		//else
		return -1;
	}

	public NewCookie createAuthCookie(int id, int maxAgeSeconds) {
		if ((maxAgeSeconds < minCookieExpirationSeconds) || (maxAgeSeconds > maxCookieExpirationSeconds))
			maxAgeSeconds = defaultCookieExpirationSeconds;

		long timestamp = System.currentTimeMillis();
		String hmacString = new String(Base64.encodeBase64(createHmac(id, timestamp)));
		NewCookie cookie = new NewCookie(COOKIE_NAME,
			hmacString,
			"/",
			domain,
			COOKIE_VERSION_NUMBER,
			"",
			maxAgeSeconds,
			secureCookies);
		return cookie;
	}

	public String hashPassword(final String password) {
		return Md5Crypt.apr1Crypt(password);
	}
	
	public boolean checkPassword(final String password,final String hash) {
		return Md5Crypt.apr1Crypt(password,hash).equals(hash);
	}
	
	public boolean passwordMeetsRequirements(String password) {
		if(password == null) 
			return false;
		if(password.isEmpty())
			return false;
		
		return true;
	}
	
	public boolean hashIsValid(String hash) {
		return hash.startsWith("$apr1$");
	}

	@Override
	protected void startUp() throws Exception {
		initialize();
	}

	public AuthProviderService initialize() {
		this.domain = System.getProperty("auth.cookie.domain");
		this.secureCookies = Boolean.getBoolean("auth.cookie.secure");
		String secret = System.getProperty("auth.hmac.secret");
		if(this.domain == null || secret == null) {
			LOGGER.error("could not initialize authentication svc, configuration missing");
			throw new RuntimeException("configuration missing");
		}
		// initialize Message Authentication Code
		SecretKeySpec hmacKey = new SecretKeySpec(secret.getBytes(), HMAC_ALGORITHM);
		try {
			mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(hmacKey);
		} catch (InvalidKeyException | NoSuchAlgorithmException e) {
			throw new RuntimeException("Unexpected exception creating HMAC", e);
		}
		return this;
	}

	@Override
	protected void shutDown() throws Exception {
		mac = null;
	}

}
