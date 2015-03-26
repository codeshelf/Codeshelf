package com.codeshelf.security;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.User;

@SuppressWarnings("serial")
public class AuthServlet extends HttpServlet {

	private static final Logger	LOGGER						= LoggerFactory.getLogger(AuthServlet.class);
	
	private static final String CONTENT_TYPE_HTML = "text/html";
	private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

    private static final String DEV_LOGIN_HTML = "<html><head><title>login</title></head><body>"
			+ "<form method=post action=/auth/>u: <input type=text name=u><br>p: <input type=password name=p>"
			+ "<br><input type=hidden name=next value=/mgr/security><input type=submit></form></body></html>";

	AuthProviderService authProviderService;
	
	public AuthServlet() {
		this.authProviderService = HmacAuthService.getInstance();
	}

	@Override
	protected void doPost(HttpServletRequest req,
					HttpServletResponse resp) throws ServletException, IOException {
		String username = req.getParameter("u");
		String password = req.getParameter("p");
		URI nextUri = uri(req.getParameter("next"));
		URI retryUri = uri(req.getParameter("retry"));
		boolean authSuccess = false;
		
		if (username != null && password != null) {
			AuthResponse auth = HmacAuthService.getInstance().authenticate(username, password);
			if (auth.getStatus().equals(AuthResponse.Status.ACCEPTED)) {
				User user = auth.getUser();
				// auth succeeds, generate token
				String token = authProviderService.createToken(user.getId(), user.getTenant().getId());
				Cookie cookie = authProviderService.createAuthCookie(token);
				// redirect to requested location
				resp.addCookie(cookie);
				
				if(nextUri != null) {
					resp.sendRedirect(nextUri.toASCIIString());
				} else {
					resp.sendRedirect("/");
				}
				authSuccess=true;
			} // else authenticate logged reason if failed
		} else {
			LOGGER.warn("Login failed: information not submitted");
		}
		if(!authSuccess) { // TODO: clear token cookie here
			if(retryUri != null) {
				resp.sendRedirect(retryUri.toASCIIString());
			} else {
				resp.setStatus(Status.FORBIDDEN.getStatusCode());
			}
		}
	}
    
	@Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

    	resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader(CACHE_CONTROL_HEADER, NO_CACHE);
        resp.setContentType(CONTENT_TYPE_HTML);
    
        // TODO: look at relogin cookie 
        PrintWriter out = resp.getWriter();
		out.print(DEV_LOGIN_HTML);
        out.close();
    }

    private URI uri(String s) {
    	URI uri = null;
    	if(s!=null) {
        	try {
    			uri = new URI(s);
    		} catch (URISyntaxException e) {
    		}
    	}    	
		return uri;
	}

}
