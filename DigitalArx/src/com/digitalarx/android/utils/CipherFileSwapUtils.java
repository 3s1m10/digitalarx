package com.digitalarx.android.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.digitalarx.android.MainApp;

public class CipherFileSwapUtils {

	private static final String TAG = CipherFileSwapUtils.class.getSimpleName();

	private static SecretKey skey = new SecretKeySpec(MainApp.getCryptKey().getBytes(), "AES");

	public static byte[] encode(byte[] fileData) throws Exception {

		SecretKeySpec skeySpec = new SecretKeySpec(skey.getEncoded(), "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

		byte[] encrypted = cipher.doFinal(fileData);

		return encrypted;
	}

	public static byte[] decode(byte[] fileData) throws Exception {
		
		SecretKeySpec skeySpec = new SecretKeySpec(skey.getEncoded(), "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec);

		byte[] decrypted = cipher.doFinal(fileData);

		return decrypted;
	}
	
	public static void backup(File sourceFile) {
		File targetFile = new File(FileStorageUtils.getCryptPath(), sourceFile.getName());
		
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(sourceFile);
			out = new FileOutputStream(targetFile);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0){
				out.write(encode(buf), 0, len);
			}
		} catch (Exception e) { // IO and FOF
            Log_OC.e(TAG, "Exception while encoding foreign file " + sourceFile.getPath() + File.pathSeparator + sourceFile.getName(), e);
		} finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
                Log_OC.d(TAG, "Weird exception while closing input stream for " + sourceFile.getName() + " (ignoring)", e);
            }
            try {
                if (out != null) out.close();
            } catch (IOException e) {
                Log_OC.d(TAG, "Weird exception while closing output stream for " + targetFile.getName() + " (ignoring)", e);
            }
        }
	}
	
	public static void restore(File file) {
		
	}

}
