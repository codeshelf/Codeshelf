package com.codeshelf.api;

import java.util.List;

import lombok.Getter;

import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.flyweight.command.ColorEnum;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonAutoDetect(getterVisibility=Visibility.PUBLIC_ONLY, fieldVisibility=Visibility.NONE)
public class HardwareRequest implements Validatable{
	@Getter
	private String lightController;
	@Getter
	private Short lightChannel;
	@Getter
	private Integer lightDuration;	
	@Getter
	private List<LightRequest> lights;
	@Getter
	private List<CheDisplayRequest> cheMessages;
	@Getter
	private List<PosConCommand> posConCommands;

	
	public boolean isValid(ErrorResponse errors) {
		boolean valid = true;
		if (lightController != null || lightChannel != null || lightDuration != null || lights != null){
			if (lightController == null) {
				errors.addErrorMissingBodyParam("lightController");
				valid = false;
			}
			if (lightChannel == null || lightChannel <= 0) {
				errors.addError("Provide positive controller lightChannel");
				valid = false;
			}
			if (lightDuration == null || lightDuration <= 0) {
				errors.addError("Provide positive duration for the Light commands");
				valid = false;
			}
			if (lights != null) {
				for (LightRequest light : lights) {
					if (!light.isValid(errors)){
						valid = false;
					}
				}
			}
		}
		if (cheMessages != null) {
			for (CheDisplayRequest che : cheMessages) {
				if (!che.isValid(errors)){
					valid = false;
				}
			}
		}
		if (posConCommands != null) {
			for (PosConCommand posCon : posConCommands) {
				if (!posCon.isValid(errors)){
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
	
	@JsonAutoDetect(getterVisibility=Visibility.PUBLIC_ONLY, fieldVisibility=Visibility.NONE)
	static public class PosConCommand implements Validatable{
		@Getter
		private String controller;
		@Getter
		private Byte position, quantity, min, max;
		@Getter
		private Brightness brightness = Brightness.BRIGHT;
		@Getter
		private Frequency frequency = Frequency.SOLID;
		
		public PosConCommand() {}
		
		@Override
		public boolean isValid(ErrorResponse errors) {
			boolean valid = true;
			if (controller == null) {
				errors.addErrorMissingBodyParam("posConCommands.controller");
				valid = false;
			}
			if (position == null) {
				errors.addErrorMissingBodyParam("posConCommands.position");
				valid = false;
			}
			if (quantity == null) {
				errors.addErrorMissingBodyParam("posConCommands.quantity");
				valid = false;
			}
			return valid;
		}
		
		public void fillMinMax() {
			if (min == null) {min = quantity;}
			if (max == null) {max = quantity;}
		}
		
		public enum Brightness {
			BRIGHT, MEDIUM, DIM;
			
			public Byte toByte(){
				if (this == BRIGHT) {
					return PosControllerInstr.BRIGHT_DUTYCYCLE;
				} else if (this == MEDIUM){
					return PosControllerInstr.MED_DUTYCYCLE;
				} else if (this == DIM){
					return PosControllerInstr.DIM_DUTYCYCLE;
				}
				return null;
			}
		}
		
		public enum Frequency {
			SOLID, BLINK;
			
			public Byte toByte(){
				if (this == SOLID) {
					return PosControllerInstr.SOLID_FREQ;
				} else if (this == BLINK){
					return PosControllerInstr.BLINK_FREQ;
				}
				return null;
			}
		}
	}
}