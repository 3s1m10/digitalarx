package com.digitalarx.android.services;

import java.io.File;
import java.util.Vector;

import android.accounts.Account;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.digitalarx.android.MainApp;
import com.digitalarx.android.authentication.AccountUtils;
import com.digitalarx.android.datamodel.FileDataStorageManager;
import com.digitalarx.android.datamodel.OCFile;
import com.digitalarx.android.files.services.FileDownloader;
import com.digitalarx.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.lib.common.operations.RemoteOperation;

public class DownloadSyncService extends Service {

    // need refactor
    private static final String FIRST_LEVEL_SYNC_DIR = "/Shared/";
    private static final String SECOND_LEVEL_SYNC_DIR = "/Shared/MobileSync/";
    
    private IBinder mBinder = new DownloadSyncServiceBinder();

    private FileDataStorageManager mFileDataStorageManager = null;
    
    private Account mAccount = null;
    
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("DownloadSyncService", "Received start id " + startId + ": " + intent);
        mAccount = AccountUtils.getCurrentOwnCloudAccount(getApplicationContext());
        if(mAccount != null) {
        	// TODO: testing single file punctual download for push service
        	//syncRemoteFile("/Shared/MobileSync/sync.txt");
        	syncRemoteTree();
        }
        return Service.START_NOT_STICKY;
    }
    
    public void onCreate() {
        Log.i("DownloadSyncService", "onCreate");
    }

    public class DownloadSyncServiceBinder extends Binder {
        DownloadSyncService getService() {
            return DownloadSyncService.this;
        }
    }
    
    private void syncRemoteFile(String filename) {
    	if(filename.startsWith(MainApp.getMobileSyncFolder())) {
	    	Account account = AccountUtils.getCurrentOwnCloudAccount(getApplicationContext());
	    	mFileDataStorageManager = new FileDataStorageManager(account, getApplicationContext().getContentResolver());
	    	
	    	OCFile ocFile = mFileDataStorageManager.getFileByPath(filename);
	    	if(ocFile != null) {
		    	Log.i("DownloadSyncService", "starting sync of file '" + filename + "'");
		    	Intent i = new Intent(this, FileDownloader.class);
		    	i.putExtra(FileDownloader.EXTRA_ACCOUNT, account);
		    	i.putExtra(FileDownloader.EXTRA_FILE, ocFile);
		    	startService(i);
	    	} else {
	    		Log.i("DownloadSyncService", "got null ocFile from storageManager with specified filename '" + filename + "', skipping...");
	    	}
    	} else {
    		Log.i("DownloadSyncService", "'" + filename + "', is not pertinent do mobile sync, skipping...");
    	}
    }
    
    private void syncRemoteTree() {
    	Log.i("DownloadSyncService", "Starting full mobile remote sync");


    	Account account = AccountUtils.getCurrentOwnCloudAccount(getApplicationContext());

    	mFileDataStorageManager = new FileDataStorageManager(account, getApplicationContext().getContentResolver());

    	// init
    	OCFile firstLevelsyncDir = mFileDataStorageManager.getFileByPath(FIRST_LEVEL_SYNC_DIR);
    	OCFile secondLevelsyncDir = mFileDataStorageManager.getFileByPath(SECOND_LEVEL_SYNC_DIR);
    	if(firstLevelsyncDir != null) {
    		Log.i("DownloadSyncService", "got '" + FIRST_LEVEL_SYNC_DIR + "'");
    		//browseTo(syncDir);
    	}
    	if(secondLevelsyncDir != null) { 
    		Log.i("DownloadSyncService", "got '" + SECOND_LEVEL_SYNC_DIR + "'");
    		//browseTo(sharedDir);
    	}

    	Vector<OCFile> firstLevelSyncFiles = mFileDataStorageManager.getFolderContent(firstLevelsyncDir);
    	Vector<OCFile> secondLevelSyncFiles = mFileDataStorageManager.getFolderContent(secondLevelsyncDir);

    	Log.i("DownloadSyncService", "Found '" + secondLevelSyncFiles.size() + "' files in folder '" + SECOND_LEVEL_SYNC_DIR + "'");


    	if(secondLevelSyncFiles.size() == 0) {
    		initDefaultSync();
    	}

    	// awaked?
    	secondLevelsyncDir = mFileDataStorageManager.getFileByPath(SECOND_LEVEL_SYNC_DIR);

    	for (OCFile ocFile : secondLevelSyncFiles) {
    		Log.i("DownloadSyncService", "Processing '" + SECOND_LEVEL_SYNC_DIR + ocFile.getFileName() + "'");

    		//OCFile localFile = getStorageManager().getFileById(ocFile.getFileId());
    		//OCFile localFile = new OCFile(SYNC_DIR + ocFile.getFileName());
    		//Log.i("FileSyncService", "'" + SYNC_DIR + ocFile.getFileName() + "' locally instantiated");

    		File localFile = null;

    		//File localFile = getBaseContext().getFileStreamPath(ocFile.getStoragePath());
    		if(ocFile.getStoragePath() != null && !ocFile.getStoragePath().isEmpty()) {
    			localFile = new File(ocFile.getStoragePath());
    		}

    		Log.i("DownloadSyncService", "###### local filename getStoragePath is '" + ocFile.getStoragePath() + "'");
    		Log.i("DownloadSyncService", "###### getLocalModificationTimestamp is '" + ocFile.getLocalModificationTimestamp() + "' getModificationTimestamp() is '" + ocFile.getModificationTimestamp() +"'");
    		Log.i("DownloadSyncService", "###### getLastSyncDateForData is '" + ocFile.getLastSyncDateForData() + "' getCreationTimestamp is '" + ocFile.getCreationTimestamp() +"'");

    		/*
    		 * timestampUpdateRequired true if:
    		 * 1) local file has modification time after last sync time so must be locally overwritten
    		 * 2) server creation time is newer than local file modification time
    		 * 
    		 * Note : if server modification time is in future,
    		 * we need avoid indefinitely download that file.
    		 */
    		boolean localModificationSyncRequired /*AfterLastSync*/ = ocFile.getLocalModificationTimestamp() > ocFile.getLastSyncDateForData();
    		Log.i("DownloadSyncService", "###### localModificationSyncRequired is '" + localModificationSyncRequired + "'");

    		boolean remoteModificationIsNewer = ocFile.getModificationTimestamp() > ocFile.getLocalModificationTimestamp();
    		Log.i("DownloadSyncService", "###### remoteModificationIsNewer is '" + remoteModificationIsNewer + "'");
    		boolean remoteModificationTimestampIsInFuture = ocFile.getModificationTimestamp() > System.currentTimeMillis();
    		Log.i("DownloadSyncService", "###### remoteModificationTimestampIsInFuture is '" + remoteModificationTimestampIsInFuture + "'");
    		boolean remoteModificationSyncRequired = remoteModificationIsNewer && !remoteModificationTimestampIsInFuture;
    		Log.i("DownloadSyncService", "###### remoteModificationSyncRequired is '" + remoteModificationSyncRequired + "'");

    		boolean timestampUpdateRequired = localModificationSyncRequired || remoteModificationSyncRequired;
    		Log.i("DownloadSyncService", "###### timestampUpdateRequired is '" + timestampUpdateRequired + "'");


    		if(localFile != null && localFile.exists() && !timestampUpdateRequired) {
    			//if(localFile==null || (localFile.getCreationTimestamp() != ocFile.getCreationTimestamp() ) ) {
    			//if (!mDownloaderBinder.isDownloading(account, ocFile)) {
    			Log.i("DownloadSyncService", "###### '" + ocFile.getRemotePath() + "' already in sync");
    			//}
    		} else {
    			Log.i("DownloadSyncService", "###### '" + ocFile.getRemotePath() + "', going to sync...");
    			Intent i = new Intent(this, FileDownloader.class);
    			i.putExtra(FileDownloader.EXTRA_ACCOUNT, account);
    			i.putExtra(FileDownloader.EXTRA_FILE, ocFile);
    			startService(i);
    		}
    	}
    }

    
    private void initDefaultSync() {
//        OCFile secondLevelsyncDir = mFileDataStorageManager.getFileByPath(SECOND_LEVEL_SYNC_DIR);
//        Vector<OCFile> secondLevelSyncFiles = mFileDataStorageManager.getFolderContent(secondLevelsyncDir);
//        if(secondLevelSyncFiles.size() == 0) {
            OCFile firstLevelsyncDir = new OCFile(FIRST_LEVEL_SYNC_DIR);
            firstLevelsyncDir.setMimetype("DIR");
            firstLevelsyncDir.setParentId(FileDataStorageManager.ROOT_PARENT_ID);
            startSyncFolderOperation(firstLevelsyncDir);
            //}
            
            OCFile secondLevelsyncDir = new OCFile(SECOND_LEVEL_SYNC_DIR);
            secondLevelsyncDir.setMimetype("DIR");
            secondLevelsyncDir.setParentId(firstLevelsyncDir.getFileId());
            startSyncFolderOperation(secondLevelsyncDir);
//        }
    }
    
    public void startSyncFolderOperation(OCFile folder) {
        new DownloadSyncTask(folder).execute();
        
    }
    
    private class DownloadSyncTask extends AsyncTask<Void, Void, Void> {

        private OCFile mFolder = null;
        
        public DownloadSyncTask(OCFile folder) {
            mFolder = folder;
        }
        @Override
        protected Void doInBackground(Void... params) {
            startSyncFolderOperation(mFolder);
            return null;
        }
        public void startSyncFolderOperation(OCFile folder) {
            long currentSyncTime = System.currentTimeMillis(); 

            // perform folder synchronization
            RemoteOperation synchFolderOp = new SynchronizeFolderOperation( folder,  
                    currentSyncTime,
                    false,
                    true,
                    mFileDataStorageManager, 
                    mAccount, 
                    getApplicationContext()
                    );
            synchFolderOp.execute(mAccount, getApplicationContext());

        }

    }
    
}
