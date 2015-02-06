package com.gadgetworks.codeshelf.application;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gadgetworks.codeshelf.model.domain.User;

@SuppressWarnings("serial")
public class UsersServlet extends HttpServlet {
	private static final String CONTENT_TYPE_TEXT = "text/html";
	private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final String NO_CACHE = "must-revalidate,no-cache,no-store";
    
    public UsersServlet() {
    }

    @Override
    protected void doGet(HttpServletRequest req,
        HttpServletResponse resp) throws ServletException, IOException {
    	doPost(req,resp);
    }
    
    @Override
    protected void doPost(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader(CACHE_CONTROL_HEADER, NO_CACHE);
        resp.setContentType(CONTENT_TYPE_TEXT);
                
        PrintWriter out = resp.getWriter();
        out.println("<html><head><title>service control</title></head><body><h4>user management</h4>");
        
        /*
        PersistenceService.getInstance().beginTenantTransaction();
        List<User> users = User.DAO.getAll();
        for(User u : users) {
            out.println("<form name=stop action='#' method=post><input type=hidden name='action' value='stop' /></form>");
        }
        PersistenceService.getInstance().commitTenantTransaction();
        */
        
        String schema = req.getParameter("schema");
        String org = req.getParameter("org");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        if(schema!=null && org!=null && email!=null && password !=null) {
        	//out.println("<!-- ");
        	try {
				out.println(User.generatePasswordUpdateSql(org, email, password, schema));
			} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
				out.println("error: "+e.getMessage());
			}
        	//out.println(" -->");
        	out.println("<br/>");
        } else {
        	schema="codeshelf";
        	org="DEMO1";
        	email="@example.com";
        	password="testme";
        }
        
        out.println("<form action=\"#\" method=POST>");
        out.println("<table border=1>");
        out.println("<tr><td>schema:<td><input type=text name=schema value='"+schema+"' /></tr>");
        out.println("<tr><td>orgName:<td><input type=text name=org value='"+org+"' /></tr>");
        out.println("<tr><td>email:<td><input type=text name=email value='"+email+"' /></tr>");
        out.println("<tr><td>password:<td><input type=text name=password value='"+password+"'/></tr>");
        out.println("</table>");

        out.println("<input type=submit name=submit /></br>");
        out.println("</body></html>");        
    }
}

