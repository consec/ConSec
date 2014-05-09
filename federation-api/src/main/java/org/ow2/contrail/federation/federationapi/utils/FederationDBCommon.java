package org.ow2.contrail.federation.federationapi.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class FederationDBCommon {

	protected static Logger logger =
		Logger.getLogger(FederationDBCommon.class);


	public static final String REST_HEADERS_FEDERATIONDB = "X-ConFedDB-Attribute";

	/**
	 * Helper method for printing stack trace into a string.
	 * 
	 * @param aThrowable error to be traced
	 * @return string presenting stack trace
	 */
	public static String getStackTrace(Throwable aThrowable) {
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return result.toString();
	}

	public static String makeSHA1Hash(String input)
	throws NoSuchAlgorithmException
	{
		MessageDigest md = MessageDigest.getInstance("SHA1");
		md.reset();
		byte[] buffer = input.getBytes();
		md.update(buffer);
		byte[] digest = md.digest();

		String hexStr = "";
		for (int i = 0; i < digest.length; i++) {
			hexStr +=  Integer.toString( ( digest[i] & 0xff ) + 0x100, 16).substring( 1 );
		}
		return hexStr;
	}

	/**
	 * Extract id from given entity Id.  
	 * 
	 * @param response
	 * @return
	 * @throws Exception
	 */
	public static int getIdFromString(String response) throws Exception {		
		logger.debug("Got string to parse " +  response);
		int id = -1;
		try{
			id = Integer.parseInt(response);
			return id;
		}catch(NumberFormatException err){
			logger.debug("Not a number");
		}
		// try to parse URI
		response = response.substring(response.lastIndexOf("/") + 1);
		try{
			id = Integer.parseInt(response);
		}catch(NumberFormatException err){
			logger.debug("Could not parse the URI for Id.");
			throw new Exception ( "Could not parse the URI for Id." );
		}
		logger.debug("Got id "+ id );
		return id;
	}
}
