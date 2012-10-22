/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsvImporter.java,v 1.1 2012/10/22 07:38:07 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.InputStreamReader;

import com.gadgetworks.codeshelf.model.domain.Facility;

/**
 * @author jeffw
 *
 */
public interface ICsvImporter {

	void importOrdersFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility);

	void importInventoryFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility);

}
