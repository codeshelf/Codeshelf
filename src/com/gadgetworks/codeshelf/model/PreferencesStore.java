/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: PreferencesStore.java,v 1.8 2012/03/22 07:35:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;

import com.gadgetworks.codeshelf.controller.ControllerABC;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class PreferencesStore {

	private static final Log				LOGGER	= LogFactory.getLog(PreferencesStore.class);

	private static PreferencesStore			mPreferencesStore;
	private Map<String, PersistentProperty>	mChangedProperties;
	private static IGenericDao<PersistentProperty>			mPersistentPropertyDao;

	private PreferencesStore(IGenericDao<PersistentProperty> inPersistentPropertyDao) {
		mChangedProperties = new HashMap<String, PersistentProperty>();
		mPersistentPropertyDao = inPersistentPropertyDao;
	}

	public static void initPreferencesStore(IGenericDao<PersistentProperty> inPersistentPropertyDao) {
		// Setup the preferences store singleton.
		mPreferencesStore = new PreferencesStore(inPersistentPropertyDao);

		initPreference(PersistentProperty.SHOW_CONSOLE_PREF, "Show the console at startup", String.valueOf(false));
		initPreference(PersistentProperty.SHOW_CONNECTION_DEBUG_PREF, "Show a connection debug dialog", String.valueOf(false));
		initPreference(PersistentProperty.FORCE_CHANNEL, "Preferred wireless channel", ControllerABC.NO_PREFERRED_CHANNEL_TEXT);
		initPreference(PersistentProperty.GENERAL_INTF_LOG_LEVEL, "Preferred general log level", Level.INFO.toString());
		initPreference(PersistentProperty.GATEWAY_INTF_LOG_LEVEL, "Preferred gateway log level", Level.INFO.toString());
		initPreference(PersistentProperty.ACTIVEMQ_RUN, "Run ActiveMQ", String.valueOf(false));
		initPreference(PersistentProperty.ACTIVEMQ_USERID, "ActiveMQ User Id", "");
		initPreference(PersistentProperty.ACTIVEMQ_PASSWORD, "ActiveMQ Password", "");
		initPreference(PersistentProperty.ACTIVEMQ_STOMP_PORTNUM, "ActiveMQ STOMP Portnum", "61613");
		initPreference(PersistentProperty.ACTIVEMQ_JMS_PORTNUM, "ActiveMQ JMS Portnum", "61616");
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public static PreferencesStore getPreferencesStore(IGenericDao<PersistentProperty> inPersistentPropertyDao) {
		if (mPreferencesStore == null) {
			initPreferencesStore(inPersistentPropertyDao);
		}
		return mPreferencesStore;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inPropertyID
	 *  @param inDescription
	 *  @param inDefaultValue
	 */
	private static void initPreference(String inPropertyID, String inDescription, String inDefaultValue) {
		boolean shouldUpdate = false;

		// Find the property in the DB.
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);

		// If the property doesn't exist then create it.
		if (property == null) {
			property = new PersistentProperty();
			property.setId(inPropertyID);
			property.setCurrentValueAsStr(inDefaultValue);
			property.setDefaultValueAsStr(inDefaultValue);
			shouldUpdate = true;
		}

		// If the stored default value doesn't match then change it.
		if (!property.getDefaultValueAsStr().equals(inDefaultValue)) {
			property.setDefaultValueAsStr(inDefaultValue);
			shouldUpdate = true;
		}

		// If the property changed then we need to persist the change.
		if (shouldUpdate) {
			try {
				mPersistentPropertyDao.store(property);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPersistentPreferenceStore#save()
	 */
	public void save() throws IOException {
		for (PersistentProperty property : mChangedProperties.values()) {
			try {
				mPersistentPropertyDao.store(property);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
		mChangedProperties.clear();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#contains(java.lang.String)
	 */
	public boolean contains(String inPropertyID) {
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		return (property != null);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#getBoolean(java.lang.String)
	 */
	public boolean getBoolean(String inPropertyID) {
		boolean result = false;
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null)
			result = property.getCurrentValueAsBoolean();
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#getDefaultBoolean(java.lang.String)
	 */
	public boolean getDefaultBoolean(String inPropertyID) {
		boolean result = false;
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null)
			result = property.getDefaultValueAsBoolean();
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#getDefaultDouble(java.lang.String)
	 */
	public double getDefaultDouble(String inPropertyID) {
		double result = 0;
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null)
			result = property.getDefaultValueAsDouble();
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#getDefaultFloat(java.lang.String)
	 */
	public float getDefaultFloat(String inPropertyID) {
		float result = 0;
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null)
			result = property.getDefaultValueAsFloat();
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#getDefaultInt(java.lang.String)
	 */
	public int getDefaultInt(String inPropertyID) {
		int result = 0;
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null)
			result = property.getDefaultValueAsInt();
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#getDefaultLong(java.lang.String)
	 */
	public long getDefaultLong(String inPropertyID) {
		long result = 0;
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null)
			result = property.getDefaultValueAsLong();
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#getDefaultString(java.lang.String)
	 */
	public String getDefaultString(String inPropertyID) {
		String result = "";
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null)
			result = property.getDefaultValueAsStr();
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#getDouble(java.lang.String)
	 */
	public double getDouble(String inPropertyID) {
		double result = 0;
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null)
			result = property.getCurrentValueAsDouble();
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#getFloat(java.lang.String)
	 */
	public float getFloat(String inPropertyID) {
		float result = 0;
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null)
			result = property.getCurrentValueAsFloat();
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#getInt(java.lang.String)
	 */
	public int getInt(String inPropertyID) {
		int result = 0;
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null)
			result = property.getCurrentValueAsInt();
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#getLong(java.lang.String)
	 */
	public long getLong(String inPropertyID) {
		long result = 0;
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null)
			result = property.getCurrentValueAsLong();
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#getString(java.lang.String)
	 */
	public String getString(String inPropertyID) {
		String result = "";
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null)
			result = property.getCurrentValueAsStr();
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#isDefault(java.lang.String)
	 */
	public boolean isDefault(String inPropertyID) {
		boolean result = true;
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null)
			result = (property.getCurrentValueAsStr().equals(property.getDefaultValueAsStr()));
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#needsSaving()
	 */
	public boolean needsSaving() {
		return (mChangedProperties.size() > 0);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#putValue(java.lang.String, java.lang.String)
	 */
	public void putValue(String inPropertyID, String inValue) {
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null) {
			property.setCurrentValueAsStr(inValue);
			mChangedProperties.put(property.getId(), property);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#setDefault(java.lang.String, double)
	 */
	public void setDefault(String inPropertyID, double inValue) {
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null) {
			property.setDefaultValueAsDouble(inValue);
			mChangedProperties.put(property.getId(), property);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#setDefault(java.lang.String, float)
	 */
	public void setDefault(String inPropertyID, float inValue) {
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null) {
			property.setDefaultValueAsFloat(inValue);
			mChangedProperties.put(property.getId(), property);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#setDefault(java.lang.String, int)
	 */
	public void setDefault(String inPropertyID, int inValue) {
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null) {
			property.setCurrentValueAsInt(inValue);
			mChangedProperties.put(property.getId(), property);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#setDefault(java.lang.String, long)
	 */
	public void setDefault(String inPropertyID, long inValue) {
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null) {
			property.setDefaultValueAsLong(inValue);
			mChangedProperties.put(property.getId(), property);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#setDefault(java.lang.String, java.lang.String)
	 */
	public void setDefault(String inPropertyID, String inValue) {
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null) {
			property.setDefaultValueAsStr(inValue);
			mChangedProperties.put(property.getId(), property);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#setDefault(java.lang.String, boolean)
	 */
	public void setDefault(String inPropertyID, boolean inValue) {
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null) {
			property.setDefaultValueAsBoolean(inValue);
			mChangedProperties.put(property.getId(), property);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#setToDefault(java.lang.String)
	 */
	public void setToDefault(String inPropertyID) {
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null) {
			property.setCurrentValueAsStr(property.getDefaultValueAsStr());
			mChangedProperties.put(property.getId(), property);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#setValue(java.lang.String, double)
	 */
	public void setValue(String inPropertyID, double inValue) {
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null) {
			property.setCurrentValueAsDouble(inValue);
			mChangedProperties.put(property.getId(), property);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#setValue(java.lang.String, float)
	 */
	public void setValue(String inPropertyID, float inValue) {
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null) {
			property.setCurrentValueAsFloat(inValue);
			mChangedProperties.put(property.getId(), property);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#setValue(java.lang.String, int)
	 */
	public void setValue(String inPropertyID, int inValue) {
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null) {
			property.setCurrentValueAsInt(inValue);
			mChangedProperties.put(property.getId(), property);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#setValue(java.lang.String, long)
	 */
	public void setValue(String inPropertyID, long inValue) {
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null) {
			property.setCurrentValueAsLong(inValue);
			mChangedProperties.put(property.getId(), property);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#setValue(java.lang.String, java.lang.String)
	 */
	public void setValue(String inPropertyID, String inValue) {
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null) {
			property.setCurrentValueAsStr(inValue);
			mChangedProperties.put(property.getId(), property);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceStore#setValue(java.lang.String, boolean)
	 */
	public void setValue(String inPropertyID, boolean inValue) {
		PersistentProperty property = mPersistentPropertyDao.findByDomainId(inPropertyID);
		if (property != null) {
			property.setCurrentValueAsBoolean(inValue);
			mChangedProperties.put(property.getId(), property);
		}
	}

}
