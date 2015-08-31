package com.codeshelf.model.domain;

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
import com.codeshelf.edi.EdiExportTransport;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

@Entity
@DiscriminatorValue("SFTP_WIS")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class SftpWIsEdiService extends AbstractSftpEdiService implements EdiExportTransport {
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
	public void transportWiComplete(OrderHeader inOrder, Che inChe, String exportMessage) {
	
	}

	@Override
	public void transportOrderRemoveFromCart(OrderHeader inOrder, Che inChe, String message) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public ExportReceipt transportOrderCompleteOnCart(OrderHeader inOrder, Che inChe, String contents) {
		String filename = String.format("COMPLETE_%s_%s_%s.DAT",  inOrder.getOrderId(), inChe.getDeviceGuidStr(), System.currentTimeMillis());
		final String absoluteFilename = this.getConfiguration().getExportPath() + "/" + filename;
		return uploadAsFile(contents, absoluteFilename);
	}

	@Override
	public void transportOrderOnCart(OrderHeader inOrder, Che inChe, String contents) {
		String filename = String.format("LOADED_%s_%s_%s.DAT",  inOrder.getOrderId(), inChe.getDeviceGuidStr(), System.currentTimeMillis());
		final String absoluteFilename = this.getConfiguration().getExportPath() + "/" + filename;
		try {
			uploadAsFile(contents, absoluteFilename);
		} catch (Exception e) {
			LOGGER.warn("Unable to upload {}\n Contents:\n{}", absoluteFilename,  contents, e);
		}
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
