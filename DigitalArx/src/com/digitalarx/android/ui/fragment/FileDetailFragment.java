/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.digitalarx.android.ui.fragment;

import java.lang.ref.WeakReference;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.digitalarx.android.R;
import com.digitalarx.android.datamodel.FileDataStorageManager;
import com.digitalarx.android.datamodel.OCFile;
import com.digitalarx.android.files.FileMenuFilter;
import com.digitalarx.android.files.services.FileDownloader.FileDownloaderBinder;
import com.digitalarx.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.digitalarx.android.services.observer.FileObserverService;
import com.digitalarx.android.ui.activity.FileActivity;
import com.digitalarx.android.ui.activity.FileDisplayActivity;
import com.digitalarx.android.ui.dialog.RemoveFileDialogFragment;
import com.digitalarx.android.ui.dialog.RenameFileDialogFragment;
import com.digitalarx.android.utils.DisplayUtils;
import com.digitalarx.android.utils.Log_OC;


/**
 * This Fragment is used to display the details about a file.
 * 
 * @author Bartek Przybylski
 * @author David A. Velasco
 */
public class FileDetailFragment extends FileFragment implements OnClickListener {

    private int mLayout;
    private View mView;
    private Account mAccount;
    
    public ProgressListener mProgressListener;
    
    private static final String TAG = FileDetailFragment.class.getSimpleName();
    public static final String FTAG_CONFIRMATION = "REMOVE_CONFIRMATION_FRAGMENT";
    public static final String FTAG_RENAME_FILE = "RENAME_FILE_FRAGMENT";
    

    /**
     * Creates an empty details fragment.
     * 
     * It's necessary to keep a public constructor without parameters; the system uses it when tries to reinstantiate a fragment automatically. 
     */
    public FileDetailFragment() {
        super();
        mAccount = null;
        mLayout = R.layout.file_details_empty;
        mProgressListener = null;
    }
    
    /**
     * Creates a details fragment.
     * 
     * When 'fileToDetail' or 'ocAccount' are null, creates a dummy layout (to use when a file wasn't tapped before).
     * 
     * @param fileToDetail      An {@link OCFile} to show in the fragment
     * @param ocAccount         An ownCloud account; needed to start downloads
     */
    public FileDetailFragment(OCFile fileToDetail, Account ocAccount) {
        super(fileToDetail);
        mAccount = ocAccount;
        mLayout = R.layout.file_details_empty;
        mProgressListener = null;
    }
    
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        if (savedInstanceState != null) {
            setFile((OCFile)savedInstanceState.getParcelable(FileActivity.EXTRA_FILE));
            mAccount = savedInstanceState.getParcelable(FileActivity.EXTRA_ACCOUNT);
        }
        
        if(getFile() != null && mAccount != null) {
            mLayout = R.layout.file_details_fragment;
        }
        
        View view = null;
        view = inflater.inflate(mLayout, null);
        mView = view;
        
        if (mLayout == R.layout.file_details_fragment) {
            mView.findViewById(R.id.fdKeepInSync).setOnClickListener(this);
            ProgressBar progressBar = (ProgressBar)mView.findViewById(R.id.fdProgressBar);
            mProgressListener = new ProgressListener(progressBar);
            mView.findViewById(R.id.fdCancelBtn).setOnClickListener(this);
        }
        
