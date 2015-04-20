package com.codeshelf.security.api;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.manager.User;
import com.codeshelf.security.TokenSession;
import com.codeshelf.security.TokenSessionService;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

@Path("/")
public class AuthResource {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(AuthResource.class);
	private TokenSessionService	tokenSessionService;

	public AuthResource() {
		this(TokenSessionService.getInstance());
	}
	
	AuthResource(TokenSessionService tokenSessionService) {
		this.tokenSessionService = tokenSessionService;
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response authenticate(@FormParam("u") String username, @FormParam("p") String password) {

		if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password)) {
			// authenticate method will log success/failure of attempt
			TokenSession tokenSession = tokenSessionService.authenticate(username, password);
			if (tokenSession != null && tokenSession.getStatus().equals(TokenSession.Status.ACCEPTED)) {
				// put token into a cookie
				NewCookie cookie = tokenSessionService.createAuthNewCookie(tokenSession.getNewToken());
				return Response.ok(toUserResponse(tokenSession)).cookie(cookie).build();
			}
		} else {
			LOGGER.warn("Login failed: 'u' and/or 'p' parameters not submitted");
		}
		return Response.status(Status.UNAUTHORIZED.getStatusCode()).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAuthorizedUser(@CookieParam(TokenSessionService.COOKIE_NAME) Cookie authCookie) {

		TokenSession tokenSession = tokenSessionService.checkAuthCookie(authCookie);
		if (tokenSession != null && tokenSession.getStatus().equals(TokenSession.Status.ACCEPTED)) {
			return Response.ok(toUserResponse(tokenSession)).build();
		}
		return Response.status(Status.UNAUTHORIZED.getStatusCode()).build();
	}

	@GET
	@Path("/logout")
	public Response logout() {
		return Response.ok().header("Set-Cookie", tokenSessionService.removerCookie()).build();
	}

	@POST
	@Path("/recover")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response recover(@FormParam("u") String username) {
		if (!Strings.isNullOrEmpty(username)) {
			// TODO: implement
			// random delay to reduce feasibility of timing attack in determining valid usernames			
		} else {
		}
		return Response.status(Status.FORBIDDEN.getStatusCode()).build();
	}

	@POST
	@Path("/pw")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response changePassword(@FormParam("old") String oldPassword,
		@FormParam("new") String newPassword,
		@CookieParam(TokenSessionService.COOKIE_NAME) Cookie authCookie) {

		TokenSession tokenSession = tokenSessionService.checkAuthCookie(authCookie);
		if (tokenSession != null 
				&& !Strings.isNullOrEmpty(oldPassword)
				&& !Strings.isNullOrEmpty(newPassword)
				&& tokenSession.getStatus().equals(TokenSession.Status.ACCEPTED)) {
			// verify password
			TokenSession authTest = tokenSessionService.authenticate(tokenSession.getUser().getUsername(), oldPassword);
			if (authTest != null && authTest.getStatus().equals(TokenSession.Status.ACCEPTED)) {
				// change password
				User user = authTest.getUser();
				user.setHashedPassword(tokenSessionService.hashPassword(newPassword));
				TenantManagerService.getInstance().updateUser(user);
				return Response.ok(user).build();
			}
		} 
		LOGGER.warn("Invalid password change request");
		return Response.status(Status.FORBIDDEN.getStatusCode()).build();
	}

	private Map<String,Object> toUserResponse(TokenSession session) {
		return ImmutableMap.of("tenant", ImmutableMap.of("name", session.getTenant().getName()),
							   "user", session.getUser());
	}

}

