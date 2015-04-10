package com.codeshelf.security;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.manager.User;
import com.codeshelf.security.SessionFlags.Flag;
import com.codeshelf.security.TokenSession.Status;
import com.codeshelf.service.AbstractCodeshelfIdleService;

import edu.emory.mathcs.backport.java.util.Arrays;

public abstract class AbstractHmacTokenService extends AbstractCodeshelfIdleService {
	static final Logger	LOGGER								= LoggerFactory.getLogger(AbstractHmacTokenService.class);


	abstract int getSessionMaxFutureSeconds();
	abstract int getSessionMaxIdleMinutes();
	abstract int getSessionMinIdleMinutes();
	abstract byte[] getTokenXor();

	private static final String	HMAC_ALGORITHM	= "HmacSHA1";
	private static final int	TOKEN_VERSION	= 3;
	private Mac	mac;

	public AbstractHmacTokenService() {
		super();
	}

	/**************************** token methods ****************************/
	
	// subclass must call initializeHmac before using tokens
	protected void initializeHmac(String secret) {
		SecretKeySpec hmacKey = new SecretKeySpec(secret.getBytes(), HMAC_ALGORITHM);
		try {
			mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(hmacKey);
		} catch (InvalidKeyException | NoSuchAlgorithmException e) {
			throw new RuntimeException("Unexpected exception creating HMAC", e);
		}
	}

	// subclass should implement method(s) to authenticate and get a token by calling this method
	protected String createToken(int userId, int tenantId, Long timestamp, Long sessionStart, SessionFlags sessionFlags) {
		if (timestamp == null)
			timestamp = System.currentTimeMillis();
		if (sessionStart == null)
			sessionStart = timestamp;
		if (sessionFlags == null)
			sessionFlags = new SessionFlags();
		byte[] rawHmac = createHmacBytes(userId, tenantId, timestamp, sessionStart, sessionFlags);
		return encodeToken(rawHmac);
	}

	public TokenSession checkToken(String value) {
		TokenSession resp = null;
		ByteBuffer hmac = ByteBuffer.wrap(decodeToken(value));
		if (hmac.remaining() == (4 + 4 + 4 + 8 + 8 + 1 + 20)) {
			int version = hmac.getInt();
			if (version == TOKEN_VERSION) {
				int userId = hmac.getInt();
				int tenantId = hmac.getInt();
				long timestamp = hmac.getLong();
				long sessionStart = hmac.getLong();
				SessionFlags sessionFlags = new SessionFlags(hmac.get());
				byte[] matchHmac = createHmacBytes(userId, tenantId, timestamp, sessionStart, sessionFlags);
				if (Arrays.equals(hmac.array(), matchHmac)) {
					resp = respondToValidToken(userId, tenantId, timestamp, sessionStart, sessionFlags);
				} else {
					LOGGER.warn("Invalid HMAC for user ID {} timestamp {}", userId, timestamp);
					resp = new TokenSession(Status.INVALID_TOKEN);
				}
			} else {
				LOGGER.warn("Failed to parse auth token, bad version {}", version);
				resp = new TokenSession(Status.INVALID_TOKEN);
			}
		} else {
			LOGGER.warn("auth token was wrong size, {} bytes", hmac.remaining());
			resp = new TokenSession(Status.INVALID_TOKEN);
		}
		return resp;
	}

	private byte[] createHmacBytes(int userId, int tenantId, long timestamp, Long sessionStart, SessionFlags sessionFlags) {
		ByteBuffer hmac_data = ByteBuffer.allocate(4 + 4 + 4 + 8 + 8 + 1);
		hmac_data.putInt(TOKEN_VERSION);
		hmac_data.putInt(userId);
		hmac_data.putInt(tenantId);
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

	private TokenSession respondToValidToken(int userId, int tenantId, long timestamp, long sessionStart, SessionFlags sessionFlags) {
		TokenSession response;
		User user = TenantManagerService.getInstance().getUser(userId);
		Tenant tenant = TenantManagerService.getInstance().getTenant(tenantId);
		if (user != null) {
			if (user.isLoginAllowed()) {
				long now = System.currentTimeMillis();
				long ageSeconds = (now - timestamp) / 1000L;
				if (ageSeconds > (0 - this.getSessionMaxFutureSeconds())) {
					// timestamp is not in the future
					if (ageSeconds < this.getSessionMaxIdleMinutes() * 60) {
						String refreshToken = null;
						if (sessionFlags.get(Flag.AUTO_REFRESH_SESSION)) {
							// session is still active
							if (ageSeconds > this.getSessionMinIdleMinutes() * 60) {
								// if token is valid but getting old, offer an updated one
								LOGGER.info("renewing token for {}", user.getUsername());
								refreshToken = this.createToken(userId, tenantId, now, sessionStart, sessionFlags);
							}
						}
						response = new TokenSession(Status.ACCEPTED, user, tenant, timestamp, sessionStart, sessionFlags, refreshToken);
					} else {
						LOGGER.info("expired token for user {} (timestamp {}, sessionStart {})", userId, timestamp, sessionStart);
						response = new TokenSession(Status.SESSION_IDLE_TIMEOUT, user, tenant, timestamp, sessionStart, sessionFlags, null);
					}
				} else {
					LOGGER.error("ALERT - future timestamp {} authenticated HMAC for user {} sessionStart {}",
						timestamp,
						userId,
						sessionStart);
					response = new TokenSession(Status.INVALID_TIMESTAMP, user, tenant, timestamp, sessionStart, sessionFlags, null);
				}
			} else {
				LOGGER.error("ALERT - login not allowed for user {}", user.getUsername());
				response = new TokenSession(Status.LOGIN_NOT_ALLOWED, user);
			}
		} else {
			LOGGER.error("ALERT - invalid user id {} with authenticated HMAC", userId);
			response = new TokenSession(Status.INVALID_USER_ID, null);
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
		byte[] tokenXor = getTokenXor();
		int xi = 0;
		for (int i = 0; i < buf.length; i++) {
			buf[i] ^= tokenXor[xi];
			xi = ((xi + 1) % tokenXor.length);	
		}
	}

}