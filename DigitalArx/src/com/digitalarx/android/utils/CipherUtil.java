package com.digitalarx.android.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CipherUtil {


	private static SecretKey skey = null;

	public CipherUtil(String password) {
		skey = new SecretKeySpec(password.getBytes(), "AES");
	}

	public static byte[] encodeFile(byte[] fileData) throws Exception
	{

		SecretKeySpec skeySpec = new SecretKeySpec(skey.getEncoded(), "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

		byte[] encrypted = cipher.doFinal(fileData);

		return encrypted;
	}

	public static byte[] decodeFile(byte[] fileData) throws Exception
	{
		SecretKeySpec skeySpec = new SecretKeySpec(skey.getEncoded(), "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec);

		byte[] decrypted = cipher.doFinal(fileData);

		return decrypted;
	}

}
