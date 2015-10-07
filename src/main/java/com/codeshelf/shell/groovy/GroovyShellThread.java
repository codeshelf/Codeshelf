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

package com.codeshelf.shell.groovy;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * @author Bruce Fancher
 */
public final class GroovyShellThread extends Thread {
    public static final String OUT_KEY = "out";

    private final Socket socket;
    private final Binding binding;
    private final List<String> defaultScripts;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public GroovyShellThread(final Socket socket, final Binding binding, final List<String> defaultScripts) {
        super();
        this.socket = socket;
        this.binding = binding;
        this.defaultScripts = defaultScripts;
    }

    @Override
    public void run() {
        OutputStream out = null;
        InputStream in = null;

        try {
        	final TelnetStream telnet = new TelnetStream(socket.getInputStream(), socket.getOutputStream());

            telnet.getOutputStream().writeWONT(34); // linemode
            telnet.getOutputStream().writeWILL(1); // echo
            telnet.getOutputStream().writeWILL(3); // supress go ahead
            out = telnet.getOutputStream();
            in = telnet.getInputStream();
            logger.debug("Created socket IO streams...");
            
            binding.setVariable(OUT_KEY, out);
            logger.debug("Added output stream to binding collection as {}", OUT_KEY);
            
            final GroovyClassLoader loader = new GroovyClassLoader(this.getContextClassLoader());
            final IO io = new IO(in, out, out);
                        
            final Groovysh gsh = new Groovysh(loader, binding, io);
            gsh.getImports().addAll(ImmutableList.of("com.codeshelf.sim.worker.PickSimulator"));
    		try {
    			loadDefaultScripts(gsh);
    		} catch (Exception e) {
    			new PrintStream(io.errorStream, true, "utf8").println("Unable to load default scripts: "
    				+ e.getClass().getName() + ": " + e.getMessage());
    		}
            
            try {
                logger.debug("Launching groovy interactive shell");
                gsh.run("");
            } catch (final Exception e) {
                logger.error(e.getMessage(), e);
            }
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (out != null) {
                IOUtils.closeQuietly(out);
            }

            if (in != null) {
                IOUtils.closeQuietly(in);
            }

            if (socket != null) {
                IOUtils.closeQuietly(this.socket);
            }
        }
    }

    public Socket getSocket() {
        return socket;
    }
    
    @SuppressWarnings({ "unchecked", "serial" })
	private void loadDefaultScripts(final Groovysh shell) {
		if (!defaultScripts.isEmpty()) {
			Closure<Groovysh> defaultResultHook = shell.getResultHook();

			try {
				// Set a "no-op closure so we don't get per-line value output when evaluating the default script
				shell.setResultHook(new Closure<Groovysh>(this) {
					@Override
					public Groovysh call(Object... args) {
						return shell;
					}
				});

				org.codehaus.groovy.tools.shell.Command cmd = shell.getRegistry().find("load");
				for (String script : defaultScripts) {
					cmd.execute(ImmutableList.of(script));
				}
			} finally {
				// Restoring original result hook
				shell.setResultHook(defaultResultHook);
			}
		}
	}
    
}
