package com.codeshelf.security;

import java.util.Locale;
import java.util.Map;

import lombok.Getter;

import org.apache.commons.codec.digest.Md5Crypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.SecurityAnswer;
import com.codeshelf.manager.SecurityQuestion;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.User;
import com.codeshelf.security.SessionFlags.Flag;
import com.codeshelf.security.TokenSession.Status;
import com.codeshelf.util.StringUIConverter;
import com.google.inject.Inject;

public class TokenSessionService extends AbstractCookieSessionService {
	static final Logger			LOGGER								= LoggerFactory.getLogger(TokenSessionService.class);
	private static final String	TOKEN_DEFAULT_XOR					= "00";
	@Getter
	byte[]						tokenXor;

	// session settings
	private static final int	SESSION_DEFAULT_MAX_FUTURE_SECONDS	= 30;													// do not allow timestamps significantly in the future
	private static final int	SESSION_DEFAULT_MAX_IDLE_MINUTES	= 6;
	private static final int	SESSION_DEFAULT_MIN_IDLE_MINUTES	= 5;
	private static final int	SETUP_DEFAULT_IDLE_MINUTES			= 60 * 24;
	private static final int	RECOVERY_DEFAULT_IDLE_MINUTES		= 60 * 2;
	private static final int	RECOVERY_SETPW_DEFAULT_IDLE_MINUTES	= 10;
	@Getter
	int							sessionMaxFutureSeconds;
	int							sessionMaxIdleMinutes;
	int							sessionMinIdleMinutes;
	int							sessionSetupIdleMinutes;
	int							sessionRecoveryIdleMinutes;
	int							sessionRecoverySetPwIdleMinutes;

	// cookie settings
	static final public String	COOKIE_NAME							= "CSTOK";
	private static final int	COOKIE_DEFAULT_MAX_AGE_HOURS		= 24;
	private static final String	COOKIE_DEFAULT_DOMAIN				= "";
	@Getter
	String						cookieDomain;
	@Getter
	boolean						cookieSecure;
	@Getter
	int							cookieMaxAgeHours;

	// username/password settings
	static final int			USERNAME_MIN_LEN					= 1;
	static final int			USERNAME_MAX_LEN					= 254;													// probably shouldn't be more than this
	private static final int	PASSWORD_DEFAULT_MIN_LEN			= 6;
	private static final int	PASSWORD_DEFAULT_MAX_LEN			= 32;
	@Getter
	int							passwordMinLength;
	@Getter
	int							passwordMaxLength;
	@Getter
	boolean						passwordRequireSymbol;																		// defaults false
	@Getter
	boolean						passwordRequireMixed;

	// security question/answer settings
	private static final int	SECURITY_ANSWER_DEFAULT_MIN_LEN		= 3;
	private static final int	SECURITY_ANSWER_DEFAULT_MAX_LEN		= 255;
	private static final int	SECURITY_ANSWER_DEFAULT_MIN_COUNT	= 3;
	@Getter
	int							securityAnswerMinLength;
	@Getter
	int							securityAnswerMaxLength;
	@Getter
	int							securityAnswerMinCount;

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
		this.sessionSetupIdleMinutes = Integer.getInteger("auth.session.setup.idleminutes", SETUP_DEFAULT_IDLE_MINUTES);
		this.sessionRecoveryIdleMinutes = Integer.getInteger("auth.session.recovery.idleminutes", RECOVERY_DEFAULT_IDLE_MINUTES);
		this.sessionRecoverySetPwIdleMinutes = Integer.getInteger("auth.session.recoverypw.idleminutes", RECOVERY_SETPW_DEFAULT_IDLE_MINUTES);
		
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

