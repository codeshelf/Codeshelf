package com.codeshelf.security.api;

import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.email.EmailService;
import com.codeshelf.manager.User;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.security.SecurityEmails;
import com.codeshelf.security.TokenSessionService;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

@Path("/recovery")
public class RecoveryResource {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(RecoveryResource.class);

	@POST
	@Path("start")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response start(@FormParam("u") String username) {
		if (!Strings.isNullOrEmpty(username) && EmailService.getInstance().isEmailAddress(username)) {
			User user = TenantManagerService.getInstance().getUser(username);
			if(user != null) {
				int remainingRecoveryEmails = user.getRecoveryEmailsRemain();
				if(remainingRecoveryEmails >0) {
					int minimumMinutesSinceLastEmail = Integer.getInteger("auth.session.recovery.idleminutes", TokenSessionService.RECOVERY_DEFAULT_IDLE_MINUTES);
					Date threshold = (new DateTime()).minusMinutes(minimumMinutesSinceLastEmail).toDate();
					
					if(user.getLastRecoveryEmail() == null 
							|| user.getLastRecoveryEmail().before(threshold)) {
						
						LOGGER.info("Account recovery requested for user {}",user.getUsername());
						user.setRecoveryEmailsRemain(remainingRecoveryEmails - 1);
						user.setLastRecoveryEmail();
						TenantManagerService.getInstance().updateUser(user);
						SecurityEmails.sendRecovery(user);
					} else {
						LOGGER.warn("Not sending recovery email to {} because it was requested too recently",user.getUsername());
					}
				} else {
					LOGGER.warn("Not sending recovery email to {} because user does not have any recoveries remaining",user.getUsername());
				}
			} else {
				// email address is not a user
				LOGGER.warn("Account recovery attempted for email address {} , user not found",username);
			}
			
			// always return 200 ok on anything that looks like an email address, regardless of action taken
			return Response.ok(ImmutableMap.of("u", username)).build();
		}
		LOGGER.warn("Could not parse email provided for starting account recovery");
		return Response.status(Status.BAD_REQUEST).build();
	}


}
