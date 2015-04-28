package com.codeshelf.email;

import java.io.StringWriter;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.Log4JLogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import com.codeshelf.service.AbstractCodeshelfIdleService;
import com.google.inject.Inject;

public class TemplateService extends AbstractCodeshelfIdleService {
	@SuppressWarnings("unused")
	private static final Logger				VELOCITY_LOGGER			= LogManager.getLogger(VelocityEngine.class.getSimpleName());

	private static final String	PREPEND_TEMPLATE_PATH = "templates/";
	private static final String	APPEND_TEMPLATE_EXTENSION	= ".vm";

	private VelocityEngine	velocityEngine;
	private VelocityContext	defaultContext;
	
	@Inject
	private static TemplateService theInstance;
	
	@Inject
	private TemplateService() {
	}
	
	public static final TemplateService getInstance() {
		return theInstance;
	}

	public static final void setInstance(TemplateService instance) {
		// for testing only
		theInstance = instance;
	}
	
	@Override
	protected void startUp() throws Exception {
		defaultContext = new VelocityContext();
		// set up shared default context here 
		//defaultContext.put("auth.baseurl", System.getProperty("auth.baseurl"));
		
		velocityEngine = new VelocityEngine();
		velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, Log4JLogChute.class.getName());
		velocityEngine.setProperty("runtime.log.logsystem.log4j.logger",VelocityEngine.class.getName());
		velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		velocityEngine.setProperty("classpath.resource.loader.path", "templates");
		velocityEngine.init();
	}

	@Override
	protected void shutDown() throws Exception {
		velocityEngine = null;		
	}

	// all other interfaces to load should call this one:
	private String load(String template, VelocityContext context) {
		StringWriter writer = new StringWriter();
		if(velocityEngine.mergeTemplate(PREPEND_TEMPLATE_PATH+template+APPEND_TEMPLATE_EXTENSION, RuntimeConstants.ENCODING_DEFAULT, context, writer)) {
			return writer.toString();
		} //else
		throw new RuntimeException("Failed to process template "+template);
	}
	
	public String load(String template, Map<String,Object> moreContext) {
		VelocityContext context = new VelocityContext(this.defaultContext);
		for(String key : moreContext.keySet()) {
			context.put(key, moreContext.get(key));
		}
		return load(template,context);
	}
	
	public String load(String template, String name1, Object value1) {
		VelocityContext context = new VelocityContext(this.defaultContext);
		context.put(name1, value1);
		return load(template,context);
	}

	public String load(String template) {
		return load(template,defaultContext);
	}
}