		// security answer settings
		this.securityAnswerMinLength = Integer.getInteger("auth.securityanswer.length.min", SECURITY_ANSWER_DEFAULT_MIN_LEN);
		this.securityAnswerMaxLength = Integer.getInteger("auth.securityanswer.length.max", SECURITY_ANSWER_DEFAULT_MAX_LEN);
		this.securityAnswerMinCount = Integer.getInteger("auth.securityanswer.count.min", SECURITY_ANSWER_DEFAULT_MIN_COUNT);

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
		return createToken(userId, tenantId, null, null, null);
	}

	/* specify timeout behavior for various types of sessions */
	@Override
	int getSessionMaxIdleMinutes(SessionFlags sessionFlags) {
		if(sessionFlags.get(Flag.ACCOUNT_SETUP)) {
			if(sessionFlags.get(Flag.ACCOUNT_RECOVERY)) {
				// re-setting account after successfully answering security questions
				return this.sessionRecoverySetPwIdleMinutes;
			} else {
				// setting up a new account
				return this.sessionSetupIdleMinutes;
			}
		} else if(sessionFlags.get(Flag.ACCOUNT_RECOVERY)) {
			// responding to an account recovery email
			return this.sessionRecoveryIdleMinutes;
		}
		// normal session
		return this.sessionMaxIdleMinutes;
	}

	@Override
	int getSessionMinIdleMinutes(SessionFlags sessionFlags) {
		return this.sessionMinIdleMinutes;
	}

	/***************** Security Question methods ******************/

	private String stripSecurityAnswer(String value) {
		// strip all non-alphanumeric characters 
		// combine all double spaces 
		// convert to upper case 
		// remove leading/trailing whitespace 
		return value.replaceAll("[^A-Za-z0-9 ]", "").replaceAll("\\s+", " ").toUpperCase(Locale.US).trim();
	}

	public String hashSecurityAnswer(String answer) {
		answer = stripSecurityAnswer(answer);
		if (securityAnswerMeetsRequirements(answer)) {
			return Md5Crypt.apr1Crypt(answer);
		}
		return null;
	}

	public TokenSession checkSecurityAnswers(TokenSession tokenSession, Map<SecurityQuestion, String> securityAnswers) {
		if (securityAnswers.size() < this.getSecurityAnswerMinCount()) {
			LOGGER.error("Invalid securityAnswers map passed to session service (caller should validate)");
			return null;
		}
		if (tokenSession != null && tokenSession.getStatus().equals(TokenSession.Status.SPECIAL_SESSION)
				&& tokenSession.getSessionFlags().get(Flag.ACCOUNT_RECOVERY)) {
			
			User user = tokenSession.getUser();
			Tenant tenant = user.getTenant();
			Map<SecurityQuestion, SecurityAnswer> userAnswers = user.getSecurityAnswers();
			for (SecurityQuestion question : securityAnswers.keySet()) {
				if (!userAnswers.containsKey(question)) {
					LOGGER.warn("Tried to answer security question {} that doesn't belong to user {}",
						question.getCode(),
						user.getUsername());
					return null;
				}
				SecurityAnswer userAnswer = userAnswers.get(question);
				if (!checkSecurityAnswer(securityAnswers.get(question), userAnswer.getHashedAnswer())) {
					LOGGER.warn("Incorrect answer given for question {} user {}", question.getCode(), user.getUsername());
					return null;
				}
				// else continue
			}
			// made it to end, all questions answered successfully

			// new token is special SETUP+RECOVERY token (short expiration, usage is to reset password)
			SessionFlags sessionFlags = new SessionFlags();
			sessionFlags.set(Flag.ACCOUNT_SETUP, true);
			sessionFlags.set(Flag.ACCOUNT_RECOVERY, true);
			String refreshToken = this.createToken(user.getId(), tenant.getId(), null, null, sessionFlags);

			TokenSession newSession = new TokenSession(Status.SPECIAL_SESSION, user, tenant, null, null, sessionFlags, refreshToken);
			return newSession;
		} // else invalid token

		return null;
	}

	private boolean checkSecurityAnswer(String value, String hashedAnswer) {
		value = stripSecurityAnswer(value);
		return Md5Crypt.apr1Crypt(value, hashedAnswer).equals(hashedAnswer);
	}

	public boolean securityAnswerMeetsRequirements(String newAnswerString) {
		String cAnswer = stripSecurityAnswer(newAnswerString);
		return cAnswer != null && cAnswer.length() >= securityAnswerMinLength && cAnswer.length() <= securityAnswerMaxLength;
	}

	public String describeSecurityAnswerRequirements() {
		return String.format("Security answer must be between %d and %d characters long, "
				+ "NOT including punctuation or extra spaces. Capitalization is ignored.",
			this.getSecurityAnswerMinLength(),
			this.getSecurityAnswerMaxLength());
	}

	public TokenSession createAccountSetupToken(User newUser) {
		SessionFlags sessionFlags = new SessionFlags();
		sessionFlags.set(Flag.ACCOUNT_SETUP, true);
		long timestamp = System.currentTimeMillis();
		String token = this.createToken(newUser.getId(), newUser.getTenantId(), timestamp, timestamp, sessionFlags);
		TokenSession session = new TokenSession(Status.SPECIAL_SESSION, newUser, newUser.getTenant(), timestamp, timestamp, sessionFlags, token);
		return session;
	}

	public TokenSession createAccountRecoveryToken(User user) {
		SessionFlags sessionFlags = new SessionFlags();
		sessionFlags.set(Flag.ACCOUNT_RECOVERY, true);
		long timestamp = System.currentTimeMillis();
		String token = this.createToken(user.getId(), user.getTenantId(), timestamp, timestamp, sessionFlags);
		TokenSession session = new TokenSession(Status.SPECIAL_SESSION, user, user.getTenant(), timestamp, timestamp, sessionFlags, token);
		return session;
	}

}
