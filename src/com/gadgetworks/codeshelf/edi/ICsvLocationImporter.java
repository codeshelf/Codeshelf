/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsvImporter.java,v 1.2 2013/04/09 07:58:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.InputStreamReader;

import com.gadgetworks.codeshelf.model.domain.Facility;

/**
 * @author jeffw
 *
 */
public interface ICsvLocationImporter {

	void importLocationAliasesFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility);

}
