package com.codeshelf.service;

import groovy.lang.GroovyRuntimeException;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.codeshelf.metrics.EdiFreeSpaceHealthCheckParamaters;
import com.codeshelf.model.DataPurgeParameters;
import com.codeshelf.model.domain.ExtensionPoint;
import com.codeshelf.model.domain.Facility;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class ExtensionPointEngine {

	private static final Logger	LOGGER				= LoggerFactory.getLogger(ExtensionPointEngine.class);
	private static final HashMap<UUID, ExtensionPointEngine> facilityEngines = new HashMap<>();
	
	ScriptEngine				engine;

	HashSet<ExtensionPointType>	activeExtensions	= new HashSet<ExtensionPointType>();

	@Getter
	private ArrayList<String>	failedExtensions	= Lists.newArrayList();

	@Getter
	private Facility	facility;

	public ExtensionPointEngine(Facility facility) throws ScriptException {
		initEngine();
		loadAllExtensionPoints(facility);
		this.facility = facility;
	}

	private void initEngine() throws ScriptException {
		ScriptEngineManager factory = new ScriptEngineManager();
		engine = factory.getEngineByName("groovy");
		if (engine == null) {
			throw new ScriptException("groovy engine not setup in ExtensionPointService.  Need to run gradle eclipse?");
		}
	}

	
	/**
	 * Record that the extension point is valid and active or throw
	 */
	private void activateExtensionPoint(ExtensionPoint ep) throws ScriptException {
		ExtensionPointType extp = ep.getType();
		this.activeExtensions.add(extp);
	}

	private void inactivateExtensionPoint(ExtensionPoint ep) throws ScriptException {
		LOGGER.info("Inactivating extension point " + ep.getType());
		ExtensionPointType extp = ep.getType();
		this.activeExtensions.remove(extp);
		//TODO may consider building a noop version for each type that can be evaluated into the engine
	}

	public boolean hasActiveExtensionPoint(ExtensionPointType extp) {
		return this.activeExtensions.contains(extp);
	}

	private List<ExtensionPoint> loadAllExtensionPoints(Facility facility) throws ScriptException {
		List<ExtensionPoint> eps = ExtensionPoint.staticGetDao().findByParent(facility);
		this.activeExtensions.clear();
		failedExtensions.clear();
		for (ExtensionPoint ep : eps) {
			try {
				if (ep.isActive()) {
					try {
						loadExtensionPoint(ep);
						this.activateExtensionPoint(ep);
					} catch (ScriptException e) {
						failedExtensions.add(ep + " " + e);
						Throwable cause = e.getCause();
						if (cause instanceof GroovyRuntimeException) {
							LOGGER.warn("Inactivating invalid extension " + ep.getDomainId(), e);
							ep.setActive(false);
							ExtensionPoint.staticGetDao().store(ep);
						} else {
							throw e;
						}
					}
	
					
				} else { 
					LOGGER.info("Skipping inactive extension point " + ep.getType());
				}
			} catch(Exception e) {
				LOGGER.error("Extension point {} for facility {} could not be loaded skipping", ep.getType(), facility);
			}
		}
		return eps;
	}

	private void loadExtensionPoint(ExtensionPoint ep) throws ScriptException {
		LOGGER.info("Loading extension point " + ep.getType());
		engine.eval(ep.getScript());
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

	public static ExtensionPointEngine getInstance(Facility facility) throws ScriptException {
		synchronized(facilityEngines) {
			ExtensionPointEngine engine = facilityEngines.get(facility.getPersistentId());
			if (engine == null) {
				engine = new ExtensionPointEngine(facility);
				facilityEngines.put(facility.getPersistentId(), engine);
			}
			return engine;
		}
	}

	// Methods to get the parameter beans	
	//+++++++++++++++++++++++++++++++++++++++

	public Optional<ExtensionPoint> getExtensionPoint(ExtensionPointType type) {
		@SuppressWarnings("unchecked")
		List<ExtensionPoint> eps = ExtensionPoint.staticGetDao().createCriteria()
				.add(Restrictions.eq("type", type))
				.add(Restrictions.eq("parent.persistentId", facility.getPersistentId()))
				.list();
		if (eps.size() > 1) {
			LOGGER.warn("Fould multiple extension points for type {} in faciltiy {}", type, facility);
		} else if (eps.size() < 1) {
			LOGGER.warn("Fould multiple extension points for type {} in faciltiy {}", type, facility);
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

		if (hasActiveExtensionPoint(ExtensionPointType.ParameterSetDataQuantityHealthCheck)) {
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

		if (hasActiveExtensionPoint(ExtensionPointType.ParameterSetDataPurge)) {
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


	public EdiFreeSpaceHealthCheckParamaters getEdiFreeSpaceParameters() {

		EdiFreeSpaceHealthCheckParamaters theBean = new EdiFreeSpaceHealthCheckParamaters();
		Object[] params = { theBean };

		if (hasActiveExtensionPoint(ExtensionPointType.ParameterEdiFreeSpaceHealthCheck)) {
			try {
				theBean = (EdiFreeSpaceHealthCheckParamaters) this.eval(ExtensionPointType.ParameterEdiFreeSpaceHealthCheck, params);
				if (theBean == null) {
					LOGGER.error("ExtensionPointType.EdiFreeSpaceHealthCheckParamaters is returning null bean, using defaults");
					theBean = new EdiFreeSpaceHealthCheckParamaters();
				}
			} catch (ScriptException e) {
				LOGGER.error("EdiFreeSpaceHealthCheckParamaters groovy threw", e);
			}
		}
		return theBean;
	}
	
	public Optional<ExtensionPoint> getEdiFreeSpaceExtensionPoint() {
		return this.getExtensionPoint(ExtensionPointType.ParameterEdiFreeSpaceHealthCheck);
	}

	public List<ExtensionPoint> getAllExtensions() {
		@SuppressWarnings("unchecked")
		List<ExtensionPoint> eps = ExtensionPoint.staticGetDao().createCriteria()
				.add(Restrictions.eq("parent.persistentId", facility.getPersistentId()))
				.list();
		return eps;
	}

	public ExtensionPoint findById(UUID extensionPointId) {
		ExtensionPoint point = ExtensionPoint.staticGetDao().findByPersistentId(extensionPointId);
		return point;
	}
	
	public ExtensionPoint create(ExtensionPointType typeEnum) throws ScriptException {
		ExtensionPoint point = new ExtensionPoint(facility.reload(), typeEnum);
		store(point);
		return point;
	}

	public ExtensionPoint create(ExtensionPoint point) throws ScriptException {
		store(point);
		return point;
	}

	
	public ExtensionPoint update(ExtensionPoint point) throws ScriptException {
		store(point);
		return point;
	}

	public void delete(ExtensionPoint point) {
		ExtensionPoint.staticGetDao().delete(point);
	}

	private ExtensionPoint store(ExtensionPoint point) throws ScriptException {
		loadExtensionPoint(point);
		if (point.isActive()) {
			this.activateExtensionPoint(point);
		} else {
			this.inactivateExtensionPoint(point);
		}
		ExtensionPoint.staticGetDao().store(point);
		return point;
	}

}
