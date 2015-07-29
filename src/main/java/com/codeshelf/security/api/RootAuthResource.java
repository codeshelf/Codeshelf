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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.User;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.security.SecurityEmails;
import com.codeshelf.security.SessionFlags.Flag;
import com.codeshelf.security.TokenSession;
import com.codeshelf.security.TokenSessionService;
import com.codeshelf.validation.DefaultErrors;
import com.codeshelf.validation.ErrorCode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

@Path("/")
public class RootAuthResource {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(RootAuthResource.class);
	private TokenSessionService	tokenSessionService;

	private static final String OLD = "old";
	
	public RootAuthResource() {
		this(TokenSessionService.getInstance());
	}
	
	RootAuthResource(TokenSessionService tokenSessionService) {
		this.tokenSessionService = tokenSessionService;
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response authenticate(@FormParam("u") String username, @FormParam("p") String password) {

		if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password)) {
			// authenticate method will log success/failure of attempt
			TokenSession tokenSession = tokenSessionService.authenticate(username, password);
			if (tokenSession != null && tokenSession.getStatus().equals(TokenSession.Status.ACTIVE_SESSION)) {
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
		if (tokenSession != null && tokenSession.getStatus().equals(TokenSession.Status.ACTIVE_SESSION)) {
			return Response.ok(toUserResponse(tokenSession)).build();
		}
		return Response.status(Status.UNAUTHORIZED.getStatusCode()).build();
	}

	@GET
	@Path("lastuser")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLastUsername(@CookieParam(TokenSessionService.COOKIE_NAME) Cookie authCookie) {
		TokenSession tokenSession = tokenSessionService.checkAuthCookie(authCookie);
		if (tokenSession != null && tokenSession.getUser() != null) { // any status (timeout, etc) accepted
			return Response.ok(tokenSession.getUser().getUsername()).build();
		}
		return Response.ok("").build();
	}

	@GET
	@Path("logout")
	public Response logout() {
		return Response.ok().header("Set-Cookie", tokenSessionService.removerCookie()).build();
	}

	@POST
	@Path("pw")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response changePassword(@FormParam(OLD) String oldPassword,
		@FormParam("new") String newPassword,
		@FormParam(TokenSessionService.COOKIE_NAME) String authToken,
		@CookieParam(TokenSessionService.COOKIE_NAME) Cookie authCookie) {

		// accept token from either query param (recovery) or cookie (account settings) 
		TokenSession tokenSession = tokenSessionService.checkToken(authToken);
		if(tokenSession == null)
			tokenSession = tokenSessionService.checkAuthCookie(authCookie);
		
		if (tokenSession != null) {
			NewCookie newCookie = null;
			boolean allowChange = false;
			boolean passwordWasSet = (tokenSession.getUser().getHashedPassword() != null);

			if(tokenSession.getStatus().equals(TokenSession.Status.ACTIVE_SESSION)) {
				if (!Strings.isNullOrEmpty(oldPassword)) { 
					// regular password change - old password must be submitted
					TokenSession authTest = tokenSessionService.authenticate(tokenSession.getUser().getUsername(), oldPassword);
					if (authTest != null && authTest.getStatus().equals(TokenSession.Status.ACTIVE_SESSION)) {
						allowChange = true;
					}
				}
				
				if (!allowChange) {
					LOGGER.warn("Could not confirm old password trying to change password for user {} with token status {}",tokenSession.getUser().getUsername(),tokenSession.getStatus());
					DefaultErrors errors = new DefaultErrors();
					errors.rejectValue(OLD, oldPassword, ErrorCode.FIELD_INVALID);
					return Response.status(Status.BAD_REQUEST.getStatusCode()).entity(errors).build();
				}
			} else if(tokenSession.getStatus().equals(TokenSession.Status.SPECIAL_SESSION) 
					&& tokenSession.getSessionFlags().get(Flag.ACCOUNT_SETUP)
					&& (tokenSession.getUser().getHashedPassword() == null || tokenSession.getSessionFlags().get(Flag.ACCOUNT_RECOVERY)) ) {				
				// alternatively, allow user to set password without providing old password, 
				// if ACCOUNT_SETUP (or re-SETUP) mode
				
				allowChange = true;
				newCookie = tokenSessionService.createAuthNewCookie(tokenSession.getNewToken());
			} else {
				LOGGER.warn("Bad request trying to change password for user {} with token status {}",tokenSession.getUser().getUsername(),tokenSession.getStatus());
			}
			if(allowChange) {
				// change password
				User user = tokenSession.getUser();
				user.setHashedPassword(tokenSessionService.hashPassword(newPassword));
				TenantManagerService.getInstance().updateUser(user);
				if(passwordWasSet) {
					SecurityEmails.sendPasswordChanged(user);
				}
				ResponseBuilder responseBuilder = Response.ok(user);
				if(newCookie != null) {
					responseBuilder = responseBuilder.cookie(newCookie);
				}
				return responseBuilder.build();
			}
		} else {
			LOGGER.warn("Invalid password change request (has session = {})",(tokenSession != null));
		}
		return Response.status(Status.FORBIDDEN.getStatusCode()).build();
	}

	private Map<String,Object> toUserResponse(TokenSession session) {
		return ImmutableMap.of("tenant", ImmutableMap.of("name", session.getTenant().getName()),
							   "user", session.getUser());
	}

}

