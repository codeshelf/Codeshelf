package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class AssociateRemoteCheResponse extends ResponseABC {
	
	// private static final Logger		LOGGER	= LoggerFactory.getLogger(GetPutWallInstructionResponse.class);
	// had some temporary debugging. Not needed now.
	
	@Getter
	@Setter
	String							networkGuid; // Guid of the (remote) CHE that is associating.

	@Getter
	@Setter
	String							cheName; // Name corresponding to the guid above

	@Getter
	@Setter
	String							associatedCheGuid; // Guid of the cart CHE that remote is associating to

	@Getter
	@Setter
	String							associatedCheName; // Name of the cart CHE that remote is associating to


	@Override
	public String getDeviceIdentifier() {
		return getNetworkGuid();
	}
	
}
