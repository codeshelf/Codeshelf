package com.codeshelf.ws.protocol.message;

import java.util.HashMap;
import java.util.List;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.ContainerUse;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CheStatusMessage extends MessageABC {
	
	@Getter @Setter
	String cheId;
	
	@Getter
	HashMap<String, Integer> containerPositions;

	@Enumerated(value = EnumType.STRING)
	@Getter @Setter
	@JsonProperty
	ColorEnum color;
	
	public CheStatusMessage() {	
	}
	
	public CheStatusMessage(Che che) {
		this.cheId = che.getPersistentId().toString();
		this.color = che.getColor();
		List<ContainerUse> uses = che.getUses();
		if (uses!=null) {
			containerPositions = new HashMap<String,Integer>();
			for (ContainerUse use : uses) {
				this.containerPositions.put(use.getContainerName(), use.getPosconIndex());	
			}
		}
	}
}
