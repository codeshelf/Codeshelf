package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;

public class LinkRemoteCheResponse extends DeviceResponseABC {
	
	// private static final Logger		LOGGER	= LoggerFactory.getLogger(GetPutWallInstructionResponse.class);
	// had some temporary debugging. Not needed now.
	
	@Getter
	@Setter
	String							cheName; // Name corresponding to the guid above

	@Getter
	@Setter
	String							linkedCheGuid; // Guid of the cart CHE that remote is associating to

	@Getter
	@Setter
	String							linkedCheName; // Name of the cart CHE that remote is associating to
}
