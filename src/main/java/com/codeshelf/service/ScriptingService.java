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
import com.codeshelf.model.domain.ExtensionPoint;

public class ScriptingService {

	private static final Logger	LOGGER				= LoggerFactory.getLogger(ScriptingService.class);

	ScriptEngine				engine;

	HashSet<ExtensionPointType>		availableExtentions	= new HashSet<ExtensionPointType>();

	public ScriptingService() {
		init();
	}

	public void init() {
		ScriptEngineManager factory = new ScriptEngineManager();
		engine = factory.getEngineByName("groovy");
	}

	public void addExtentionPoint(ExtensionPointType extp, String functionScript) throws ScriptException {
		if (engine == null) {
			LOGGER.error("engine not set up in ScriptingService.  Need to run gradle?");
			return;
		}
		engine.eval(functionScript);
		this.availableExtentions.add(extp);
	}

	public boolean hasExtentionPoint(ExtensionPointType extp) {
		return this.availableExtentions.contains(extp);
	}

	public List<ExtensionPoint> loadScripts(Facility facility) throws ScriptException {
		List<ExtensionPoint> scripts = ExtensionPoint.staticGetDao().findByParent(facility);
		for (ExtensionPoint script : scripts) {
			LOGGER.info("Adding extention point " + script.getType());
			this.addExtentionPoint(script.getType(), script.getScript());
		}
		return scripts;
	}

	public Object eval(Facility facility, ExtensionPointType ext, Object[] params) throws ScriptException {
		if (!this.availableExtentions.contains(ext)) {
			LOGGER.info("Unable to eval extension point: Script it");
			return null;
		}
		Invocable inv = (Invocable) engine;
		Object result = null;
		try {
			result = inv.invokeFunction(ext.name(), params);
		} catch (NoSuchMethodException e) {
			LOGGER.error("Failed to evaluate " + ext.toString(), e);
		}
		return result;
	}

	public static ScriptingService createInstance() {
		// new instance every time now.  would be good to re-use on a tenant/facility level.
		return new ScriptingService();
	}

}
