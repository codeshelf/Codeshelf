package com.codeshelf.ws.protocol.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/* Message Format:
"ObjectPropertiesRequest": {
	"messageId":"707b8570-87d3-11e4-8a3f-0c4de99ad9d1",
	"className":"Organization",
	"persistentId":"215410fa-42e9-4b1c-947c-a0b183cf68f2"
}
*/
@ToString
public class ObjectPropertiesRequest extends RequestABC {

	@Getter @Setter
	String className;
	
	@Getter @Setter
	String persistentId;
	
	// This really needs a properties list, but not now. The list view asks for the properties it wants returned.
	// @Getter @Setter
	// List<String> propertyNames;
}
