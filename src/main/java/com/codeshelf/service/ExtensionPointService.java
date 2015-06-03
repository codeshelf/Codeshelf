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

public class ExtensionPointService {

	private static final Logger	LOGGER				= LoggerFactory.getLogger(ExtensionPointService.class);

	ScriptEngine				engine;

	HashSet<ExtensionPointType>		activeExtensions	= new HashSet<ExtensionPointType>();

	public ExtensionPointService() {
		init();
	}

	public void init() {
		ScriptEngineManager factory = new ScriptEngineManager();
		engine = factory.getEngineByName("groovy");
	}

	public void addExtensionPoint(ExtensionPointType extp, String functionScript) throws ScriptException {
		if (engine == null) {
			LOGGER.error("engine not set up in ScriptingService.  Need to run gradle?");
			return;
		}
		engine.eval(functionScript);
		this.activeExtensions.add(extp);
	}

	public boolean hasExtensionPoint(ExtensionPointType extp) {
		return this.activeExtensions.contains(extp);
	}

	public List<ExtensionPoint> load(Facility facility) throws ScriptException {
		List<ExtensionPoint> eps = ExtensionPoint.staticGetDao().findByParent(facility);
		for (ExtensionPoint ep : eps) {
			if (ep.isActive()) {
				LOGGER.info("Adding extension point " + ep.getType());
				this.addExtensionPoint(ep.getType(), ep.getScript());
			}
			else {
				LOGGER.info("Skipping inactive extension point " + ep.getType());
			}
		}
		return eps;
	}

	public Object eval(ExtensionPointType ext, Object[] params) throws ScriptException {
		if (!this.activeExtensions.contains(ext)) {
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

	public static ExtensionPointService createInstance() {
		// new instance every time now.  would be good to re-use on a tenant/facility level.
		return new ExtensionPointService();
	}

}
