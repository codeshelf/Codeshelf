/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014-2014, Codeshelf, All rights reserved
 *******************************************************************************/
package com.codeshelf.edi;

// --------------------------------------------------------------------------
/**
 *  @author jon ranstrom
 */
public class EdiFileWriteException extends RuntimeException {
	// Should this be an Exception? (checked). The java style police say so, but DaoException is not, and database errors are lumped with user input as
	// something for checked exceptions.
	// The point of this exception is to throw from some methods slightly deep in the call stack, and then catch at the bean reader level.
	// Much easier and less code with an unchecked exception.

	// Because RuntimeException is serializable there needs to be a serial version. Is this made up value ok?
	// DaoException has private static final long	serialVersionUID	= -8652996336581739434L;
	private static final long	serialVersionUID	= -8652996336581739435L;

	public EdiFileWriteException(final String inMsg) {
		super(inMsg);
	}

	public EdiFileWriteException(final String inMsg, Throwable cause) {
		super(inMsg, cause);
	}

}
