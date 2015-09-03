package com.codeshelf.model.domain;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Vector;

import javax.persistence.Transient;

import lombok.AccessLevel;
import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.EdiFileWriteException;
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
		setServiceState(EdiServiceStateEnum.UNLINKED); // TODO: maybe add UX setup procedure to verify connection works
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
		if (getHasCredentials()) {
			setServiceState(EdiServiceStateEnum.LINKED); // TODO: maybe add UX setup procedure to verify connection works
		}
	}

	@Override
	public boolean getHasCredentials() {
		SftpConfiguration config = this.getConfiguration();
		return (config != null && config.getHost() != null && config.getUsername() != null && config.getPassword() != null);
	}

	
	synchronized protected ChannelSftp connect() throws IOException {
		if (session != null || channel != null) {
			LOGGER.error("tried to connect SFTP session, but was already open. forcing disconnect first.");
			disconnect();
		}

		SftpConfiguration config = this.getConfiguration();
		try {
			try {
				session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());
			} catch (JSchException e) {
				throw new IOException(String.format("Failed to create SFTP session, check Edi configuration: %s", toSftpChannelDebug()), e);
			}
			session.setUserInfo(config);

			// create sftp connection
			try {
				session.connect(config.getTimeOutMilliseconds());
			} catch (JSchException e) {
				throw new IOException(String.format("Failed to connect to SFTP server, check Edi configuration: %s", toSftpChannelDebug()), e);
			}

			// connected, now create SFTP channel
			try {
				Channel c = session.openChannel("sftp");
				if (c instanceof ChannelSftp) {
					channel = (ChannelSftp) c;
					channel.connect();
				}
			} catch (JSchException e) {
				throw new IOException(String.format("Failed to open channel, check Edi configuration: %s", toSftpChannelDebug()), e);
			}
			LOGGER.info("EDI service {} connected to {}", this.getServiceName(), toSftpChannelDebug());

			return channel;
		} finally {
			if (channel == null) { 
				disconnect(); // hang up if SFTP channel wasn't created
			}
		}

	}

	
	protected void disconnect() {
		if (session == null) {
			if (channel != null) {
				LOGGER.error("disconnect() was called, there is a channel but no session. this is probably a bug. {}", toSftpChannelDebug());
				channel.disconnect();
				channel = null;
			} else {
				LOGGER.error("disconnect() was called, but there is no session. this is probably a bug. {}", toSftpChannelDebug());
			}
		} else if (!session.isConnected()) {
			if (channel == null) {
				LOGGER.warn("disconnect() was called on an existing session that is not connected. {}", toSftpChannelDebug());
			} else {
				LOGGER.warn("disconnect() was called on an existing session that is not connected (sftp channel not null) {}", toSftpChannelDebug());
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
	
	synchronized public FileExportReceipt uploadAsFile(final String fileContents, final String filename) throws EdiFileWriteException {
		try {
			ChannelSftp sftp = connect();
			byte[] bytes = fileContents.getBytes(StandardCharsets.ISO_8859_1);
			sftp.put(new ByteArrayInputStream(bytes), filename, ChannelSftp.OVERWRITE);
			return new FileExportReceipt(filename, bytes.length);
		} catch(SftpException | IOException  e) {
			throw new EdiFileWriteException("Unable to put file: " + filename, e);
		}finally {
			disconnect();
		}
	}
	
	synchronized public void downloadFile(String absoluteFilename, OutputStream out) throws EdiFileWriteException {
		try {
			ChannelSftp sftp = connect();
			sftp.get(absoluteFilename, out);
		} catch(SftpException | IOException  e) {
			throw new EdiFileWriteException("Unable to get file: " + absoluteFilename, e);
		} finally {
			disconnect();
		}
		
	}

	synchronized public void delete(String path) throws EdiFileWriteException {
		try {
			ChannelSftp sftp = connect();
			sftp.rm(path);
		} catch(SftpException | IOException  e) {
			throw new EdiFileWriteException("Unable to remove file: " + path, e);
		} finally {
 			disconnect();
		}
	}


	protected ArrayList<String> retrieveImportFileList() throws EdiFileWriteException {
		Vector<?> fileList = null;
		SftpConfiguration config = getConfiguration();
		String importPath = config.getImportPath();
		try {
			fileList = this.getChannel().ls(importPath);
		} catch (SftpException e) {
			throw new EdiFileWriteException("Unable to list files from directory: " + importPath, e);
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
	
	private String toSftpChannelDebug() {
		SftpConfiguration config = getConfiguration();
		return String.format("sftp://%s@%s:%d", 
			config.getUsername(),
			config.getHost(),
			config.getPort());
	}
}