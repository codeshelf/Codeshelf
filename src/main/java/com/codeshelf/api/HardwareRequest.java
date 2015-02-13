package com.codeshelf.api;

import java.util.List;

import lombok.Getter;

import com.codeshelf.flyweight.command.ColorEnum;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonAutoDetect(getterVisibility=Visibility.PUBLIC_ONLY, fieldVisibility=Visibility.NONE)
public class HardwareRequest implements Validatable{
	@Getter
	private String controller;
	@Getter
	private short channel;
	@Getter
	private int lightDuration;	
	@Getter
	private List<LightCommand> lights;
	
	public boolean isValid(ErrorResponse errors) {
		boolean valid = true;
		if (controller == null) {
			errors.addErrorMissingBodyParam("lights.controller");
			valid = false;
		}
		if (channel <= 0) {
			errors.addError("Provide positive controller lights.channel");
			valid = false;
		}
		if (lightDuration <= 0) {
			errors.addError("Provide positive duration for the Light commands");
			valid = false;
		}
		if (lights != null) {
			for (LightCommand light : lights) {
				if (!light.isValid(errors)){
					valid = false;
				}
			}
		}
		return valid;
				
	}
	
	@JsonAutoDetect(getterVisibility=Visibility.PUBLIC_ONLY, fieldVisibility=Visibility.NONE)
	static public class LightCommand implements Validatable{
		public LightCommand() {}
		@Getter
		private short position;
		@Getter
		private ColorEnum color;
		
		@Override
		public boolean isValid(ErrorResponse errors) {
			boolean valid = true;

			if (color == null) {
				errors.addErrorMissingBodyParam("lights.color");
				valid = false;
			}
			return valid;
		}
	}
}