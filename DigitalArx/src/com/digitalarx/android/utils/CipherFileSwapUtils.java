package com.digitalarx.android.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.digitalarx.android.MainApp;

public class CipherFileSwapUtils {

	private static final String TAG = CipherFileSwapUtils.class.getSimpleName();

	private String accountName = null;
	
	public CipherFileSwapUtils(String accountName) {
		this.accountName = accountName;
	}
	
	public Cipher createCipherDecryptor() {
		Cipher cipher = null;
		try {
			String salt = MainApp.getCryptKey();
			String key = accountName + salt;
			key = key.substring(0, 16);
			SecretKey skey = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
			
			IvParameterSpec ivSpec = new IvParameterSpec(salt.getBytes("UTF-8"));
			cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
			cipher.init(Cipher.DECRYPT_MODE, skey, ivSpec);
			
		} catch (Exception e) {
			Log_OC.e(TAG, "Failed creating decrypt cipher", e);
		}
		return cipher;
	}
	
	public Cipher createCipherEncryptor() {
		Cipher cipher = null;
		try {
			String salt = MainApp.getCryptKey();
			String key = accountName + salt;
			key = key.substring(0, 16);
			SecretKey skey = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
			
			IvParameterSpec ivSpec = new IvParameterSpec(salt.getBytes("UTF-8"));
			cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
			cipher.init(Cipher.ENCRYPT_MODE, skey, ivSpec);
			
		} catch (Exception e) {
			Log_OC.e(TAG, "Failed creating encrypt cipher", e);
		}
		return cipher;
	}
	
//	public byte[] encode(byte[] data) throws Exception {
//
//		//SecretKeySpec skeySpec = new SecretKeySpec(skey.getEncoded(), "AES");
//		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//		
//		//cipher.init(Cipher.ENCRYPT_MODE, skey, ivSpec);
//		
//		cipher.init(Cipher.ENCRYPT_MODE, skey);
//
//		byte[] encrypted = Base64.encode(cipher.doFinal(data), Base64.DEFAULT);
//
//		return encrypted;
//	}

//	public byte[] decode(byte[] data) throws Exception {
//		
//		//SecretKeySpec skeySpec = new SecretKeySpec(skey.getEncoded(), "AES");
//		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//		
//		//cipher.init(Cipher.DECRYPT_MODE, skey, ivSpec);
//		
//		cipher.init(Cipher.DECRYPT_MODE, skey);
//		
//		byte[] decrypted = cipher.doFinal(Base64.decode(data, Base64.DEFAULT));
//
//		return decrypted;
//	}
	
	public void backup(File sourceFile) {
		String backupFilename = FileStorageUtils.getBackupFilename(accountName, sourceFile);
		
		if(backupFilename!=null) {
		
			Log_OC.d(TAG, "Starting backup of file " + sourceFile.getName() + " with path '" + sourceFile.getPath() + "'");
			
			File backupFile = new File(backupFilename);
			backupFile.getParentFile().mkdirs();
			
			mergeFileAttributes(sourceFile, backupFile);
			
			FileInputStream fis = null;
			FileOutputStream fos = null;
			CipherOutputStream cos = null;;
			try {
				fis = new FileInputStream(sourceFile);
				fos = new FileOutputStream(backupFile);
				cos = new CipherOutputStream(fos, createCipherEncryptor());
				
				byte[] block = new byte[1024];
				int i;
				while ((i = fis.read(block)) != -1) {
					cos.write(block, 0, i);
				}
			
			} catch (Exception e) { // IO and FOF
	            Log_OC.e(TAG, "Exception while encoding foreign file '" + sourceFile.getPath() + File.pathSeparator + sourceFile.getName() + "'", e);
			} finally {
				try {
	                if (fis != null) fis.close();
	            } catch (IOException e) {
	                Log_OC.d(TAG, "Weird exception while closing input stream for '" + sourceFile.getName() + "' (ignoring)", e);
	            }
				try {
	                if (cos != null) cos.close();
	            } catch (IOException e) {
	                Log_OC.d(TAG, "Weird exception while closing ciphered output stream for '" + backupFile.getName() + "' (ignoring)", e);
	            }
				Log_OC.d(TAG, "backup of file " + backupFile.getName() + " with path '" + backupFile.getPath() + "' ended");
	        }
		} else {
			Log_OC.d(TAG, "File " + sourceFile.getName() + " with path '" + sourceFile.getPath() + "' is not pertinent with mobilesync");
		}
			
	}
	
