package com.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.Vector;

import javax.persistence.Transient;

import lombok.Getter;
import lombok.AccessLevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.SftpConfiguration;
import com.codeshelf.model.EdiProviderEnum;
import com.codeshelf.model.EdiServiceStateEnum;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public abstract class AbstractSftpEdiService extends EdiServiceABC {
	static final Logger	LOGGER				= LoggerFactory.getLogger(AbstractSftpEdiService.class);

	@Transient
	private JSch		jsch				= new JSch();

	@Transient
	@Getter(AccessLevel.PROTECTED)
	private Session		session				= null;

	@Transient
	@Getter(AccessLevel.PROTECTED)
	private ChannelSftp	channel				= null;

	@Transient
	SftpConfiguration	sftpConfiguration	= null;

	@Transient
	String				lastProviderCredentials = null; 

	public AbstractSftpEdiService() {
		super();
		setProvider(EdiProviderEnum.OTHER);
		setServiceState(EdiServiceStateEnum.LINKED); // TODO: maybe add UX setup procedure to verify connection works
	}
	
	public AbstractSftpEdiService(String domainId) {
		this();
		setDomainId(domainId);
	}

	synchronized public SftpConfiguration getConfiguration() {
		// decoding the SFTP provider configuration is expensive, so we store the result
		// and check whether the source string has been changed
		String currentProviderCredentials = getProviderCredentials();
		if (sftpConfiguration == null 
				|| (lastProviderCredentials != null && !lastProviderCredentials.equals(currentProviderCredentials))) {
			sftpConfiguration = SftpConfiguration.fromString(currentProviderCredentials);
		}
		lastProviderCredentials = currentProviderCredentials;
		return sftpConfiguration;
	}

	synchronized public void setConfiguration(SftpConfiguration configuration) {
		sftpConfiguration = configuration;
		lastProviderCredentials = configuration.toString();
		this.setProviderCredentials(lastProviderCredentials);
		setServiceState(EdiServiceStateEnum.LINKED); // TODO: maybe add UX setup procedure to verify connection works
	}

	@Override
	public boolean getHasCredentials() {
		SftpConfiguration config = this.getConfiguration();
		return (config != null && config.getHost() != null && config.getUsername() != null && config.getPassword() != null);
	}

	public ChannelSftp connect() {
		if (session != null || channel != null) {
			LOGGER.error("tried to connect SFTP session, but was already open. forcing disconnect first.");
			disconnect();
		}

		SftpConfiguration config = this.getConfiguration();
		try {
			session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());
		} catch (JSchException e) {
			LOGGER.error("Unexpected exception setting up SFTP connection, check site configuration", e);
			return null;
		}
		session.setUserInfo(config);

		// create sftp connection
		try {
			session.connect();
		} catch (JSchException e) {
			LOGGER.warn("Failed to connect to sftp://{}@{}:{}", config.getUsername(), config.getHost(), config.getPort(), e);
			return null;
		}

		// connected, now create SFTP channel
		try {
			Channel c = session.openChannel("sftp");
			if (c instanceof ChannelSftp) {
				channel = (ChannelSftp) c;
				channel.connect();
			}
		} catch (JSchException e) {
			LOGGER.warn("Connected, but failed to open channel sftp://{}@{}:{}",
				config.getUsername(),
				config.getHost(),
				config.getPort(),
				e);
			channel = null;
		}
		if (channel == null)
			disconnect(); // hang up if SFTP channel wasn't created

		LOGGER.info("EDI service {} connected to sftp://{}@{}:{}",
			this.getServiceName(),
			config.getUsername(),
			config.getHost(),
			config.getPort());

		return channel;
	}

	public void disconnect() {
		if (session == null) {
			if (channel != null) {
				LOGGER.error("disconnect() was called, there is a channel but no session. this is probably a bug.");
				channel.disconnect();
				channel = null;
			} else {
				LOGGER.error("disconnect() was called, but there is no session. this is probably a bug.");
			}
		} else if (!session.isConnected()) {
			if (channel == null) {
				LOGGER.warn("disconnect() was called on an existing session that is not connected");
			} else {
				LOGGER.warn("disconnect() was called on an existing session that is not connected (sftp channel not null)");
				if (channel.isConnected()) {
					channel.disconnect();
				}
				channel = null;
			}
			session = null;
		} else {
			if (channel != null) {
				if (channel.isConnected()) {
					channel.disconnect();
				}
				channel = null;
			}
			session.disconnect();
			session = null;
		}
	}

	public ArrayList<String> retrieveImportFileList() {
		Vector<?> fileList = null;
		SftpConfiguration config = getConfiguration();
		try {
			fileList = this.getChannel().ls(config.getImportPath());
		} catch (SftpException e) {
			LOGGER.warn("Error retrieving file list from sftp://{}@{}:{}/{}",
				config.getUsername(),
				config.getHost(),
				config.getPort(),
				config.getImportPath(),
				e);
			return new ArrayList<String>(0);
		} 
		
		ArrayList<String> filesToImport = new ArrayList<String>(fileList.size()); 
		if (fileList != null) {
			// iterate through list of files and remove all but regular files
			for(int ix=0; ix < fileList.size(); ix++) {
				Object entry = fileList.get(ix);
				if (entry instanceof LsEntry) {
					LsEntry lsEntry = (LsEntry) entry;
					if(lsEntry.getAttrs().isReg()) {
						// regular file (not a folder or link etc)
						filesToImport.add(lsEntry.getFilename());
					} else {
						LOGGER.debug("skipping non-file: {}",lsEntry.getLongname());
					}
				} else {
					LOGGER.warn("Unexpected ls result, not LsEntry but {} {}",entry.getClass(),entry.toString());
				}
			}
		}
		return filesToImport;
	}
}