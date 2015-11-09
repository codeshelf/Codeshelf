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

import com.codeshelf.security.TokenSession.Status;
import com.codeshelf.testframework.MockDaoTest;

public class TokenSessionServiceTest extends MockDaoTest {
	@Test
	public void cryptoCookieTest() {
		TokenSessionService auth = new TokenSessionService().initialize();
//		User user = TenantManagerService.getInstance().getUser(0); // we are in mock dao, this should work
		
		// can create auth cookie
		String token = auth.testCreateToken(0,0);
		Cookie authCookie = auth.createAuthCookie(token);
		Assert.assertNotNull(authCookie);
		
		Cookie otherCookie = create("othercookie","asdfasdfasdf","/","test.com",1,"comment",86400,false);
		Cookie[] cookies = new Cookie[2];
		// String name, String value, String path, String domain, int version, String comment, int maxAge, boolean secure
		cookies[0] = otherCookie; 
		cookies[1] = authCookie;
		
		// can find and validate auth cookie
		TokenSession resp = auth.checkAuthCookie(cookies);

		assertActive(resp, 0);

		// fail to validate if more than one
		String token2 = auth.testCreateToken(0,0);
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
		decoded[decoded.length-2]++;
		String reEncoded = new String(Base64.encodeBase64(decoded));
		authCookie.setValue(reEncoded);
		resp = auth.checkAuthCookie(cookies);
		Assert.assertEquals(Status.INVALID_TOKEN,resp.getStatus());		
	}
	
	@Test
	public void multipleThreadCreateCheck() throws InterruptedException, ExecutionException {
		final TokenSessionService auth = new TokenSessionService().initialize();
		
		ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		CompletionService<TokenSession> completionService = 
				new ExecutorCompletionService<TokenSession>(executorService);
		int TOTAL = 1000;
		final Set<String> tokenSet = new HashSet<String>();
		for(int i = 0; i < TOTAL; i++) {
			final int userId = i;
			completionService.submit(new Callable<TokenSession>() {

				@Override
				public TokenSession call() throws Exception {
					String token2 = auth.testCreateToken(0,userId);
					tokenSet.add(token2);
					Cookie cookie = auth.createAuthCookie(token2);
					TokenSession resp = auth.checkAuthCookie(cookie);
					return resp;
				}
				
			});
		}
		for(int i=0; i < TOTAL; i++) {
			TokenSession resp = completionService.take().get();
			assertActive(resp, 0);
		}
		Assert.assertEquals("Tokens were not unique", TOTAL, tokenSet.size()); 
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
		Assert.assertEquals(Status.ACTIVE_SESSION,resp.getStatus());
		Assert.assertEquals(userId,resp.getUser().getId().intValue());

		// has valid timestamp
		Assert.assertTrue(resp.getTokenTimestamp() <= System.currentTimeMillis());
		Assert.assertTrue(resp.getTokenTimestamp() > System.currentTimeMillis()-1000);
		
	}
	
	private Cookie create(String name, String value, String path, String domain, int version, String comment, int maxAge, boolean secure) {
		Cookie cookie = new Cookie(name,value);
		cookie.setPath(path);
		cookie.setDomain(domain);
		cookie.setVersion(version);
		cookie.setComment(comment);
		cookie.setMaxAge(maxAge);
		cookie.setSecure(secure);
		return cookie;
	}

}
