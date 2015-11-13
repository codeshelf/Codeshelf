package com.codeshelf.security;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.Cookie;

import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.security.TokenSession.Status;
import com.codeshelf.testframework.MockDaoTest;
import com.codeshelf.util.ThreadUtils;

public class TokenSessionServiceTest extends MockDaoTest {
	static final Logger	LOGGER	= LoggerFactory.getLogger(TokenSessionServiceTest.class);

	@Test
	public void cryptoCookieTest() {
		TokenSessionService auth = new TokenSessionService().initialize();
		//		User user = TenantManagerService.getInstance().getUser(0); // we are in mock dao, this should work

		// can create auth cookie
		String token = auth.testCreateToken(0, 0);
		Cookie authCookie = auth.createAuthCookie(token);
		Assert.assertNotNull(authCookie);

		Cookie otherCookie = create("othercookie", "asdfasdfasdf", "/", "test.com", 1, "comment", 86400, false);
		Cookie[] cookies = new Cookie[2];
		// String name, String value, String path, String domain, int version, String comment, int maxAge, boolean secure
		cookies[0] = otherCookie;
		cookies[1] = authCookie;

		// can find and validate auth cookie
		TokenSession resp = auth.checkAuthCookie(cookies);

		assertActive(resp, 0);

		// fail to validate if more than one
		String token2 = auth.testCreateToken(0, 0);
		cookies[0] = auth.createAuthCookie(token2);
		resp = auth.checkAuthCookie(cookies);
		Assert.assertNull(resp);
		cookies[0] = otherCookie;
		resp = auth.checkAuthCookie(cookies);
		Assert.assertNotNull(resp);

		// fail to validate if signature altered
		String value = authCookie.getValue();
		Assert.assertTrue(Base64.isBase64(value));
		byte[] decoded = Base64.decodeBase64(value);
		decoded[decoded.length - 2]++;
		String reEncoded = new String(Base64.encodeBase64(decoded));
		authCookie.setValue(reEncoded);
		resp = auth.checkAuthCookie(cookies);
		Assert.assertEquals(Status.INVALID_TOKEN, resp.getStatus());
	}

	@Test
	public void multipleThreadCreateCheck() throws InterruptedException, ExecutionException {
		final TokenSessionService auth = new TokenSessionService().initialize();

		ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		CompletionService<TokenSession> completionService = new ExecutorCompletionService<TokenSession>(executorService);
		int TOTAL = 1000;
		final Set<String> tokenSet = new HashSet<String>();
		
		// We think the millisecond time stamp is a component of the token creation. If same session, tenant, ms, make get duplicate token.
		// So, contrived here to separate by two ms.
		long previousTimeStamp = System.currentTimeMillis();

		for (int i = 0; i < TOTAL; i++) {
			final int userId = i;
			previousTimeStamp = System.currentTimeMillis();
			completionService.submit(new Callable<TokenSession>() {

				@Override
				public TokenSession call() throws Exception {
					String token2 = auth.testCreateToken(0, userId);
					tokenSet.add(token2);
					Cookie cookie = auth.createAuthCookie(token2);
					TokenSession resp = auth.checkAuthCookie(cookie);
					return resp;
				}

			});
			ThreadUtils.sleep(2);
			if (System.currentTimeMillis() == previousTimeStamp){
				LOGGER.error("might get a failure"); // Never see this, but still get a failure sometimes. only 999 of 1000 unique tokens
			}
		}
		for (int i = 0; i < TOTAL; i++) {
			TokenSession resp = completionService.take().get();
			assertActive(resp, 0);
		}
		Assert.assertEquals("Tokens were not unique", TOTAL, tokenSet.size());
	}

	/**
	 * The purpose of this is to see how MacOS, windows, linux does with milliseconds. That is a key parameter for the Mac token generation.
	 * Windows 7 used to give the same millisecond for about 7 ms. This test seems to show that sometimes MacOS can give same for 2ms
	 * That give a 2 ms window for two token creates from the same source to get identical token.
	 */
	@Test
	public void milliSecondBehavior() {
		long startTimeStamp = System.currentTimeMillis();
		long endTime = startTimeStamp + 100;
		long thisTimeStamp = startTimeStamp;
		LOGGER.info("millis:{}", thisTimeStamp);
		long prevTimeStamp = thisTimeStamp;
		int msCount = 0;
		while (thisTimeStamp <= endTime) {
			thisTimeStamp = System.currentTimeMillis();
			if (prevTimeStamp != thisTimeStamp) {
				msCount++;
				if (thisTimeStamp != prevTimeStamp + 1) {
					LOGGER.error(String.format("Slow (skipped a ms or clock not so good) at %d ms", msCount));
					// Assert.fail(String.format("Slow (skipped a ms or clock not so good) at %d ms", msCount));
				}
				prevTimeStamp = thisTimeStamp;
				LOGGER.info("millis:{}", thisTimeStamp);				
			}
		}
	}

	@Test
	public void passwordHashTest() {
		TokenSessionService auth = new TokenSessionService().initialize();

		String password = "goodpassword";
		Assert.assertTrue(auth.passwordMeetsRequirements(password));
		Assert.assertFalse(auth.passwordMeetsRequirements(""));

		String hash = auth.hashPassword(password);
		Assert.assertTrue(auth.hashIsValid(hash));
		Assert.assertFalse(auth.hashIsValid("$myhash$asdf"));

		// not applicable, now we just check the user directly from the auth service
		//Assert.assertTrue(auth.checkPassword(password, hash));
		//Assert.assertFalse(auth.checkPassword(password+"!", hash));
	}

	private void assertActive(TokenSession resp, int userId) {
		Assert.assertNotNull(resp);
		Assert.assertEquals(Status.ACTIVE_SESSION, resp.getStatus());
		Assert.assertEquals(userId, resp.getUser().getId().intValue());

		// has valid timestamp
		Assert.assertTrue(resp.getTokenTimestamp() <= System.currentTimeMillis());
		// was 1000 ms, but now we are intentionally creating them slower.
		Assert.assertTrue(resp.getTokenTimestamp() > System.currentTimeMillis() - 10000);

	}

	private Cookie create(String name,
		String value,
		String path,
		String domain,
		int version,
		String comment,
		int maxAge,
		boolean secure) {
		Cookie cookie = new Cookie(name, value);
		cookie.setPath(path);
		cookie.setDomain(domain);
		cookie.setVersion(version);
		cookie.setComment(comment);
		cookie.setMaxAge(maxAge);
		cookie.setSecure(secure);
		return cookie;
	}

}
