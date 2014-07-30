package com.gadgetworks.codeshelf.application;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import com.gadgetworks.codeshelf.model.domain.User;

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
		String usage = "Usage java " + PasswordUpdateGenerator.class.getName() + " -e <email> -o <organizationName> -p <password>";
		
		String inOrganizationName = null;
		String inEmail = null;
		String inPassword = null;
		if (args.length != 6) System.err.println(usage);
		for (int i = 0; i < args.length; i++) {
			String string = args[i];
			if(string.equals("-o")) {
				inOrganizationName = args[++i];
			} else if(string.equals("-e")) {
				inEmail = args[++i];
			} else if (string.equals("-p")) {
				inPassword = args[++i];
			}
			else {
				System.err.println(usage);
			}
		} 
		String updateSql = User.generatePasswordUpdateSql(inOrganizationName, inEmail, inPassword);
		System.out.println(updateSql);
	}

}
