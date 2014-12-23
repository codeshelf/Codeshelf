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
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader(CACHE_CONTROL_HEADER, NO_CACHE);
        resp.setContentType(CONTENT_TYPE_TEXT);
                
        PrintWriter out = resp.getWriter();
        out.println("<html><head><title>service control</title></head><body><h4>service control</h4>");
        String action = req.getParameter("action");
        if(action!=null) {
            if(action.equals("stop")) {
            	out.println("<li>Service will stop in "+ACTION_DELAY_SECONDS+" seconds</li>");
            	stop(ApplicationABC.ShutdownCleanupReq.NONE);
            } else if(schemaManager != null) {
            	// schema actions
            	if(action.equals("dropschema")) {
                	out.println("<li>DROPPING SCHEMA. Service will stop in "+ACTION_DELAY_SECONDS+" seconds</li>");
            		stop(ApplicationABC.ShutdownCleanupReq.DROP_SCHEMA);
            	}
            }
            out.println("</br></br>");
        }
        
        out.println("<a href=\"?action=stop\">Shutdown (restart) this service</a></br></br>");
        if(schemaManager != null) {
            out.println("<a href=\"?action=dropschema\">Erase Database, then Shutdown (restart) this service</a></br></br>");        
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

