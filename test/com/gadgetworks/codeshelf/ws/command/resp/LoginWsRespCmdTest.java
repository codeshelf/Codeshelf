package com.gadgetworks.codeshelf.ws.command.resp;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.gadgetworks.codeshelf.model.dao.DAOTestABC;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmd;


@RunWith(MockitoJUnitRunner.class)
public class LoginWsRespCmdTest extends DAOTestABC {

	@Mock
	private IDaoProvider mockDaoProvider;
	
	@Test
	public void shouldIncludeUser() throws JsonParseException, JsonMappingException, IOException {
		User user = new User();
		user.setEmail("test@test.com");
		Organization organization = new Organization();
		LoginWsRespCmd cmd = new LoginWsRespCmd(IWsReqCmd.SUCCEED, organization, user);
		JsonNode node = cmd.getResponseNode().findPath("user");
		Assert.assertEquals(user.getEmail(), node.get("email").asText());
	}

	@Test
	public void shouldNotIncludeUserOnFail() throws JsonParseException, JsonMappingException, IOException {
		User user = new User();
		user.setEmail("test@test.com");
		Organization organization = new Organization();
		LoginWsRespCmd cmd = new LoginWsRespCmd(IWsReqCmd.FAIL, organization, user);
		cmd.getResponseNode().has("user");
		Assert.assertFalse(cmd.getResponseNode().has("user"));
	}


	
	@Test
	public void shouldNotIncludePassword() throws JsonParseException, JsonMappingException, IOException {
		JsonNode node = createUserJsonNode();
		Assert.assertFalse(node.has("password"));
	}
	
	@Test
	public void shouldNotIncludeHashedPassword() throws JsonParseException, JsonMappingException, IOException {
		JsonNode node = createUserJsonNode();
		Assert.assertFalse(node.has("hashedPassword"));
	}

	@Test
	public void shouldNotIncludeHashSalt() throws JsonParseException, JsonMappingException, IOException {
		JsonNode node = createUserJsonNode();
		Assert.assertFalse(node.has("hashSalt"));
	}
	
	private JsonNode createUserJsonNode() {
		User user = new User();
		user.setPassword("testPassword");
		Organization organization = new Organization();
		LoginWsRespCmd cmd = new LoginWsRespCmd(IWsReqCmd.SUCCEED, organization, user);
		JsonNode node = cmd.getResponseNode().findPath("user");
		return node;
	}

	
}
