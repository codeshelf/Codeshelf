package com.codeshelf.security.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.SecurityQuestion;
import com.codeshelf.manager.User;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.security.SessionFlags.Flag;
import com.codeshelf.security.TokenSession;
import com.codeshelf.security.TokenSessionService;

@Path("/questions")
public class SecurityQuestionsResource {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(SecurityQuestionsResource.class);
	private TokenSessionService	tokenSessionService;
	
	public SecurityQuestionsResource() {
		this(TokenSessionService.getInstance());
	}

	public SecurityQuestionsResource(TokenSessionService tokenSessionService) {
		this.tokenSessionService = tokenSessionService;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getActiveSecurityQuestions(@CookieParam(TokenSessionService.COOKIE_NAME) Cookie authCookie,
		@QueryParam(TokenSessionService.COOKIE_NAME) String authToken) {
		
		// accept token from either query param (recovery) or cookie (account settings) 
		TokenSession tokenSession = tokenSessionService.checkToken(authToken);
		if(tokenSession == null)
			tokenSession = tokenSessionService.checkAuthCookie(authCookie);

		// allow with either regular session or account_setup session
		if (tokenSession != null 
				&& (tokenSession.getStatus().equals(TokenSession.Status.ACTIVE_SESSION)
						|| (tokenSession.getStatus().equals(TokenSession.Status.SPECIAL_SESSION) 
								&& tokenSession.getSessionFlags().get(Flag.ACCOUNT_SETUP) 
								&& !tokenSession.getSessionFlags().get(Flag.ACCOUNT_RECOVERY) ))) {
			Map<String,String> questionStrings = TenantManagerService.getInstance().getActiveSecurityQuestions();
			return Response.ok(questionStrings).build();
		} // else
		LOGGER.warn("No valid token for getting security questions");
		return Response.status(Status.UNAUTHORIZED.getStatusCode()).build();
	}
	
	@POST
	@Path("mine")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response setMySecurityQuestions(MultivaluedMap<String, String> questionCodesAndAnswers,
		@CookieParam(TokenSessionService.COOKIE_NAME) Cookie authCookie,
		@QueryParam(TokenSessionService.COOKIE_NAME) String authToken) {

		// accept token from either query param (recovery) or cookie (account settings) 
		TokenSession tokenSession = tokenSessionService.checkToken(authToken);
		if(tokenSession == null)
			tokenSession = tokenSessionService.checkAuthCookie(authCookie);
		
		// allow with either regular session or account_setup session (latter only if user has never logged in) 
		if (tokenSession != null 
				&& (tokenSession.getStatus().equals(TokenSession.Status.ACTIVE_SESSION)
						|| (tokenSession.getStatus().equals(TokenSession.Status.SPECIAL_SESSION) 
								&& tokenSession.getSessionFlags().get(Flag.ACCOUNT_SETUP) 
								&& !tokenSession.getSessionFlags().get(Flag.ACCOUNT_RECOVERY)
								&& tokenSession.getUser().getLastAuthenticated() == null))) {
			Map<SecurityQuestion, String> questionMap = formValidSecurityQuestionMap(questionCodesAndAnswers);
			if(questionMap != null) {
				TenantManagerService.getInstance().setSecurityAnswers(tokenSession.getUser(), questionMap);
				LOGGER.info("Set new security questions for user");
				return Response.ok(questionMap.keySet().size()).build();
			}//else (reason logged by formValidSecurityQuestionMap)
			return Response.status(Status.BAD_REQUEST.getStatusCode()).build();
		}
		LOGGER.warn("No valid token for set security questions request");
		return Response.status(Status.UNAUTHORIZED.getStatusCode()).build();
	}
	
	@GET
	@Path("mine")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMySecurityQuestions(@CookieParam(TokenSessionService.COOKIE_NAME) Cookie authCookie, 
		@QueryParam(TokenSessionService.COOKIE_NAME) String authToken) {

		// accept token from either query param (recovery) or cookie (account settings) 
		TokenSession tokenSession = tokenSessionService.checkToken(authToken);
		if(tokenSession == null)
			tokenSession = tokenSessionService.checkAuthCookie(authCookie);

		// allow with regular session, or account_recovery
		if (tokenSession != null 
				&& (tokenSession.getStatus().equals(TokenSession.Status.ACTIVE_SESSION)
						|| (tokenSession.getStatus().equals(TokenSession.Status.SPECIAL_SESSION)
								&& tokenSession.getSessionFlags().get(Flag.ACCOUNT_RECOVERY) 
								&& !tokenSession.getSessionFlags().get(Flag.ACCOUNT_SETUP)))) {
			return Response.ok(tokenSession.getUser().getSecurityAnswers().keySet()).build();
		} 
		LOGGER.warn("Invalid set security questions request");
		return Response.status(Status.FORBIDDEN.getStatusCode()).build();
	}
	
	@POST
	@Path("check")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response checkSecurityAnswers(MultivaluedMap<String, String> questionCodesAndAnswers,
		@QueryParam(TokenSessionService.COOKIE_NAME) String authToken) {

		Map<SecurityQuestion, String> qmap = formValidSecurityQuestionMap(questionCodesAndAnswers);

		// accept token from query param (recovery)
		TokenSession currentSession = tokenSessionService.checkToken(authToken);
		User user = currentSession.getUser();
		if(user.getRecoveryTriesRemain() > 0) {
			user.setRecoveryTriesRemain(user.getRecoveryTriesRemain() - 1); // decrement
			TenantManagerService.getInstance().updateUser(user);
			
			TokenSession newSession = tokenSessionService.checkSecurityAnswers(currentSession, qmap);
			
			// allowed only with account_recovery session
			if(newSession != null && newSession.getStatus().equals(TokenSession.Status.SPECIAL_SESSION) 
					&& newSession.getSessionFlags().get(Flag.ACCOUNT_RECOVERY)
					&& !newSession.getSessionFlags().get(Flag.ACCOUNT_SETUP)) {

				// record last login
				user.setLastAuthenticated();
				TenantManagerService.getInstance().updateUser(user);

				NewCookie cookie = tokenSessionService.createAuthNewCookie(newSession.getNewToken());
				return Response.ok(newSession.getUser()).cookie(cookie).build();
			} // else
		} else {
			LOGGER.warn("No more recovery attempts for user {}",currentSession.getUser().getUsername());
		}
		
		return Response.status(Status.UNAUTHORIZED.getStatusCode()).build();
	}
	
	private Map<SecurityQuestion,String> formValidSecurityQuestionMap(MultivaluedMap<String,String> questionCodesAndAnswers) {
		Map<SecurityQuestion,String> questionMap = new HashMap<SecurityQuestion,String>();
		Map<String,SecurityQuestion> allQuestions = TenantManagerService.getInstance().getAllSecurityQuestions();
		
		for(String questionCode : questionCodesAndAnswers.keySet()) {
			List<String> answers = questionCodesAndAnswers.get(questionCode);
			if(answers != null && answers.size() == 1) {
				String answer = answers.get(0);
				SecurityQuestion question = allQuestions.get(questionCode);
				if(question != null) {
					if(tokenSessionService.securityAnswerMeetsRequirements(answer)) {
						// valid answer
						questionMap.put(question, answer);
					} else {
						LOGGER.warn("A security answer did not meet requirements (too short/long?)");
						return null;
					}
				} else {
					LOGGER.warn("Security question code is not valid");
					return null;
				}					
			} else {
				LOGGER.warn("Invalid format question/answer map provided when trying to set or evaluate security questions");
				return null;
			} 
		}
		if(questionMap.size() < tokenSessionService.getSecurityAnswerMinCount()) {
			LOGGER.warn("Could not set or evaluate security questions (needed {}, got {})",tokenSessionService.getSecurityAnswerMinCount(),questionMap.size());
			return null;
		} //else ok:
		return questionMap;
	}
}
