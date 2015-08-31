package com.codeshelf.device;

public enum DeviceRestartCauseEnum {
	
	INVALID(RestartCauseNum.INVALID, "INVALID"),
	USER_RESTART(RestartCauseNum.USER_RESTART, "USER_RESTART"),
	HARD_FAULT(RestartCauseNum.HARD_FAULT, "HARD_FAULT"),
	WATCHDOG_TIMEOUT(RestartCauseNum.WATCHDOG_TIMEOUT, "WATCHDOG_TIMER"),
	RESPONSE_TIMEOUT(RestartCauseNum.RESPONSE_TIMEOUT, "RESPONSE_TIMEOUT"),
	TXBUFF_TIMEOUT(RestartCauseNum.TXBUFF_TIMEOUT, "TXBUFF_TIMEOUT"),
	RXBUFF_TIMEOUT(RestartCauseNum.RXBUFF_TIMEOUT, "RXBUFF_TIMEOUT"),
	SMAC_ERROR(RestartCauseNum.SMAC_ERROR, "SMAC_ERROR"),
	OTHER_ERROR(RestartCauseNum.OTHER_ERROR, "OTHER_ERROR"),
	POWER_ON(RestartCauseNum.POWER_ON, "POWER_ON");
	
	private byte mRestartCause;
	private String mName;
	
	DeviceRestartCauseEnum(final byte inRestartCause, final String inName) {
		mRestartCause = inRestartCause;
		mName = inName;
	}
	
	public static DeviceRestartCauseEnum getRestartEnum(byte inRestartNum) {
		DeviceRestartCauseEnum result = DeviceRestartCauseEnum.INVALID;
		
		switch(inRestartNum) {
			case RestartCauseNum.INVALID:
				result = DeviceRestartCauseEnum.INVALID;
				break;
			case RestartCauseNum.USER_RESTART:
				result = DeviceRestartCauseEnum.USER_RESTART;
				break;
			case RestartCauseNum.HARD_FAULT:
				result = DeviceRestartCauseEnum.HARD_FAULT;
				break;
			case RestartCauseNum.WATCHDOG_TIMEOUT:
				result = DeviceRestartCauseEnum.WATCHDOG_TIMEOUT;
				break;
			case RestartCauseNum.RESPONSE_TIMEOUT:
				result = DeviceRestartCauseEnum.RESPONSE_TIMEOUT;
				break;
			case RestartCauseNum.TXBUFF_TIMEOUT:
				result = DeviceRestartCauseEnum.TXBUFF_TIMEOUT;
				break;
			case RestartCauseNum.RXBUFF_TIMEOUT:
				result = DeviceRestartCauseEnum.RXBUFF_TIMEOUT;
				break;
			case RestartCauseNum.SMAC_ERROR:
				result = DeviceRestartCauseEnum.SMAC_ERROR;
				break;
			case RestartCauseNum.OTHER_ERROR:
				result = DeviceRestartCauseEnum.OTHER_ERROR;
				break;
			case RestartCauseNum.POWER_ON:
				result = DeviceRestartCauseEnum.POWER_ON;
				break;
			default:
				break;
		}
		
		return result;
	}
	
	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public int getRestartValue() {
		return mRestartCause;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public String getName() {
		return mName;
	}
	
	final static class RestartCauseNum {
		static final byte	INVALID				= -1;
		static final byte	USER_RESTART		= 1;
		static final byte	HARD_FAULT	= 2;
		static final byte	WATCHDOG_TIMEOUT = 3;
		static final byte	RESPONSE_TIMEOUT	= 4;
		static final byte	TXBUFF_TIMEOUT	= 5;
		static final byte	RXBUFF_TIMEOUT	= 6;
		static final byte	SMAC_ERROR	= 7;
		static final byte	OTHER_ERROR = 9;
		static final byte	POWER_ON	= 10;
	
		private RestartCauseNum() {
	
		};
	}
}