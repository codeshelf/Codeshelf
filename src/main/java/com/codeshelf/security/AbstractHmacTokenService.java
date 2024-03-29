package com.codeshelf.security;	

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.User;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.security.SessionFlags.Flag;
import com.codeshelf.security.TokenSession.Status;
import com.codeshelf.service.AbstractCodeshelfIdleService;

public abstract class AbstractHmacTokenService extends AbstractCodeshelfIdleService {
	static final Logger	LOGGER								= LoggerFactory.getLogger(AbstractHmacTokenService.class);


	abstract int getSessionMaxFutureSeconds();
	abstract int getSessionMaxIdleMinutes(SessionFlags sessionFlags);
	abstract int getSessionMinIdleMinutes(SessionFlags sessionFlags);
	abstract byte[] getTokenXor();

	private static final String	HMAC_ALGORITHM	= "HmacSHA1";
	private static final int	TOKEN_VERSION	= 3;
	private static final ThreadLocal<Mac>	mac = new ThreadLocal<Mac>() {
		@Override
		protected Mac initialValue() {
			String secret = System.getProperty("auth.token.secret");
			SecretKeySpec hmacKey = new SecretKeySpec(secret.getBytes(), HMAC_ALGORITHM);
			try {
				Mac mac = Mac.getInstance(HMAC_ALGORITHM);
				mac.init(hmacKey);
				return mac;
			} catch (InvalidKeyException | NoSuchAlgorithmException e) {
				throw new RuntimeException("Unexpected exception creating HMAC", e);
			}
		}
		
	};

	public AbstractHmacTokenService() {
		super();
	}

	/**************************** token methods ****************************/
	
	// subclass must call initializeHmac before using tokens
	protected void initializeHmac(String secret) {
	}

	// subclass should implement method(s) to authenticate and get a token by calling this method
	protected String createToken(int userId, int tenantId, Long timestamp, Long sessionStart, SessionFlags sessionFlags) {
		if (timestamp == null)
			timestamp = System.currentTimeMillis();
		if (sessionStart == null)
			sessionStart = timestamp;
		if (sessionFlags == null)
			sessionFlags = new SessionFlags();
		int random = ThreadLocalRandom.current().nextInt();
		byte[] rawHmac = createHmacBytes(userId, tenantId, timestamp, sessionStart, random, sessionFlags);
		String lastToken = encodeToken(rawHmac);
		return lastToken;
	}

	public TokenSession checkToken(String value) {
		TokenSession resp = null;
		if(value != null) {
			ByteBuffer hmac = ByteBuffer.wrap(decodeToken(value));
			if (hmac.remaining() == (4 + 4 + 4 + 8 + 8 + 4 + 1 + 20)) {
				int version = hmac.getInt();
				if (version == TOKEN_VERSION) {
					int userId = hmac.getInt();
					int tenantId = hmac.getInt();
					long timestamp = hmac.getLong();
					long sessionStart = hmac.getLong();
					int random = hmac.getInt();
					SessionFlags sessionFlags = new SessionFlags(hmac.get());
					byte[] matchHmac = createHmacBytes(userId, tenantId, timestamp, sessionStart, random, sessionFlags);
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
		} else {
			LOGGER.debug("no auth token provided");
			resp = null;
		}
		return resp;
	}

	private byte[] createHmacBytes(int userId, int tenantId, long timestamp, Long sessionStart, int random, SessionFlags sessionFlags) {
		ByteBuffer hmac_data = ByteBuffer.allocate(4 + 4 + 4 + 8 + 8 + 4 + 1);
		hmac_data.putInt(TOKEN_VERSION);
		hmac_data.putInt(userId);
		hmac_data.putInt(tenantId);
		hmac_data.putLong(timestamp);
		hmac_data.putLong(sessionStart);
		hmac_data.putInt(random);
		hmac_data.put(sessionFlags.getPacked());
	
		byte[] hmac_signature;
		hmac_signature = mac.get().doFinal(hmac_data.array());

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
					if (ageSeconds < this.getSessionMaxIdleMinutes(sessionFlags) * 60) {
						// we are going to accept this token, but flags may indicate special status

						String refreshToken = null;
						Status tokenStatus;
						
						if (sessionFlags.get(Flag.ACCOUNT_SETUP) || sessionFlags.get(Flag.ACCOUNT_RECOVERY)) {
							tokenStatus = Status.SPECIAL_SESSION;
						} else {
							tokenStatus = Status.ACTIVE_SESSION;
							// auto refresh is only applicable for regular sessions
							if (sessionFlags.get(Flag.AUTO_REFRESH_SESSION)) {
								if (ageSeconds > this.getSessionMinIdleMinutes(sessionFlags) * 60) {
									// if token is valid but getting old, offer an updated one
									LOGGER.info("renewing token for {}", user.getUsername());
									refreshToken = this.createToken(userId, tenantId, now, sessionStart, sessionFlags);
								}
							}
						}
						response = new TokenSession(tokenStatus, user, tenant, timestamp, sessionStart, sessionFlags, refreshToken);
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