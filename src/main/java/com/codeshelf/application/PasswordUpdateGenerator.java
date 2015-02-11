package com.codeshelf.application;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import com.codeshelf.platform.multitenancy.User;

/**
 * 		
 * 
 * Generate a sql statement that can be executed at the database to chance a user's password.  
 * 
 * TODO this is expected to be shortlived until there is a remote admin function
 * 
 * 
 */
public class PasswordUpdateGenerator {

	/**
	 *
	 * @throws InvalidKeySpecException 
	 * 	@throws NoSuchAlgorithmException 
	 */
	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException {
		String usage = "Usage java " + PasswordUpdateGenerator.class.getName() + " -e <email> -o <organizationName> -s <schemName> -p <password>";
		
		String organizationName = null;
		String email = null;
		String password = null;
		String schemaName = null;
		if (args.length != 8) System.err.println(usage);
		for (int i = 0; i < args.length; i++) {
			String string = args[i];
			if(string.equals("-o")) {
				organizationName = args[++i];
			} else if(string.equals("-e")) {
				email = args[++i];
			} else if (string.equals("-p")) {
				password = args[++i];
			} else if (string.equals("-s")) {
				schemaName = args[++i];
			}
			else {
				System.err.println(usage);
			}
		} 
		String updateSql = User.generatePasswordUpdateSql(organizationName, email, password, schemaName);
		System.out.println(updateSql);
	}

}
