package com.codeshelf.testframework;

public abstract class MinimalTest extends FrameworkTest {

	@Override
	Type getFrameworkType() {
		return Type.MINIMAL;
	}

	@Override
	public boolean ephemeralServicesShouldStartAutomatically() {
		return false;
	}

}
