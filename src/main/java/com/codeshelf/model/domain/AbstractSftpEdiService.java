package com.codeshelf.model.domain;

import com.codeshelf.edi.SftpConfiguration;
import com.codeshelf.model.EdiProviderEnum;
import com.codeshelf.model.EdiServiceStateEnum;

public abstract class AbstractSftpEdiService extends EdiServiceABC {

	public AbstractSftpEdiService() {
		super();
		setProvider(EdiProviderEnum.OTHER);
		setServiceState(EdiServiceStateEnum.LINKED);
	}

	public SftpConfiguration getConfiguration() {
		return SftpConfiguration.fromString(getProviderCredentials());
	}

	public void setConfiguration(SftpConfiguration configuration) {
		this.setProviderCredentials(configuration.toString());
	}

	@Override
	public boolean getHasCredentials() {
		SftpConfiguration config = this.getConfiguration();
		return (config != null
				&& config.getHost() != null
				&& config.getUsername() != null
				&& config.getPassword() != null);				
	}

}