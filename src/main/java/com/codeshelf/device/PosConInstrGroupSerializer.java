package com.codeshelf.device;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.Validatable;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

public class PosConInstrGroupSerializer {
	/**
	 * Duplicated from LedCmdGroupSerializer
	 * 
	 * The encoding is JSON.
	 * 
	 * The format of the stream is:
	 * 
	 * 	public PosControllerInstr(final byte inPosition,
		final byte inReqQty,
		final byte inMinQty,
		final byte inMaxQty,
		final byte inFreq,
		final byte inDutyCycle) {

	 * [
	 * 		{
	 * 			ctrl:		"controller mac addr",
	 * 			pos:		"position number",
	 * 			quantity:	"quantity",
	 * 			min:		"min quantity"
	 * 			max:		"max quantity"
	 * 			frequency:	"blinking frequency"
	 * 			brightness:	"brightness"
	 * 		}
	 * ]
	 * 
	 * @author jeffw
	 *
	 */

	private static final Logger	LOGGER	= LoggerFactory.getLogger(PosConInstrGroupSerializer.class);
	
	// Don't expose a constructor.
	private PosConInstrGroupSerializer() {}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public static String serializePosConCmdString(final List<PosConCmdGroup> inPosConCmdGroupList) {
		String result = "";

		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		result = gson.toJson(inPosConCmdGroupList);

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCmdString
	 * @return
	 */
	public static List<PosConCmdGroup> deserializePosConCmdString(final String inCmdString) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(inCmdString), "posConCmdString cannot be null");
		List<PosConCmdGroup> result = new ArrayList<PosConCmdGroup>();

		Gson mGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		Type collectionType = new TypeToken<Collection<PosConCmdGroup>>() {
		}.getType();
		result = mGson.fromJson(inCmdString, collectionType);

		return result;
	}
	
	// Just a utility checker.
	public static boolean verifyPosConCmdGroupList(final List<PosConCmdGroup> inPosConCmdGroupList){
		boolean allOk = true;
		for (PosConCmdGroup posConCmdGroup : inPosConCmdGroupList) {
			ErrorResponse errors = new ErrorResponse();
			if (!posConCmdGroup.isValid(errors)){
				allOk = false;
			}
			List<String> errorList = errors.getErrors();
			for (String error : errorList) {
				LOGGER.error(error);
			}
		}
		return allOk;
	}
	
	@JsonAutoDetect(getterVisibility=Visibility.PUBLIC_ONLY, fieldVisibility=Visibility.NONE)
	public static class PosConCmdGroup implements Validatable{		
		@Accessors(prefix = "m")
		@Getter @Setter @Expose
		@SerializedName(value = "controllerId")
		private String			mControllerId;

		@Accessors(prefix = "m")
		@Getter @Setter @Expose
		@SerializedName(value = "posNum")
		private Byte			mPosNum;

		@Accessors(prefix = "m")
		@Getter @Setter @Expose
		@SerializedName(value = "quantity")
		private Byte			mQuantity;

		@Accessors(prefix = "m")
		@Getter @Setter @Expose
		@SerializedName(value = "min")
		private Byte			mMin;

		@Accessors(prefix = "m")
		@Getter @Setter @Expose
		@SerializedName(value = "max")
		private Byte			mMax;

		@Accessors(prefix = "m")
		@Getter @Setter @Expose
		@SerializedName(value = "brightness")
		private Brightness		mBrightness = Brightness.BRIGHT;

		@Accessors(prefix = "m")
		@Getter @Setter @Expose
		@SerializedName(value = "frequency")
		private Frequency		mFrequency = Frequency.SOLID;
		
		@Accessors(prefix = "m")
		@Getter @Setter @Expose
		@SerializedName(value = "remove")
		private String mRemove;
		
		@Accessors(prefix = "m")
		@Getter
		private boolean mRemoveAll = false;
		
		@Accessors(prefix = "m")
		@Getter
		private List<Byte> mRemovePos = new ArrayList<Byte>();
		
		private String mRemoveError = null;


		public PosConCmdGroup(){}
		
		public PosConCmdGroup(String inController, Byte posNum, Byte quantity, Byte min, Byte max, Brightness brightness, Frequency frequency, String remove) {
			mControllerId = inController;
			mPosNum = posNum;
			mQuantity = quantity;
			mMin = min;
			mMax = max;
			mBrightness = brightness;
			mFrequency = frequency;
			mRemove = remove;
		}

		public void processRemovedField(){
			if (mRemove == null) {return;}
			if ("all".equalsIgnoreCase(mRemove)){
				mRemoveAll = true;
				return;
			}
			try {
				mRemovePos.clear();
				String[] removePositionsStr = mRemove.split(",");
				for (String positionSrt : removePositionsStr) {
					mRemovePos.add(Byte.parseByte(positionSrt));
				}
			} catch (Exception e) {
				mRemoveError = "Could not process posConCommands.remove";
				LOGGER.error("posConCommands.remove expcts \"all\" or a list of Byte position values");
				LOGGER.error(mRemoveError + " " + e);
			}
		}
		
		public int compareTo(PosConCmdGroup anotherPosConGroup) {
			return Byte.compare(getPosNum(), anotherPosConGroup.getPosNum());
		}
		
		@Override
		public boolean isValid(ErrorResponse errors) {
			boolean valid = true;
			processRemovedField();
			if (mControllerId == null) {
				errors.addErrorMissingBodyParam("posConCommands.ctrl");
				valid = false;
			}
			if (mRemoveError != null) {
				errors.addError(mRemoveError);
				valid = false;
			}
			if (!mRemoveAll && mRemovePos.isEmpty()) {
				if (mPosNum == null) {
					errors.addErrorMissingBodyParam("posConCommands.pos");
					valid = false;
				}
				if (mQuantity == null) {
					errors.addErrorMissingBodyParam("posConCommands.quantity");
					valid = false;
				} else if (mQuantity < 0) {
					errors.addError("provide a positive posConCommands.quantity");
					valid = false;
				}
			}
			return valid;
		}
		
		public void fillMinMax() {
			if (mMin == null) {mMin = mQuantity;}
			if (mMax == null) {mMax = mQuantity;}
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