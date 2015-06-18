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

	public ExtensionPointService(Facility facility) throws ScriptException {
		initEngine();
		load(facility);
	}

	private void initEngine() throws ScriptException {
		ScriptEngineManager factory = new ScriptEngineManager();
		engine = factory.getEngineByName("groovy");
		if (engine == null) {
			throw new ScriptException("groovy engine not setup in ExtensionPointService.  Need to run gradle eclipse?");
		}
	}

	private void addExtensionPoint(ExtensionPointType extp, String functionScript) throws ScriptException {
		engine.eval(functionScript);
		this.activeExtensions.add(extp);
	}

	private void clearExtensionPoints() {
		this.activeExtensions.clear();
	}


	public boolean hasExtensionPoint(ExtensionPointType extp) {
		return this.activeExtensions.contains(extp);
	}

	private List<ExtensionPoint> load(Facility facility) throws ScriptException {
		List<ExtensionPoint> eps = ExtensionPoint.staticGetDao().findByParent(facility);
		this.clearExtensionPoints();
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
			throw new ScriptException("Script type " + ext + " is not active");
		}
		Invocable inv = (Invocable) engine;
		Object result = null;
		try {
			result = inv.invokeFunction(ext.name(), params);
		} catch (NoSuchMethodException e) {
			throw new ScriptException("Script type " + ext + " does not contain method name " +  ext.name());
		}
		return result;
	}

	public static ExtensionPointService createInstance(Facility facility) throws ScriptException {
		// new instance every time now.  would be good to re-use on a tenant/facility level.
		return new ExtensionPointService(facility);
	}

}
