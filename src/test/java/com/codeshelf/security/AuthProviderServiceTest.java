package com.codeshelf.security;

import javax.servlet.http.Cookie;
import javax.ws.rs.core.NewCookie;

import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.testframework.MinimalTest;

public class AuthProviderServiceTest extends MinimalTest {
	@Test
	public void cryptoCookieTest() {
		AuthProviderService auth = new HmacAuthService().initialize();
		
		// can create auth cookie
		NewCookie newCookie = auth.createAuthCookie(123, auth.getDefaultCookieExpirationSeconds());
		Assert.assertNotNull(newCookie);
		
		Cookie authCookie = convert(newCookie); 
		Cookie otherCookie = create("othercookie","asdfasdfasdf","/","test.com",1,"comment",86400,false);
		Cookie[] cookies = new Cookie[2];
		// String name, String value, String path, String domain, int version, String comment, int maxAge, boolean secure
		cookies[0] = otherCookie; 
		cookies[1] = authCookie;
		
		// can find and validate auth cookie
		AuthCookieContents validContents = auth.checkAuthCookie(cookies);
		Assert.assertNotNull(validContents);
		Assert.assertEquals(123,validContents.getId());

		// has valid timestamp
		Assert.assertTrue(validContents.getTimestamp() <= System.currentTimeMillis());
		Assert.assertTrue(validContents.getTimestamp() > System.currentTimeMillis()-1000);

		// fail to validate if more than one
		cookies[0] = convert(auth.createAuthCookie(0, 90));
		validContents = auth.checkAuthCookie(cookies);
		Assert.assertNull(validContents);
		cookies[0] = otherCookie;
		validContents = auth.checkAuthCookie(cookies);
		Assert.assertNotNull(validContents);
		
		// fail to validate if wrong version
		int version = authCookie.getVersion();
		authCookie.setVersion(999999);
		validContents = auth.checkAuthCookie(cookies);
		Assert.assertNull(validContents);
		authCookie.setVersion(version);
		validContents = auth.checkAuthCookie(cookies);
		Assert.assertNotNull(validContents);
		
		// fail to validate if altered
		String value = authCookie.getValue();
		Assert.assertTrue(Base64.isBase64(value));
		byte[] decoded = Base64.decodeBase64(value);
		Assert.assertTrue(decoded[0] == '1'); // first character of uid
		decoded[0] = '2';
		authCookie.setValue(Base64.encodeBase64(decoded).toString());
		validContents = auth.checkAuthCookie(cookies);
		Assert.assertNull(validContents);		
	}
	
	@Test
	public void passwordHashTest() {
		AuthProviderService auth = new HmacAuthService().initialize();

		String password = "goodpassword";
		Assert.assertTrue(auth.passwordMeetsRequirements(password));
		Assert.assertFalse(auth.passwordMeetsRequirements(""));

		String hash = auth.hashPassword(password);
		Assert.assertTrue(auth.hashIsValid(hash));
		Assert.assertFalse(auth.hashIsValid("$myhash$asdf"));
		
		Assert.assertTrue(auth.checkPassword(password, hash));
		Assert.assertFalse(auth.checkPassword(password+"!", hash));
	}

	private Cookie convert(NewCookie newCookie) {
		return create(newCookie.getName(),newCookie.getValue(),newCookie.getPath(),
			newCookie.getDomain(),newCookie.getVersion(),newCookie.getComment(),
			newCookie.getMaxAge(),newCookie.isSecure());
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