        updateFileDetails(false, false);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileActivity.EXTRA_FILE, getFile());
        outState.putParcelable(FileActivity.EXTRA_ACCOUNT, mAccount);
    }

    @Override
    public void onStart() {
        super.onStart();
        listenForTransferProgress();
    }
    
    @Override
    public void onStop() {
        leaveTransferProgress();
        super.onStop();
    }

    
    @Override
    public View getView() {
        return super.getView() == null ? mView : super.getView();
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.file_actions_menu, menu);
   }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        if (mContainerActivity.getStorageManager() != null) {
            FileMenuFilter mf = new FileMenuFilter(
                getFile(),
                mContainerActivity.getStorageManager().getAccount(),
                mContainerActivity,
                getSherlockActivity()
            );
            mf.filter(menu);
        }
        
        // additional restriction for this fragment 
        MenuItem item = menu.findItem(R.id.action_see_details);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share_file: {
                mContainerActivity.getFileOperationsHelper().shareFileWithLink(getFile());
                return true;
            }
            case R.id.action_unshare_file: {
                mContainerActivity.getFileOperationsHelper().unshareFileWithLink(getFile());
                return true;
            }
            case R.id.action_open_file_with: {
                mContainerActivity.getFileOperationsHelper().openFile(getFile());
                return true;
            }
            case R.id.action_remove_file: {
                RemoveFileDialogFragment dialog = RemoveFileDialogFragment.newInstance(getFile());
                dialog.show(getFragmentManager(), FTAG_CONFIRMATION);
                return true;
            }
            case R.id.action_rename_file: {
                RenameFileDialogFragment dialog = RenameFileDialogFragment.newInstance(getFile());
                dialog.show(getFragmentManager(), FTAG_RENAME_FILE);
                return true;
            }
            case R.id.action_cancel_download:
            case R.id.action_cancel_upload: {
                ((FileDisplayActivity)mContainerActivity).cancelTransference(getFile());
                return true;
            }
            case R.id.action_download_file: 
            case R.id.action_sync_file: {
                mContainerActivity.getFileOperationsHelper().syncFile(getFile());
                return true;
            }
            case R.id.action_send_file: {
                // Obtain the file
                if (!getFile().isDown()) {  // Download the file                    
                    Log_OC.d(TAG, getFile().getRemotePath() + " : File must be downloaded");
                    ((FileDisplayActivity)mContainerActivity).startDownloadForSending(getFile());
                    
                } else {
                    mContainerActivity.getFileOperationsHelper().sendDownloadedFile(getFile());
                }
                return true;
            }
            default:
                return false;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fdKeepInSync: {
                toggleKeepInSync();
                break;
            }
            case R.id.fdCancelBtn: {
                ((FileDisplayActivity)mContainerActivity).cancelTransference(getFile());
                break;
            }
            default:
                Log_OC.e(TAG, "Incorrect view clicked!");
        }
    }
    
    
    private void toggleKeepInSync() {
        CheckBox cb = (CheckBox) getView().findViewById(R.id.fdKeepInSync);
        OCFile file = getFile();
        file.setKeepInSync(cb.isChecked());
        mContainerActivity.getStorageManager().saveFile(file);
        
        /// register the OCFile instance in the observer service to monitor local updates
        Intent observedFileIntent = FileObserverService.makeObservedFileIntent(
                getActivity(),
                file, 
                mAccount,
                cb.isChecked());
        getActivity().startService(observedFileIntent);
        
        /// immediate content synchronization
        if (file.keepInSync()) {
            mContainerActivity.getFileOperationsHelper().syncFile(getFile());
        }
    }

    /**
     * Check if the fragment was created with an empty layout. An empty fragment can't show file details, must be replaced.
     * 
     * @return  True when the fragment was created with the empty layout.
     */
    public boolean isEmpty() {
        return (mLayout == R.layout.file_details_empty || getFile() == null || mAccount == null);
    }

    
    /**
     * Use this method to signal this Activity that it shall update its view.
     * 
     * @param file : An {@link OCFile}
     */
    public void updateFileDetails(OCFile file, Account ocAccount) {
        setFile(file);
        mAccount = ocAccount;
        updateFileDetails(false, false);
    }

    /**
     * Updates the view with all relevant details about that file.
     *
     * TODO Remove parameter when the transferring state of files is kept in database. 
     * 
     * @param transferring      Flag signaling if the file should be considered as downloading or uploading, 
     *                          although {@link FileDownloaderBinder#isDownloading(Account, OCFile)}  and 
     *                          {@link FileUploaderBinder#isUploading(Account, OCFile)} return false.
     *                          
     * @param refresh           If 'true', try to refresh the whole file from the database
     */
    public void updateFileDetails(boolean transferring, boolean refresh) {
        if (readyToShow()) {
            FileDataStorageManager storageManager = mContainerActivity.getStorageManager();
            if (refresh && storageManager != null) {
                setFile(storageManager.getFileByPath(getFile().getRemotePath()));
            }
            OCFile file = getFile();
            
            // set file details
            setFilename(file.getFileName());
            setFiletype(file.getMimetype(), file.getFileName());
            setFilesize(file.getFileLength());
            if(ocVersionSupportsTimeCreated()){
                setTimeCreated(file.getCreationTimestamp());
            }
           
            setTimeModified(file.getModificationTimestamp());
            
            CheckBox cb = (CheckBox)getView().findViewById(R.id.fdKeepInSync);
            cb.setChecked(file.keepInSync());

            // configure UI for depending upon local state of the file
            FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
            if (transferring || (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, file)) || (uploaderBinder != null && uploaderBinder.isUploading(mAccount, file))) {
                setButtonsForTransferring();
                
            } else if (file.isDown()) {
                
                setButtonsForDown();
                
            } else {
                // TODO load default preview image; when the local file is removed, the preview remains there
                setButtonsForRemote();
            }
        }
        getView().invalidate();
    }
    
    /**
     * Checks if the fragment is ready to show details of a OCFile
     *  
     * @return  'True' when the fragment is ready to show details of a file
     */
    private boolean readyToShow() {
        return (getFile() != null && mAccount != null && mLayout == R.layout.file_details_fragment);        
    }


    /**
     * Updates the filename in view
     * @param filename to set
     */
    private void setFilename(String filename) {
        TextView tv = (TextView) getView().findViewById(R.id.fdFilename);
        if (tv != null)
            tv.setText(filename);
    }

    /**
     * Updates the MIME type in view
     * @param mimetype to set
     * @param filename
     */
    private void setFiletype(String mimetype, String filename) {
        TextView tv = (TextView) getView().findViewById(R.id.fdType);
        if (tv != null) {
            String printableMimetype = DisplayUtils.convertMIMEtoPrettyPrint(mimetype);;        
            tv.setText(printableMimetype);
        }
        ImageView iv = (ImageView) getView().findViewById(R.id.fdIcon);
        if (iv != null) {
            iv.setImageResource(DisplayUtils.getResourceId(mimetype, filename));
        }
    }

    /**
     * Updates the file size in view
     * @param filesize in bytes to set
     */
    private void setFilesize(long filesize) {
        TextView tv = (TextView) getView().findViewById(R.id.fdSize);
        if (tv != null)
            tv.setText(DisplayUtils.bytesToHumanReadable(filesize));
    }
    
    /**
     * Updates the time that the file was created in view
     * @param milliseconds Unix time to set
     */
    private void setTimeCreated(long milliseconds){
        TextView tv = (TextView) getView().findViewById(R.id.fdCreated);
        TextView tvLabel = (TextView) getView().findViewById(R.id.fdCreatedLabel);
        if(tv != null){
            tv.setText(DisplayUtils.unixTimeToHumanReadable(milliseconds));
            tv.setVisibility(View.VISIBLE);
            tvLabel.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Updates the time that the file was last modified
     * @param milliseconds Unix time to set
     */
    private void setTimeModified(long milliseconds){
        TextView tv = (TextView) getView().findViewById(R.id.fdModified);
        if(tv != null){
            tv.setText(DisplayUtils.unixTimeToHumanReadable(milliseconds));
        }
    }
    
    /**
     * Enables or disables buttons for a file being downloaded
     */
    private void setButtonsForTransferring() {
        if (!isEmpty()) {
            // let's protect the user from himself ;)
            getView().findViewById(R.id.fdKeepInSync).setEnabled(false);
            
            // show the progress bar for the transfer
            getView().findViewById(R.id.fdProgressBlock).setVisibility(View.VISIBLE);
            TextView progressText = (TextView)getView().findViewById(R.id.fdProgressText);
            progressText.setVisibility(View.VISIBLE);
            FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
            if (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, getFile())) {
                progressText.setText(R.string.downloader_download_in_progress_ticker);
            } else if (uploaderBinder != null && uploaderBinder.isUploading(mAccount, getFile())) {
                progressText.setText(R.string.uploader_upload_in_progress_ticker);
            }
        }
    }

    /**
     * Enables or disables buttons for a file locally available 
     */
    private void setButtonsForDown() {
        if (!isEmpty()) {
            getView().findViewById(R.id.fdKeepInSync).setEnabled(true);
            
            // hides the progress bar
            getView().findViewById(R.id.fdProgressBlock).setVisibility(View.GONE);
            TextView progressText = (TextView)getView().findViewById(R.id.fdProgressText);
            progressText.setVisibility(View.GONE);
        }
    }

    /**
     * Enables or disables buttons for a file not locally available 
     */
    private void setButtonsForRemote() {
        if (!isEmpty()) {
            getView().findViewById(R.id.fdKeepInSync).setEnabled(true);
            
            // hides the progress bar
            getView().findViewById(R.id.fdProgressBlock).setVisibility(View.GONE);
            TextView progressText = (TextView)getView().findViewById(R.id.fdProgressText);
            progressText.setVisibility(View.GONE);
        }
    }
    

    /**
     * In ownCloud 3.X.X and 4.X.X there is a bug that SabreDAV does not return
     * the time that the file was created. There is a chance that this will
     * be fixed in future versions. Use this method to check if this version of
     * ownCloud has this fix.
     * @return True, if ownCloud the ownCloud version is supporting creation time
     */
    private boolean ocVersionSupportsTimeCreated(){
        /*if(mAccount != null){
            AccountManager accManager = (AccountManager) getActivity().getSystemService(Context.ACCOUNT_SERVICE);
            OwnCloudVersion ocVersion = new OwnCloudVersion(accManager
                    .getUserData(mAccount, AccountAuthenticator.KEY_OC_VERSION));
            if(ocVersion.compareTo(new OwnCloudVersion(0x030000)) < 0) {
                return true;
            }
        }*/
        return false;
    }
    

    public void listenForTransferProgress() {
        if (mProgressListener != null) {
            if (mContainerActivity.getFileDownloaderBinder() != null) {
                mContainerActivity.getFileDownloaderBinder().addDatatransferProgressListener(mProgressListener, mAccount, getFile());
            }
            if (mContainerActivity.getFileUploaderBinder() != null) {
                mContainerActivity.getFileUploaderBinder().addDatatransferProgressListener(mProgressListener, mAccount, getFile());
            }
        }
    }
    
    
    public void leaveTransferProgress() {
        if (mProgressListener != null) {
            if (mContainerActivity.getFileDownloaderBinder() != null) {
                mContainerActivity.getFileDownloaderBinder().removeDatatransferProgressListener(mProgressListener, mAccount, getFile());
            }
            if (mContainerActivity.getFileUploaderBinder() != null) {
                mContainerActivity.getFileUploaderBinder().removeDatatransferProgressListener(mProgressListener, mAccount, getFile());
            }
        }
    }


    
    /**
     * Helper class responsible for updating the progress bar shown for file uploading or downloading  
     * 
     * @author David A. Velasco
     */
    private class ProgressListener implements OnDatatransferProgressListener {
        int mLastPercent = 0;
        WeakReference<ProgressBar> mProgressBar = null;
        
        ProgressListener(ProgressBar progressBar) {
            mProgressBar = new WeakReference<ProgressBar>(progressBar);
        }
        
        @Override
        public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer, String filename) {
            int percent = (int)(100.0*((double)totalTransferredSoFar)/((double)totalToTransfer));
            if (percent != mLastPercent) {
                ProgressBar pb = mProgressBar.get();
                if (pb != null) {
                    pb.setProgress(percent);
                    pb.postInvalidate();
                }
            }
            mLastPercent = percent;
        }

    };

}
