package com.codeshelf.application;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.manager.service.TenantManagerService.ShutdownCleanupReq;

@SuppressWarnings("serial")
public class ServiceControlServlet extends HttpServlet {
	private static final String CONTENT_TYPE_TEXT = "text/html";
	private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final String NO_CACHE = "must-revalidate,no-cache,no-store";
    
    private static final int ACTION_DELAY_SECONDS = 5;
    private static final ScheduledExecutorService worker = 
    		  Executors.newSingleThreadScheduledExecutor();
    
	private CodeshelfApplication	application;
	boolean enableSchemaManagement;

    public ServiceControlServlet(CodeshelfApplication application, boolean enableSchemaManagement) {
    	this.application = application;
    	this.enableSchemaManagement = enableSchemaManagement;
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
        out.println("<form name=deleteorderswisinventory action='#' method=post><input type=hidden name='action' value='deleteorderswisinventory' /></form>");

        String action = req.getParameter("action");
        if(action!=null) {
            if(action.equals("stop")) {
            	out.println("APP SERVER SHUTDOWN. Service will stop in "+ACTION_DELAY_SECONDS+" seconds");
            	stop(ShutdownCleanupReq.NONE);
            } else if(enableSchemaManagement) {
            	// schema actions
            	if(action.equals("dropschema")) {
                	out.println("DROP SCHEMA. Service will stop in "+ACTION_DELAY_SECONDS+" seconds");
            		stop(ShutdownCleanupReq.DROP_SCHEMA);
            	} else if(action.equals("deleteorderswis")) {
                	out.println("DELETE ORDERS AND WORK INSTRUCTIONS. Service will stop in "+ACTION_DELAY_SECONDS+" seconds");
            		stop(ShutdownCleanupReq.DELETE_ORDERS_WIS);
            	} else if(action.equals("deleteorderswisinventory")) {
                	out.println("DELETE ORDERS AND WORK INSTRUCTIONS AND INVENTORY. Service will stop in "+ACTION_DELAY_SECONDS+" seconds");
            		stop(ShutdownCleanupReq.DELETE_ORDERS_WIS_INVENTORY);
            	} else {
            		out.println("Invalid command.");
            	}
        	} else {
        		out.println("Invalid command.");
        	}
            out.println("</br></br>");
        } else {
            if(enableSchemaManagement) {
                out.println("<h3><a href=\"javascript:if(confirm('Reset Orders/WIs for demo. Are you sure?')){document.deleteorderswis.submit();}\">DEMO RESET 1: Delete Orders and Work Instructions, shutdown (restart)</a></h3></br></br></br></br>");
                out.println("<h3><a href=\"javascript:if(confirm('Reset Orders/WIs/Inventory for demo. Are you sure?')){document.deleteorderswisinventory.submit();}\">DEMO RESET 2: Delete Orders and Work Instructions and Inventory, shutdown (restart)</a></h3></br></br></br></br>");
                out.println("<a href=\"javascript:if(confirm('WARNING - This will delete the facility setup! Are you sure?')){document.dropschema.submit();}\">Erase Database, shutdown (restart)</a></br></br></br></br></br>");
            }
            out.println("<a href=\"javascript:if(confirm('Shutdown/Restart server?')){document.stop.submit();}\">Shutdown (restart) service</a></br></br>");
 
        }
        
        out.println("</body></html>");        
    }

    private void stop(final ShutdownCleanupReq cleanup) {
    	Runnable stopper = new Runnable() {
    		public void run() {
    			if(!cleanup.equals(ShutdownCleanupReq.NONE)) {
    				TenantManagerService.getInstance().setShutdownCleanupRequest(cleanup);
    			}
    			application.stopApplication();
    		}
    	};
    	
    	worker.schedule(stopper, ACTION_DELAY_SECONDS, TimeUnit.SECONDS);		
    }
}

