package com.codeshelf.service;

import groovy.lang.GroovyRuntimeException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import lombok.Getter;

import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.metrics.DataQuantityHealthCheckParameters;
import com.codeshelf.model.DataPurgeParameters;
import com.codeshelf.model.domain.ExtensionPoint;
import com.codeshelf.model.domain.Facility;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class ExtensionPointService {

	private static final Logger	LOGGER				= LoggerFactory.getLogger(ExtensionPointService.class);

	ScriptEngine				engine;

	HashSet<ExtensionPointType>	activeExtensions	= new HashSet<ExtensionPointType>();

	@Getter
	private ArrayList<String>	failedExtensions	= Lists.newArrayList();

	private UUID	facilityPersistentId;

	public ExtensionPointService(Facility facility) throws ScriptException {
		initEngine();
		load(facility);
		facilityPersistentId = facility.getPersistentId();
	}

	private void initEngine() throws ScriptException {
		ScriptEngineManager factory = new ScriptEngineManager();
		engine = factory.getEngineByName("groovy");
		if (engine == null) {
			throw new ScriptException("groovy engine not setup in ExtensionPointService.  Need to run gradle eclipse?");
		}
	}

	private void addExtensionPointIfValid(ExtensionPoint ep) throws ScriptException {
		ExtensionPointType extp = ep.getType();
		String functionScript = ep.getScript();
		try {
			engine.eval(functionScript);
			this.activeExtensions.add(extp);
		} catch (ScriptException e) {
			failedExtensions.add(extp + " " + e);
			Throwable cause = e.getCause();
			if (cause instanceof GroovyRuntimeException) {
				LOGGER.warn("Inactivating invalid extension " + ep.getDomainId(), e);
				ep.setActive(false);
				ExtensionPoint.staticGetDao().store(ep);
			} else {
				throw e;
			}
		}
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
		failedExtensions.clear();
		for (ExtensionPoint ep : eps) {
			if (ep.isActive()) {
				LOGGER.info("Adding extension point " + ep.getType());
				this.addExtensionPointIfValid(ep);
			} else { 
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
		String functionName = ext.name();
		try {
			result = inv.invokeFunction(functionName, params);
		} catch (NoSuchMethodException e) {
			throw new ScriptException("Script type " + ext + " does not contain method name " + functionName
					+ " or encountered parameter mismatch.\n" + e.getMessage());
		}
		return result;
	}

	public static ExtensionPointService createInstance(Facility facility) throws ScriptException {
		// New instance every time now.  Would be good to re-use on a tenant/facility level.
		return new ExtensionPointService(facility);
	}

	// Methods to get the parameter beans	
	//+++++++++++++++++++++++++++++++++++++++

	public Optional<ExtensionPoint> getExtensionPoint(ExtensionPointType type) {
		@SuppressWarnings("unchecked")
		List<ExtensionPoint> eps = ExtensionPoint.staticGetDao().createCriteria()
				.add(Restrictions.eq("type", type))
				.add(Restrictions.eq("parent.persistentId", facilityPersistentId))
				.list();
		if (eps.size() > 1) {
			LOGGER.warn("Fould multiple extension points for type {} in faciltiy {}");
		} else if (eps.size() < 1) {
			LOGGER.warn("Fould multiple extension points for type {} in faciltiy {}");
		} 	 
		
		if (eps.size() >= 1) {
			return Optional.of(eps.get(0));
		} else { 
			return Optional.absent();
		}
		
	}

	public Optional<ExtensionPoint> getDataQuantityHealthCheckExtensionPoint() {
		return this.getExtensionPoint(ExtensionPointType.ParameterSetDataQuantityHealthCheck);
	}
	
	public DataQuantityHealthCheckParameters getDataQuantityHealthCheckParameters() {

		DataQuantityHealthCheckParameters theBean = new DataQuantityHealthCheckParameters();
		Object[] params = { theBean };

		if (hasExtensionPoint(ExtensionPointType.ParameterSetDataQuantityHealthCheck)) {
			try {
				theBean = (DataQuantityHealthCheckParameters) this.eval(ExtensionPointType.ParameterSetDataQuantityHealthCheck, params);
			} catch (ScriptException e) {
				LOGGER.error("ParameterSetDataQuantityHealthCheck groovy threw", e);
			}
		}
		return theBean;
	}

	public DataPurgeParameters getDataPurgeParameters() {

		DataPurgeParameters theBean = new DataPurgeParameters();
		Object[] params = { theBean };

		if (hasExtensionPoint(ExtensionPointType.ParameterSetDataPurge)) {
			try {
				theBean = (DataPurgeParameters) this.eval(ExtensionPointType.ParameterSetDataPurge, params);
				if (theBean == null) {
					LOGGER.error("ExtensionPointType.ParameterSetDataPurge is returning null bean, using defaults");
					theBean = new DataPurgeParameters();
				}
			} catch (ScriptException e) {
				LOGGER.error("ParameterSetDataPurge groovy threw", e);
			}
		}
		return theBean;
	}

	public Optional<ExtensionPoint> getDataPurgeExtensionPoint() {
		return this.getExtensionPoint(ExtensionPointType.ParameterSetDataPurge);
	}


}
