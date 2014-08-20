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

	private SecretKey skey = null;

	private String accountName = null;
	
	public CipherFileSwapUtils(String accountName) {
		this.accountName = accountName;
		this.skey = new SecretKeySpec((MainApp.getCryptKey() + accountName).getBytes(), "AES");
	}
	
	public byte[] encode(byte[] data) throws Exception {

		SecretKeySpec skeySpec = new SecretKeySpec(skey.getEncoded(), "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

		byte[] encrypted = cipher.doFinal(data);

		return encrypted;
	}

	public byte[] decode(byte[] data) throws Exception {
		
		SecretKeySpec skeySpec = new SecretKeySpec(skey.getEncoded(), "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec);

		byte[] decrypted = cipher.doFinal(data);

		return decrypted;
	}
	
	public void backup(File sourceFile) {
		String backupFilename = FileStorageUtils.getBackupCryptFolder(accountName, sourceFile);
		
		if(backupFilename!=null) {
		
			Log_OC.d(TAG, "Starting backup of file " + sourceFile.getName() + " with path '" + sourceFile.getPath() + "'");
			
			File targetFile = new File(backupFilename);
			
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
	            Log_OC.e(TAG, "Exception while encoding foreign file '" + sourceFile.getPath() + File.pathSeparator + sourceFile.getName() + "'", e);
			} finally {
	            try {
	                if (in != null) in.close();
	            } catch (IOException e) {
	                Log_OC.d(TAG, "Weird exception while closing input stream for '" + sourceFile.getName() + "' (ignoring)", e);
	            }
	            try {
	                if (out != null) out.close();
	            } catch (IOException e) {
	                Log_OC.d(TAG, "Weird exception while closing output stream for '" + targetFile.getName() + "' (ignoring)", e);
	            }
	        }
		} else {
			Log_OC.d(TAG, "File " + sourceFile.getName() + " with path '" + sourceFile.getPath() + "' is not pertinent with mobilesync");
		}
	}
	
	public static void restore(File file) {
		
	}
	
	

}
