package com.codeshelf.application;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

@SuppressWarnings("serial")
public class LoggingServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final String CONTENT_TYPE_TEXT = "text/html";
	private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

    private static final String LEVEL_OPTIONS="<option value=ALL>ALL<option value=TRACE>TRACE</option><option value=DEBUG>DEBUG</option><option selected value=INFO>INFO</option><option value=WARN>WARN</option><option value=ERROR>ERROR</option><option value=FATAL>FATAL<option value=OFF>OFF";

	private static final Map<String,String> presets = new HashMap<String,String>() {{
    	put("SQL Logging OFF","class1=org.hibernate.type&level1=INFO&class2=org.hibernate.SQL&level2=INFO");
    	put("SQL Logging ON","class1=org.hibernate.type&level1=TRACE&class2=org.hibernate.SQL&level2=TRACE");
    }};
    
    public LoggingServlet() {
    }
    
    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader(CACHE_CONTROL_HEADER, NO_CACHE);
        resp.setContentType(CONTENT_TYPE_TEXT);
                
        PrintWriter out = resp.getWriter();
        out.println("<html><head><title>log level override</title></head><body><h4>logger level overrides:</h4>");

        String response = setLevel(req.getParameter("class1"),req.getParameter("level1"));
        if(response!=null) 
        	out.println("* Last action: "+response+"<br>");
        response = setLevel(req.getParameter("class2"),req.getParameter("level2"));
        if(response!=null) 
        	out.println("* Last action: "+response+"<br>");
        
        out.println("<form action=\"#\" method=GET onsubmit=\"return confirm('Are you sure?');\">");
        out.println("<table border=0><tr><td nowrap>logger<td nowrap>level<td></tr>");        
        out.println("<tr><td nowrap><input type=text name=class1><td nowrap><select name=level1>"+LEVEL_OPTIONS+"</select></tr>");
        out.println("<tr><td nowrap><input type=text name=class2><td nowrap><select name=level2>"+LEVEL_OPTIONS+"</select></tr>");
        out.println("<tr><td colspan=2 align=center><input type=submit value=Change></tr></table><br><h4>presets:</h4>");
        
        for(String preset : presets.keySet()) {
        	out.println("<a href=\"?"+presets.get(preset)+"\">"+preset+"</a><br>");
        }
        
        out.println("</form><h4>available loggers:</h4>");
        
        Configuration conf = getConfiguration();
        Collection<LoggerConfig> logConfigs = conf.getLoggers().values();
        
        ArrayList<String> loggerDescriptions = new ArrayList<String>();

        for(LoggerConfig logConfig : logConfigs) {
    		loggerDescriptions.add(logConfig.getName() + " (" + logConfig.getLevel().name()+")");
        }
        Collections.sort(loggerDescriptions);
        for(String description : loggerDescriptions) {
        	out.println("<li>"+description);
        }

        out.println("</body></html>");
        
        out.close();

    }
    
    private Configuration getConfiguration() {
        LoggerContext ctx = (LoggerContext)LogManager.getContext(false);
        return ctx.getConfiguration();
    }

	private String setLevel(String loggerName, String levelName) {
		if(loggerName == null)
			return null;
		if(loggerName.isEmpty())
			return null;

		Configuration conf = getConfiguration();
		LoggerConfig logger = conf.getLoggerConfig(loggerName);
		if (logger == null) {
			return "Invalid logger name - "+loggerName;
	    }
		Level level = Level.toLevel(levelName); // returns DEBUG for invalid input
		
		if(logger.getName().equals(loggerName)) {
			logger.setLevel(level);
		    return "Set logging for "+loggerName+" to "+level.toString(); 
		} else {
			return "Sorry, no specific loggerConfig exists for "+loggerName;
		}			    
	}
}
