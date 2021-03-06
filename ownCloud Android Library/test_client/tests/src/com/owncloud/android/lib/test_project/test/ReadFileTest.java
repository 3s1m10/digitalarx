/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud Inc.
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */
package com.owncloud.android.lib.test_project.test;

import java.io.File;

import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.test_project.TestActivity;

import android.test.ActivityInstrumentationTestCase2;

/**
 * Class to test Read File Operation
 * @author masensio
 * @author David A. Velasco
 */

public class ReadFileTest extends 	ActivityInstrumentationTestCase2<TestActivity> {
	
	private static final String LOG_TAG = ReadFileTest.class.getCanonicalName();
	
	private TestActivity mActivity;
	
	private String FILE_PATH = "/fileToRead.txt";
	
	public ReadFileTest() {
		super(TestActivity.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		setActivityInitialTouchMode(false);
		mActivity = getActivity();
		
		File textFile = mActivity.extractAsset(TestActivity.ASSETS__TEXT_FILE_NAME);
		RemoteOperationResult uploadResult = mActivity.uploadFile(
				textFile.getAbsolutePath(), 
				FILE_PATH, 
				"txt/plain");
		if (!uploadResult.isSuccess()) {
			Utils.logAndThrow(LOG_TAG, uploadResult);
		}
	}
	
	/**
	 * Test Read File
	 */
	public void testReadFile() {
		RemoteOperationResult result = mActivity.readFile(FILE_PATH);
		assertTrue(result.getData() != null && result.getData().size() ==  1);
		assertTrue(result.isSuccess());
		// TODO check more properties of the result
	}
	
	@Override
	protected void tearDown() throws Exception {
		RemoteOperationResult removeResult = mActivity.removeFile(FILE_PATH);
		if (!removeResult.isSuccess()) {
			Utils.logAndThrow(LOG_TAG, removeResult);
		}
		
		super.tearDown();
	}

}
