/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsWebSocketClient.java,v 1.2 2013/02/12 19:19:42 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.IUtil;
import com.gadgetworks.codeshelf.application.WirelessDeviceEventHandler;
import com.gadgetworks.codeshelf.controller.CodeShelfController;
import com.gadgetworks.codeshelf.controller.IController;
import com.gadgetworks.codeshelf.controller.IWirelessInterface;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice.IWirelessDeviceDao;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class CsWebSocketClient extends WebSocketClient implements IWebSocketClient {

	private static final Logger			LOGGER	= LoggerFactory.getLogger(CsWebSocketClient.class);

	private List<IController>			mControllerList;
	private WirelessDeviceEventHandler	mWirelessDeviceEventHandler;
	private ITypedDao<Organization>		mOrganizationDao;
	private ITypedDao<Facility>			mFacilityDao;
	private IWirelessDeviceDao			mWirelessDeviceDao;
	private ITypedDao<User>				mUserDao;
	private IUtil						mUtil;

	@Inject
	public CsWebSocketClient(@Named(WEBSOCKET_URI_PROPERTY) final String inUriStr,
		final IUtil inUtil,
		final WebSocketClient.WebSocketClientFactory inWebSocketClientFactory,
		final ITypedDao<Organization> inOrganizationDao,
		final ITypedDao<Facility> inFacilityDao,
		final IWirelessDeviceDao inWirelessDeviceDao,
		final ITypedDao<User> inUserDao) {
		super(URI.create(inUriStr));

		setWebSocketFactory(inWebSocketClientFactory);

		mOrganizationDao = inOrganizationDao;
		mFacilityDao = inFacilityDao;
		mWirelessDeviceDao = inWirelessDeviceDao;
		mWirelessDeviceDao = inWirelessDeviceDao;
		mUserDao = inUserDao;
		mUtil = inUtil;

		mControllerList = new ArrayList<IController>();
	}

	public final void start() {
		WebSocket.DEBUG = true;
		LOGGER.debug("Websocket start");
		try {
			connectBlocking();

			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				try {
					String line = reader.readLine();
					if (line.equals("close")) {
						close();
					} else {
						send(line);
					}
				} catch (NotYetConnectedException | IOException e) {
					LOGGER.error("", e);
				}
			}
		} catch (InterruptedException e) {
			LOGGER.error("", e);
		}
	}

	private void initControllers() {
		Collection<Organization> organizations = mOrganizationDao.getAll();
		for (Organization organization : organizations) {
			for (Facility facility : organization.getFacilities()) {

				List<IWirelessInterface> interfaceList = new ArrayList<IWirelessInterface>();
				// Create a CodeShelf interface for each CodeShelf network we have.
				for (CodeshelfNetwork network : facility.getNetworks()) {
//					SnapInterface snapInterface = new SnapInterface(network, mWirelessDeviceDao);
//					network.setWirelessInterface(snapInterface);
//					interfaceList.add(snapInterface);
				}

				mControllerList.add(new CodeShelfController(interfaceList, facility, mWirelessDeviceDao));
			}
		}

		mWirelessDeviceEventHandler = new WirelessDeviceEventHandler(mControllerList, mWirelessDeviceDao);

		// Start the controllers.
		LOGGER.info("Starting controllers");
		for (IController controller : mControllerList) {
			controller.startController();
		}
	}

	public final void stop() {
		close();

		// Shutdown the controllers
		for (IController controller : mControllerList) {
			controller.stopController();
		}
	}

	public final void onOpen(final ServerHandshake inHandshake) {
		LOGGER.debug("Websocket open");
	}

	public final void onClose(final int inCode, final String inReason, final boolean inRemote) {
		LOGGER.debug("Websocket close");
	}

	public final void onMessage(final String inMessage) {
		LOGGER.debug(inMessage);
	}

	public final void onError(final Exception inException) {

	}
}
