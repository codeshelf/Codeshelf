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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * @author Bruce Fancher
 */
public class GroovyShellService extends GroovyService {

    private ServerSocket serverSocket;
    private int socket;
    private final List<String> defaultScripts;

    private final List<GroovyShellThread> threads = new ArrayList<GroovyShellThread>();

    public GroovyShellService() {
        this(8185);
    }

    public GroovyShellService(final int socket) {
    	this(ImmutableMap.<String, Object>of(), ImmutableList.<String>of(), socket);
    }

    public GroovyShellService(final Map<String, Object> bindings, List<String> defaultScripts, final int socket) {
        super(bindings);
        this.socket = socket;
        this.defaultScripts = defaultScripts;
    }

    @Override
    public void launch() {
        logger.debug("Launching groovy shell service...");

        try {
            serverSocket = new ServerSocket(socket);
            logger.debug("Opened server socket {} on port {}",
                    serverSocket, this.socket);

            while (true) {
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                    logger.debug("Received client socket request {} ", clientSocket);
                } catch (final IOException e) {
                    logger.error(e.getMessage(), e);
                    return;
                }

                final GroovyShellThread clientThread = new GroovyShellThread(clientSocket, createBinding(), defaultScripts);
                threads.add(clientThread);
                
                logger.debug("Created groovy shell thread based on client socket request {}. Starting...",
                        clientSocket);
                clientThread.start();
            }
        } catch (final IOException e) {
            logger.error(e.getMessage(), e);
            return;
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (final IOException e) {
                logger.warn(e.getMessage(), e);
            }
            logger.info("Closed groovy shell service server socket connection");
        }
    }

    @Override
    public void destroy() {
        logger.info("Closing serverSocket: {}" + serverSocket);
        try {
            serverSocket.close();
            for (final GroovyShellThread nextThread : threads) {
                logger.info("Closing socket: {}", nextThread.getSocket());
                nextThread.getSocket().close();
            }
        } catch (final IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void setSocket(final int socket) {
        this.socket = socket;
    }
}
