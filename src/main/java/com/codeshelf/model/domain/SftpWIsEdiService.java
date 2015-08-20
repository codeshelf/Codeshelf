package com.codeshelf.model.domain;

import java.io.IOException;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.ICsvAislesFileImporter;
import com.codeshelf.edi.ICsvCrossBatchImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.ICsvOrderLocationImporter;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

@Entity
@DiscriminatorValue("SFTP_WIS")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class SftpWIsEdiService extends AbstractSftpEdiService {
	public static class SftpWIsEdiServiceDao extends GenericDaoABC<SftpWIsEdiService> implements ITypedDao<SftpWIsEdiService> {
		public final Class<SftpWIsEdiService> getDaoClass() {
			return SftpWIsEdiService.class;
		}
	}

	static final Logger			LOGGER						= LoggerFactory.getLogger(SftpWIsEdiService.class);
	public static final String	SFTP_SERVICE_NAME			= "SFTP_WIS";

	public SftpWIsEdiService() {
		super();
	}
	
	public SftpWIsEdiService(String domainId) {
		super(domainId);
	}

	@Override
	public String getServiceName() {
		return SFTP_SERVICE_NAME;
	}

	@Override
	public boolean getUpdatesFromHost(ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationsImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter,
		ICsvAislesFileImporter inCsvAislesFileImporter) {

		// not implemented in this service

		return true;
	}

	@Override
	public void sendWorkInstructionsToHost(String exportMessage) throws IOException {
		// not implemented in this service
	}

	@SuppressWarnings("unchecked")
	@Override
	public ITypedDao<SftpWIsEdiService> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<SftpWIsEdiService> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(SftpWIsEdiService.class);
	}

}
