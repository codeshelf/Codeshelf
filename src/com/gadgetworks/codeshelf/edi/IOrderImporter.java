/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IOrderImporter.java,v 1.1 2012/10/10 22:15:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.InputStreamReader;

import com.gadgetworks.codeshelf.model.domain.Facility;

/**
 * @author jeffw
 *
 */
public interface IOrderImporter {

	void importerFromCsvStream(InputStreamReader inStreamReader, Facility inFacility);

}
