package com.gadgetworks.codeshelf.filter;

import java.util.Set;
import java.util.UUID;

import lombok.Getter;

import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.WirelessDeviceABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.NetworkStatusResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

public class NetworkChangeListener implements ObjectEventListener {

	@Getter
	String id;
	
	@Getter
	private UUID networkId;

	public NetworkChangeListener(UUID networkId,String id) {
		this.networkId=networkId;
		this.id=id;
	}
	
	@Override
	public ResponseABC processObjectAdd(IDomainObject inDomainObject) {
		return onAnythingChanged(inDomainObject);
	}

	@Override
	public ResponseABC processObjectUpdate(IDomainObject inDomainObject, Set<String> inChangedProperties) {
		return onAnythingChanged(inDomainObject);
	}

	@Override
	public ResponseABC processObjectDelete(IDomainObject inDomainObject) {
		return onAnythingChanged(inDomainObject);
	}
	
	private ResponseABC onAnythingChanged(IDomainObject inDomainObject) {
		if(inDomainObject instanceof WirelessDeviceABC) {
			WirelessDeviceABC device = (WirelessDeviceABC)inDomainObject;
			CodeshelfNetwork network = device.getParent();
			
			// if the object changed within this network, generate a new network status response
			if(network.getPersistentId().equals(this.networkId)) {
				return NetworkStatusResponse.generate(networkId);
			}
		}
		return null;
	}
	
}
