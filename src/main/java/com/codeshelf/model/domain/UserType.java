package com.gadgetworks.codeshelf.model.domain;

public enum UserType {
	INVALID(UserTypeNum.INVALID, "INVALID"),
	SUPER(UserTypeNum.SUPER, "SUPER"),
	APPUSER(UserTypeNum.APPUSER, "APPUSER"),
	SITECON(UserTypeNum.SITECON, "SITECON"),
	SYSTEM(UserTypeNum.SYSTEM, "SYSTEM");

	private int		mValue;
	private String	mName;

	UserType(final int inValue, final String inName) {
		mValue = inValue;
		mName = inName;
	}

	public static UserType getUserTypeEnum(int inUserTypeID) {
		UserType result;

		switch (inUserTypeID) {
			case UserTypeNum.SUPER:
				result = UserType.SUPER;
				break;

			case UserTypeNum.APPUSER:
				result = UserType.APPUSER;
				break;

			case UserTypeNum.SITECON:
				result = UserType.SITECON;
				break;

			case UserTypeNum.SYSTEM:
				result = UserType.SYSTEM;
				break;

			default:
				result = UserType.INVALID;
				break;

		}

		return result;
	}

	public int getValue() {
		return mValue;
	}

	public String getName() {
		return mName;
	}

	final static class UserTypeNum {

		static final byte	INVALID		= -1;
		static final byte	SUPER	= 0;
		static final byte	APPUSER= 1;
		static final byte	SITECON	= 2;
		static final byte	SYSTEM= 3;

		private UserTypeNum() {
		};
	}
}
