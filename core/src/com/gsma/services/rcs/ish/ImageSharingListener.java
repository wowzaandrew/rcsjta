/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.gsma.services.rcs.ish;


/**
 * Image sharing event listener
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class ImageSharingListener extends IImageSharingListener.Stub {
	/**
	 * Callback called when the sharing is started
	 */
	public abstract void onSharingStarted();
	
	/**
	 * Callback called when the sharing has been aborted
	 */
	public abstract void onSharingAborted();

	/**
	 * Callback called when the sharing has failed
	 * 
	 * @param error Error
	 * @see ImageSharing.Error
	 */
	public abstract void onSharingError(int error);
	
	/**
	 * Callback called during the sharing progress
	 * 
	 * @param currentSize Current transferred size in bytes
	 * @param totalSize Total size to transfer in bytes
	 */
	public abstract void onSharingProgress(long currentSize, long totalSize);

	/**
	 * Callback called when the image has been shared
	 * 
	 * @param filename Filename including the path of the transferred file
	 */
	public abstract void onImageShared(String filename);
}
