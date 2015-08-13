package com.codeshelf.edi;

import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.jcraft.jsch.UserInfo;

/**
 * an SFTP EDI site configuration which can be stored as a single string with an encrypted password.
 * @author ivan
 *
 */
public class SftpConfiguration implements UserInfo {
	static final Logger	LOGGER	= LoggerFactory.getLogger(SftpConfiguration.class);

	private static final String	DEFAULT_SFTP_PASSWORD_KEY	= "1234567812345678";
	private static IvParameterSpec iv = new IvParameterSpec(new byte[16]);
	
	public SftpConfiguration() {
		this("", 22, "", "", "", "");
	}

	public SftpConfiguration(String host, Integer port, String username, String importPath, String archivePath, String exportPath) {
		super();
		this.host = host;
		this.port = port;
		this.username = username;
		this.importPath = importPath;
		this.archivePath = archivePath;
		this.exportPath = exportPath;
		this.passwordEnc = null;
	}

	@Getter
	@Setter
	@Expose
	private String	host;

	@Getter
	@Setter
	@Expose
	private Integer	port;

	@Getter
	@Setter
	@Expose
	private String	username;

	// encoded password
	@Getter
	@Setter
	@Expose
	private String	passwordEnc;

	// path where files (eg orders) are found and downloaded ; null or empty if not used
	@Getter
	@Setter
	@Expose
	private String	importPath;

	// path where imported files (eg orders) are moved after download ; null or empty if not used
	@Getter
	@Setter
	@Expose
	private String	archivePath;

	// path where exported files (eg completed WIs) are uploaded ; null or empty if not used
	@Getter
	@Setter
	@Expose
	private String	exportPath;

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		return gson.toJson(this);
	}

	static public SftpConfiguration fromString(String string) {
		Gson mGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		SftpConfiguration configuration = mGson.fromJson(string, SftpConfiguration.class);
		return configuration;
	}

	static public String decodePassword(String encrypted) {
		return tryCipher(encrypted, null);
	}

	static public String encodePassword(String plaintext) {
		return tryCipher(null, plaintext);
	}

	@Override
	public String getPassword() {
		String encoded = this.getPasswordEnc();
		if (encoded == null)
			return null;

		return SftpConfiguration.decodePassword(encoded);
	}

	void setPassword(String password) {
		this.setPasswordEnc(SftpConfiguration.encodePassword(password));
	}

	static private String doCipher(String toDecrypt, String toEncrypt) 
			throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, 
			InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
		String key = System.getProperty("edi.sftp.password_key",DEFAULT_SFTP_PASSWORD_KEY);
		Cipher cipher = null;
		Charset encoding = Charset.forName("ISO-8859-1");

		SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(encoding), "AES");
		cipher = Cipher.getInstance("AES");
		
		String result = "";
		if (cipher != null && keySpec != null) {
			if (toDecrypt != null) {
				// decrypt
				byte[] encrypted = Base64.decodeBase64(toDecrypt.getBytes(encoding));
				cipher.init(Cipher.DECRYPT_MODE, keySpec);
				result = new String(cipher.doFinal(encrypted),encoding);
				
			} else if (toEncrypt != null) {
				// encrypt
				byte[] plaintext = toEncrypt.getBytes(encoding);
				cipher.init(Cipher.ENCRYPT_MODE, keySpec);
				result = new String(Base64.encodeBase64(cipher.doFinal(plaintext)));
			}
		}

		return result;
	}
	
	static private String tryCipher(String encrypted, String plaintext) {
		String result = "";
		try {
			result = doCipher(encrypted,plaintext);
		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException
				| NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
			LOGGER.error("Unexpected encryption error when working with SFTP account password", e);
		}
		return result;
		
	}

	@Override
	public String getPassphrase() {
		return null;
	}

	@Override
	public boolean promptPassword(String message) {
		return true;
	}

	@Override
	public boolean promptPassphrase(String message) {
		return true;
	}

	@Override
	public boolean promptYesNo(String message) {
		return true;
	}

	@Override
	public void showMessage(String message) {
	}

	public boolean matchOrdersFilename(String filename) {
		return true;
		//return filename.endsWith(".DAT");
	}

	public String getUrl() {
		return String.format("sftp://%s@%s:%d", username,host,(int)port);
	}
}