package com.gadgetworks.codeshelf.application;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.platform.persistence.SchemaManager;

public class ServiceControlServlet extends HttpServlet {
	private static final String CONTENT_TYPE_TEXT = "text/html";
	private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final String NO_CACHE = "must-revalidate,no-cache,no-store";
    
    private static final int ACTION_DELAY_SECONDS = 5;
    private static final ScheduledExecutorService worker = 
    		  Executors.newSingleThreadScheduledExecutor();
    
	private ApplicationABC	application;
	private SchemaManager schemaManager = null;

    public ServiceControlServlet(ApplicationABC application, boolean enableSchemaManagement) {
    	this.application = application;
    	if(enableSchemaManagement) {
        	this.schemaManager = PersistenceService.getInstance().getSchemaManager();
    	}
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
        out.println("<html><head><title>service control</title></head><body><h4>service control</h4>");

        out.println("<form name=stop action='#' method=post><input type=hidden name='action' value='stop' /></form>");
        out.println("<form name=dropschema action='#' method=post><input type=hidden name='action' value='dropschema' /></form>");
        out.println("<form name=deleteorderswis action='#' method=post><input type=hidden name='action' value='deleteorderswis' /></form>");

        String action = req.getParameter("action");
        if(action!=null) {
            if(action.equals("stop")) {
            	out.println("APP SERVER SHUTDOWN. Service will stop in "+ACTION_DELAY_SECONDS+" seconds");
            	stop(ApplicationABC.ShutdownCleanupReq.NONE);
            } else if(schemaManager != null) {
            	// schema actions
            	if(action.equals("dropschema")) {
                	out.println("DROP SCHEMA. Service will stop in "+ACTION_DELAY_SECONDS+" seconds");
            		stop(ApplicationABC.ShutdownCleanupReq.DROP_SCHEMA);
            		
            	} else if(action.equals("deleteorderswis")) {
                	out.println("DELETE ORDERS AND WORK INSTRUCTIONS. Service will stop in "+ACTION_DELAY_SECONDS+" seconds");
            		stop(ApplicationABC.ShutdownCleanupReq.DELETE_ORDERS_WIS);
            	} else {
            		out.println("Invalid command.");
            	}
        	} else {
        		out.println("Invalid command.");
        	}
            out.println("</br></br>");
        } else {
            out.println("<a href=\"javascript:document.stop.submit()\">Shutdown (restart) this service</a></br></br>");
            if(schemaManager != null) {
                out.println("<a href=\"javascript:document.dropschema.submit()\">Erase Database, then Shutdown (restart) this service</a></br></br>");
                out.println("<a href=\"javascript:document.deleteorderswis.submit()\">Delete Orders and Work Instructions, then Shutdown (restart) this service</a></br></br>");        
            }

        }
        
        out.println("</body></html>");        
    }

    private void stop(final ApplicationABC.ShutdownCleanupReq cleanup) {
    	Runnable stopper = new Runnable() {
    		public void run() {
    			application.stopApplication(cleanup);
    		}
    	};
    	
		worker.schedule(stopper, ACTION_DELAY_SECONDS, TimeUnit.SECONDS);		
    }
}
