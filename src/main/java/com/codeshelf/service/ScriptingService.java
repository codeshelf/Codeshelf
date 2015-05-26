package com.codeshelf.service;

import java.util.HashSet;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Script;

public class ScriptingService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptingService.class);

	ScriptEngine engine;
	
	HashSet<ExtensionPoint> availableExtentions = new HashSet<ExtensionPoint>();

	public ScriptingService() {
		init();
	}
	
	public void init() {
		ScriptEngineManager factory = new ScriptEngineManager();
		engine = factory.getEngineByName("groovy");
	}
	
	public void addExtentionPoint(ExtensionPoint extp,String functionScript) throws ScriptException {
		engine.eval(functionScript);
		this.availableExtentions.add(extp);
	}
	
	public boolean hasExtentionPoint(ExtensionPoint extp) {
		return this.availableExtentions.contains(extp);
	}

	public List<Script> loadScripts(Facility facility) throws ScriptException {
		List<Script> scripts = Script.staticGetDao().findByParent(facility);
		for (Script script : scripts) {
			LOGGER.info("Adding extention "+script.getExtension());
			this.addExtentionPoint(script.getExtension(), script.getBody());
		}
 		return scripts;
	}
	
	public Object eval(Facility facility, ExtensionPoint ext, Object[] params) throws ScriptException {
		if (!this.availableExtentions.contains(ext)) {
			LOGGER.info("Unable to eval extension point: Script it");
			return null;
		}
		Invocable inv = (Invocable) engine;
		Object result=null;
		try {
			result = inv.invokeFunction(ext.name(), params);
		}
		catch (NoSuchMethodException e) {
			LOGGER.error("Failed to evaluate "+ext.toString(), e);
		}
		return result;
	}
	
	public static ScriptingService createInstance() {
		// new instance every time now.  would be good to re-use on a tenant/facility level.
		return new ScriptingService();
	}

}
