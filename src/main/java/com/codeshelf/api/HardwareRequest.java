package com.codeshelf.api;

import java.util.List;

import lombok.Getter;

import com.codeshelf.flyweight.command.ColorEnum;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonAutoDetect(getterVisibility=Visibility.PUBLIC_ONLY, fieldVisibility=Visibility.NONE)
public class HardwareRequest implements Validatable{
	@Getter
	private String lightController;
	@Getter
	private short lightChannel;
	@Getter
	private int lightDuration;	
	@Getter
	private List<LightRequest> lights;
	@Getter
	private List<CheDisplayRequest> cheMessages;
	
	public boolean isValid(ErrorResponse errors) {
		boolean valid = true;
		if (lightController == null) {
			errors.addErrorMissingBodyParam("lightController");
			valid = false;
		}
		if (lightChannel <= 0) {
			errors.addError("Provide positive controller lightChannel");
			valid = false;
		}
		if (lightDuration <= 0) {
			errors.addError("Provide positive duration for the Light commands");
			valid = false;
		}
		if (cheMessages != null) {
			for (CheDisplayRequest che : cheMessages) {
				if (!che.isValid(errors)){
					valid = false;
				}
			}
		}
		if (lights != null) {
			for (LightRequest light : lights) {
				if (!light.isValid(errors)){
					valid = false;
				}
			}
		}
		return valid;
				
	}
	
	@JsonAutoDetect(getterVisibility=Visibility.PUBLIC_ONLY, fieldVisibility=Visibility.NONE)
	static public class LightRequest implements Validatable{
		@Getter
		private Short position;
		@Getter
		private ColorEnum color;
		
		public LightRequest() {}
		
		@Override
		public boolean isValid(ErrorResponse errors) {
			boolean valid = true;

			if (color == null) {
				errors.addErrorMissingBodyParam("lights.color");
				valid = false;
			}
			if (position == null || position < 0) {
				errors.addErrorMissingBodyParam("Provide non-negative lights.position");
				valid = false;
			}

			return valid;
		}
	}
	
	@JsonAutoDetect(getterVisibility=Visibility.PUBLIC_ONLY, fieldVisibility=Visibility.NONE)
	static public class CheDisplayRequest implements Validatable{
		@Getter
		private String che;
		@Getter
		private String line1, line2, line3, line4;
		
		public CheDisplayRequest() {}
		
		@Override
		public boolean isValid(ErrorResponse errors) {
			boolean valid = true;

			if (che == null) {
				errors.addErrorMissingBodyParam("cheMessages.che");
				valid = false;
			}
			return valid;
		}
	}

}