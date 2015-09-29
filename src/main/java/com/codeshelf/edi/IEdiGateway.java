/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IEdiService.java,v 1.6 2013/03/19 01:19:59 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.sql.Timestamp;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObjectTree;

public interface IEdiGateway extends IDomainObjectTree<Facility> {

	String getServiceName();

	boolean isLinked();
	
	Boolean getActive();

	boolean getUpdatesFromHost(ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationsImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter,
		ICsvAislesFileImporter inCsvAislesFileImporter);

	public Timestamp getLastSuccessTime();
	
	public boolean testConnection();
}
