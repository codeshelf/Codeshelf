package com.codeshelf.edi;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.Transient;
import javax.ws.rs.core.MultivaluedMap;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.util.StringUIConverter;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
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
	static final Logger				LOGGER						= LoggerFactory.getLogger(SftpConfiguration.class);

	private static final byte[]		DEFAULT_SFTP_PASSWORD_KEY	= { 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,	0x01, 0x01, 0x01, 0x01, 0x01, 0x01};

	private static final String		SFTP_PASSWORD_CIPHER		= "AES";

	@Getter
	@Setter
	@Expose
	private String					host;

	@Getter
	@Setter
	@Expose
	private Integer					port;

	@Getter
	@Setter
	@Expose
	private String					username;

	// encoded password
	@Getter
	@Setter
	@Expose
	private String					passwordEnc;

	// path where files (eg orders) are found and downloaded ; null or empty if not used
	@Getter
	@Setter
	@Expose
	private String					importPath;

	// path where imported files (eg orders) are moved after download ; null or empty if not used
	@Getter
	@Setter
	@Expose
	private String					archivePath;

	// path where exported files (eg completed WIs) are uploaded ; null or empty if not used
	@Getter
	@Setter
	@Expose
	private String					exportPath;

	@Getter
	@Setter
	@Expose
	private int 					timeOutMilliseconds;
	
	@Getter
	@Setter
	@Expose
	private Boolean					active;

	
	@Transient
	static private SecretKeySpec	keySpec;
	static {
		byte[] keyBytes = DEFAULT_SFTP_PASSWORD_KEY;
		String keyString = System.getProperty("edi.sftp.password_key");
		if (keyString != null && keyString.length() > 1) {
			try {
				keyBytes = StringUIConverter.hexStringToBytes(keyString);
			} catch (NumberFormatException e) {
				LOGGER.error("could not parse edi.sftp.password_key value: {}", keyString);
			}
		}
		keySpec = new SecretKeySpec(keyBytes, SFTP_PASSWORD_CIPHER);
	}

	
	public static SftpConfiguration updateFromMap(SftpConfiguration config, MultivaluedMap<String, String> params) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
			if (config == null) {
				config = new SftpConfiguration();
			}
			BeanUtilsBean beanUtils = new BeanUtilsBean();
			for (String key : params.keySet()) {
				String value = Strings.emptyToNull(params.getFirst(key));
				if (key.equals("password") && value == null) {
					continue;
				}
				
				beanUtils.copyProperty(config, key, value);
			}
			return config;
	}
	
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
		this.timeOutMilliseconds = Ints.saturatedCast(TimeUnit.SECONDS.toMillis(5));

	}

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

	public void setPassword(String password) {
		this.setPasswordEnc(SftpConfiguration.encodePassword(password));
	}

	static private String doCipher(String toDecrypt, String toEncrypt) throws IllegalBlockSizeException,
		BadPaddingException,
		InvalidKeyException,
		InvalidKeySpecException,
		NoSuchAlgorithmException,
		NoSuchPaddingException,
		InvalidAlgorithmParameterException {

		Charset encoding = Charset.forName("ISO-8859-1");
		Cipher cipher = Cipher.getInstance(SFTP_PASSWORD_CIPHER);
		String result = "";
		
		if (cipher != null && keySpec != null) {
			if (toDecrypt != null) {
				// decrypt
				byte[] encrypted = Base64.decodeBase64(toDecrypt.getBytes(encoding));
				cipher.init(Cipher.DECRYPT_MODE, keySpec);
				result = new String(cipher.doFinal(encrypted), encoding);

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
			result = doCipher(encrypted, plaintext);
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
		return String.format("sftp://%s@%s:%d", username, host, (int) port);
	}
}