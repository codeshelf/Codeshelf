package com.codeshelf.edi;

import java.lang.reflect.InvocationTargetException;

import javax.ws.rs.core.MultivaluedMap;

import org.junit.Assert;
import org.junit.Test;

import com.sun.jersey.api.representation.Form;

public class SftpConfigurationTest {

	@Test
	public void testSettingPort() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Integer testPort = Integer.valueOf(2222);
		SftpConfiguration config = new SftpConfiguration();
		MultivaluedMap<String, String> params = new Form();
		params.add("port", String.valueOf(testPort));
		SftpConfiguration updatedConfig = SftpConfiguration.updateFromMap(config, params);
		Assert.assertEquals(testPort, updatedConfig.getPort());
	}

	@Test
	public void testSettingPortEmpty() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		SftpConfiguration config = new SftpConfiguration();
		MultivaluedMap<String, String> params = new Form();
		params.add("port", "");
		SftpConfiguration updatedConfig = SftpConfiguration.updateFromMap(config, params);
		Assert.assertEquals(null, updatedConfig.getPort());
	}

	@Test
	public void testOnNullEncodedPasswordRemains() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		SftpConfiguration config = new SftpConfiguration();
		config.setPassword("testme");
		String priorEncodedPassword =  config.getPasswordEnc();
		Assert.assertNotNull(priorEncodedPassword);
		MultivaluedMap<String, String> params = new Form();
		params.add("password", null);
		SftpConfiguration updatedConfig = SftpConfiguration.updateFromMap(config, params);
		Assert.assertEquals(priorEncodedPassword, updatedConfig.getPasswordEnc());
	}

	@Test
	public void testEncodedPasswordChanges() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		SftpConfiguration config = new SftpConfiguration();
		config.setPassword("testme");
		String priorEncodedPassword =  config.getPasswordEnc();
		Assert.assertNotNull(priorEncodedPassword);
		MultivaluedMap<String, String> params = new Form();
		params.add("password", "other");
		SftpConfiguration updatedConfig = SftpConfiguration.updateFromMap(config, params);
		Assert.assertNotEquals(priorEncodedPassword, updatedConfig.getPasswordEnc());
	}

	
}