	public void restore(File backupFile) {
		String sourceFilename = FileStorageUtils.getRestoreFilename(accountName, backupFile);
		if(sourceFilename!=null) {
			
			Log_OC.d(TAG, "Starting restore of file " + backupFile.getName() + " with path '" + backupFile.getPath() + "'");
			
			File sourceFile = new File(sourceFilename);
			sourceFile.getParentFile().mkdirs();
			
			mergeFileAttributes(backupFile, sourceFile);
			
			FileInputStream fis = null;
			FileOutputStream fos = null;
			CipherInputStream cis = null;
			try {
				
				fis = new FileInputStream(backupFile);
				cis = new CipherInputStream(fis, createCipherDecryptor());
				fos = new FileOutputStream(sourceFile);
				byte[] block = new byte[1024];
				int i;
				while ((i = cis.read(block)) != -1) {
					fos.write(block, 0, i);
				}
			} catch (Exception e) { // IO and FOF
	            Log_OC.e(TAG, "Exception while decoding foreign file '" + sourceFile.getPath() + File.pathSeparator + sourceFile.getName() + "'", e);
			} finally {
				try {
	                if (cis != null) cis.close();
	            } catch (IOException e) {
	                Log_OC.d(TAG, "Weird exception while closing ciphered input stream for '" + backupFile.getName() + "' (ignoring)", e);
	            }
	            try {
	                if (fos != null) fos.close();
	            } catch (IOException e) {
	                Log_OC.d(TAG, "Weird exception while closing output stream for '" + sourceFile.getName() + "' (ignoring)", e);
	            }
	            Log_OC.d(TAG, "restore of file " + backupFile.getName() + " with path '" + backupFile.getPath() + "' ended");
	        }
		} else {
			Log_OC.d(TAG, "File " + backupFile.getName() + " with path '" + backupFile.getPath() + "' is not pertinent with mobilesync");
		}
	}
	
	public void deleteBackup(File sourceFile) {
		String backupFilename = FileStorageUtils.getBackupFilename(accountName, sourceFile);
		if(backupFilename!=null) {
			Log_OC.d(TAG, "Starting deletion of file " + sourceFile.getName() + " with path '" + sourceFile.getPath() + "'");
			File targetFile = new File(backupFilename);
			targetFile.delete();
		} else {
			Log_OC.d(TAG, "File " + sourceFile.getName() + " with path '" + sourceFile.getPath() + "' is not pertinent with mobilesync");
		}
		
	}
	
	public void fullRestore() {
		File backupCryptFolder = new File(FileStorageUtils.getMobileSyncBackupPathRoot(accountName));
		File[] cryptoFiles = backupCryptFolder.listFiles();
		if(cryptoFiles!=null) {
			for (File cryptoFile : cryptoFiles) {
				restore(cryptoFile);
			}
		}
	}
	
	public void fullBackup() {
		File sourceFolder = new File(FileStorageUtils.getMobileSyncSourcePathRoot(accountName));
		File[] sourceFiles = sourceFolder.listFiles();
		if(sourceFiles!=null) {
			for (File sourceFile : sourceFiles) {
				backup(sourceFile);
			}
		}
	}
	
	public void mergeFileAttributes(File source, File target) {
		// shitty non-working
		// see https://code.google.com/p/android/issues/detail?id=18624
//		if (!target.setLastModified(source.lastModified())) {
//			throw new RuntimeException("can't set lastModified file attribute");
//		}
	}
	
	

}