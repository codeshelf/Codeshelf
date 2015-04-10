package com.codeshelf.security;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;

@SuppressWarnings("serial")
public class AuthServlet extends HttpServlet {

	private static final Logger	LOGGER						= LoggerFactory.getLogger(AuthServlet.class);
	
	private static final String CONTENT_TYPE_JSON = "application/json";
	private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

    TokenSessionService tokenSessionService;
	
	public AuthServlet() {
		this.tokenSessionService = TokenSessionService.getInstance();
	}

	@Override
	protected void doPost(HttpServletRequest req,
					HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader(CACHE_CONTROL_HEADER, NO_CACHE);
        resp.setContentType(CONTENT_TYPE_JSON);

        String username = req.getParameter("u");
		String password = req.getParameter("p");
		boolean authSuccess = false;
		
		if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password)) {
			// authenticate method will log success/failure of attempt
			TokenSession auth = TokenSessionService.getInstance().authenticate(username, password);
			if (auth.getStatus().equals(TokenSession.Status.ACCEPTED)) {
				resp.setStatus(Status.OK.getStatusCode());

				// put token into a cookie
				Cookie cookie = tokenSessionService.createAuthCookie(auth.getNewToken());
				resp.addCookie(cookie);
				
				// send user as body of response
				sendUser(auth.getUser(),resp);
				
				authSuccess=true;
			}  
		} else {
			LOGGER.warn("Login failed: 'u' and/or 'p' parameters not submitted");
		}
		if(!authSuccess) { // TODO: clear token cookie here
			resp.setStatus(Status.FORBIDDEN.getStatusCode());
		}
	}
    
	@Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

        resp.setHeader(CACHE_CONTROL_HEADER, NO_CACHE);
        resp.setContentType(CONTENT_TYPE_JSON);
    
    	TokenSession tokenSession = TokenSessionService.getInstance().checkAuthCookie(req.getCookies());
    	if(tokenSession.getStatus().equals(TokenSession.Status.ACCEPTED)) {
        	resp.setStatus(Status.OK.getStatusCode());
        	sendUser(tokenSession.getUser(),resp);    		
    	} else {
    		resp.setStatus(Status.FORBIDDEN.getStatusCode());
    	}
    }

	private void sendUser(User user, HttpServletResponse resp) throws IOException {
		ObjectMapper mapper = new ObjectMapper(); // TODO: should reuse and share globally per jackson documentation -ic
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		String jsonUser = mapper.writeValueAsString(user);

        PrintWriter out = resp.getWriter();
		out.print(jsonUser);
        out.close();
	}

}
