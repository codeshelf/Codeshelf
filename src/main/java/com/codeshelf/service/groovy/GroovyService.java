/*
 * Copyright 2007 Bruce Fancher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeshelf.service.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bruce Fancher
 */
public abstract class GroovyService {
    public static final String CONTEXT_KEY = "ctx";

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Map<String, Object> bindings;
    private boolean launchAtStart;
    private Thread serverThread;

	private String	customScriptsLocation;

    public GroovyService() {
        super();
    }

    public GroovyService(final Map<String, Object> bindings) {
        this();
        this.bindings = bindings;
    }

    public void launchInBackground() {
        serverThread = new Thread() {
            @Override
            public void run() {
                try {
                    logger.debug("Launching groovy service background thread...");
                    launch();
                } catch (final Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        };

        serverThread.setDaemon(false);
        serverThread.start();
    }

    public abstract void launch();

    protected Binding createBinding() {
        final Binding binding = new Binding();

        if (bindings != null) {
            for (final Map.Entry<String, Object> nextBinding : bindings.entrySet()) {

                logger.debug("Added variable [{}] to groovy bindings", nextBinding.getKey());
                binding.setVariable(nextBinding.getKey(), nextBinding.getValue());
            }
        }

        loadCustomGroovyScriptsIntoClasspath(binding);

        return binding;
    }

    public void initialize() {
        if (launchAtStart) {
            launchInBackground();
        }
    }

    public void destroy() {
    }

    public void setBindings(final Map<String, Object> bindings) {
        this.bindings = bindings;
    }

    public boolean isLaunchAtStart() {
        return launchAtStart;
    }

    public void setLaunchAtStart(final boolean launchAtStart) {
        this.launchAtStart = launchAtStart;
    }

    public void setCustomScriptsLocation(final String customScriptsLocation) {
        this.customScriptsLocation = customScriptsLocation;
    }

    private final void loadCustomGroovyScriptsIntoClasspath(final Binding binding) {
    	if (this.customScriptsLocation != null) {
	    	final FilenameFilter filter = new FilenameFilter() {
	            @Override
	            public boolean accept(File dir, String name) {
	                return (name.endsWith("groovy"));
	            }
	        };
	
	        
	        try (GroovyClassLoader loader = new GroovyClassLoader(this.getClass().getClassLoader())) {
		        final File[] files = new File(this.customScriptsLocation).listFiles(filter);
		        for (final File file : files) {
		            try {
		                final Class<?> c = loader.parseClass(file);
		                
		                final String fileNameWithOutExt = FilenameUtils.removeExtension(file.getName());
		                
		                binding.setVariable(fileNameWithOutExt, c.newInstance());
		                logger.debug("Add custom groovy script [{}] to the binding", fileNameWithOutExt);
		            } catch (final Exception e) {
		                logger.error(e.getMessage(), e);
		            }
		        }
	        } catch (IOException e1) {
				//quiet
			} 
    	}
    }
}
