/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/
package com.orangelabs.rcs.provider.fthttp;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * @author YPLO6403
 * 
 *         Implementation of interface to get access to FT HTTP data objects
 * 
 */
public class FtHttpResumeDaoImpl implements FtHttpResumeDao {

	private static final String SELECTION_FILE_BY_FT_ID = new StringBuilder(
			FtHttpColumns.FT_ID).append("=?").toString();

	/**
	 * Current instance
	 */
	private static FtHttpResumeDaoImpl instance = null;

	/**
	 * The logger
	 */
	final private static Logger logger = Logger.getLogger(FtHttpResumeDaoImpl.class.getSimpleName());

	/**
	 * Content resolver
	 */
	private ContentResolver cr;

	private FtHttpResumeDaoImpl(Context context) {
		this.cr = context.getContentResolver();
	}

	/**
	 * Creates an interface to get access to Data Object FtHttpResume
	 * 
	 * @param ctx
	 *            the {@code context} value.
	 * @return Instance of FtHttpResumeDaoImpl
	 */
	public static synchronized FtHttpResumeDaoImpl createInstance(Context ctx) {
		if (instance == null) {
			instance = new FtHttpResumeDaoImpl(ctx);
		}
		return instance;
	}

	/**
	 * Returns instance of DAO FtHttpResume
	 * 
	 * @return Instance
	 */
	public static FtHttpResumeDaoImpl getInstance() {
		return instance;
	}

	@Override
	public List<FtHttpResume> queryAll() {
		ArrayList<FtHttpResume> result = new ArrayList<FtHttpResume>();
		Cursor cursor = null;
		try {
			cursor = cr.query(FtHttpColumns.CONTENT_URI, FtHttpColumns.FULL_PROJECTION, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				int sizeColumnIdx = cursor.getColumnIndexOrThrow(FtHttpColumns.SIZE);
				int mimeTypeColumnIdx = cursor.getColumnIndexOrThrow(FtHttpColumns.TYPE);
				int contactColumnIdx = cursor.getColumnIndexOrThrow(FtHttpColumns.CONTACT);
				int chatIdColumnIdx = cursor.getColumnIndexOrThrow(FtHttpColumns.CHATID);
				int fileColumnIdx = cursor.getColumnIndexOrThrow(FtHttpColumns.FILEPATH);
				int directionColumnIdx = cursor.getColumnIndexOrThrow(FtHttpColumns.DIRECTION);
				int displayNameColumnIdx = cursor.getColumnIndexOrThrow(FtHttpColumns.DISPLAY_NAME);
				int fileTransferIdColumnIdx = cursor.getColumnIndexOrThrow(FtHttpColumns.FT_ID);
				int thumbnailColumnIdx = cursor.getColumnIndexOrThrow(FtHttpColumns.THUMBNAIL);
				int isGroupColumnIdx = cursor.getColumnIndexOrThrow(FtHttpColumns.IS_GROUP);
				int chatSessionIdColumnIdx = cursor.getColumnIndexOrThrow(FtHttpColumns.CHAT_SESSION_ID);
				int urlColumnIdx = cursor.getColumnIndexOrThrow(FtHttpColumns.IN_URL);
				int tidColumnIdx = cursor.getColumnIndexOrThrow(FtHttpColumns.OU_TID);
				do {
					long size = cursor.getLong(sizeColumnIdx);
					String mimeType = cursor.getString(mimeTypeColumnIdx);
					String contact = cursor.getString(contactColumnIdx);
					String chatId = cursor.getString(chatIdColumnIdx);
					String file = cursor.getString(fileColumnIdx);
					int direction = cursor.getInt(directionColumnIdx);
					String displayName = cursor.getString(displayNameColumnIdx);
					String fileTransferId = cursor.getString(fileTransferIdColumnIdx);
					String thumbnail = cursor.getString(thumbnailColumnIdx);
					boolean isGroup = cursor.getInt(isGroupColumnIdx) != 0;
					String chatSessionId = cursor.getString(chatSessionIdColumnIdx);
					if (FtHttpDirection.values()[direction] == FtHttpDirection.INCOMING) {
						String url = cursor.getString(urlColumnIdx);
						MmContent content = ContentManager.createMmContentFromMime(url, mimeType, size);
						String messageId = cursor.getString(13);
						result.add(new FtHttpResumeDownload(file, thumbnail, content, contact, displayName, chatId,
								fileTransferId, chatSessionId, isGroup));
					} else {
						String tid = cursor.getString(tidColumnIdx);
						MmContent content = ContentManager.createMmContentFromMime(file, mimeType, size);
						result.add(new FtHttpResumeUpload(content, thumbnail, tid, contact, displayName, chatId, fileTransferId,
 								chatSessionId, isGroup));
					}
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error(e.getMessage(), e);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	@Override
	public Uri insert(FtHttpResume ftHttpResume) {
		ContentValues values = new ContentValues();
		values.put(FtHttpColumns.DATE, System.currentTimeMillis());
		values.put(FtHttpColumns.DIRECTION, ftHttpResume.getDirection().ordinal());
		values.put(FtHttpColumns.FILEPATH, ftHttpResume.getFilepath());
		values.put(FtHttpColumns.TYPE, ftHttpResume.getMimetype());
		values.put(FtHttpColumns.SIZE, ftHttpResume.getSize());
		values.put(FtHttpColumns.THUMBNAIL, ftHttpResume.getThumbnail());
		values.put(FtHttpColumns.CONTACT, ftHttpResume.getContact());
		values.put(FtHttpColumns.DISPLAY_NAME, ftHttpResume.getDisplayName());
		values.put(FtHttpColumns.CHATID, ftHttpResume.getChatId());
		values.put(FtHttpColumns.FT_ID, ftHttpResume.getFileTransferId());
		values.put(FtHttpColumns.CHAT_SESSION_ID, ftHttpResume.getChatSessionId());
		values.put(FtHttpColumns.IS_GROUP, ftHttpResume.isGroup());
		if (ftHttpResume instanceof FtHttpResumeDownload) {
			FtHttpResumeDownload download = (FtHttpResumeDownload) ftHttpResume;
			values.put(FtHttpColumns.IN_URL, download.getUrl());
			if (logger.isActivated()) {
				logger.debug("insert " + download + ")");
			}
		} else if (ftHttpResume instanceof FtHttpResumeUpload) {
			FtHttpResumeUpload upload = (FtHttpResumeUpload) ftHttpResume;
			values.put(FtHttpColumns.OU_TID, upload.getTid());
			if (logger.isActivated()) {
				logger.debug("insert " + upload + ")");
			}
		} else {
			return null;
		}
		return cr.insert(FtHttpColumns.CONTENT_URI, values);
	}

	@Override
	public int deleteAll() {
		return cr.delete(FtHttpColumns.CONTENT_URI, null, null);
	}

	@Override
	public int delete(FtHttpResume ftHttpResume) {
		if (logger.isActivated()) {
			logger.debug("delete " + ftHttpResume);
		}
		return cr.delete(FtHttpColumns.CONTENT_URI, SELECTION_FILE_BY_FT_ID, new String[] {
			ftHttpResume.getFileTransferId()
		});
	}

	@Override
	public FtHttpResumeUpload queryUpload(String tid) {
		String selection = FtHttpColumns.OU_TID + " = ? AND " + FtHttpColumns.DIRECTION + " = ?";
		String[] selectionArgs = { tid, "" + FtHttpDirection.OUTGOING.ordinal() };
		Cursor cursor = null;
		try {
			cursor = cr.query(FtHttpColumns.CONTENT_URI, FtHttpColumns.FULL_PROJECTION, selection, selectionArgs, "_ID LIMIT 1");
			if (cursor != null) {
				if (cursor.moveToNext()) {
					long size = cursor.getLong(cursor.getColumnIndexOrThrow(FtHttpColumns.SIZE));
					String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(FtHttpColumns.TYPE));
					String contact = cursor.getString(cursor.getColumnIndexOrThrow(FtHttpColumns.CONTACT));
					String chatId = cursor.getString(cursor.getColumnIndexOrThrow(FtHttpColumns.CHATID));
					String file = cursor.getString(cursor.getColumnIndexOrThrow(FtHttpColumns.FILEPATH));
					String displayName = cursor.getString(cursor.getColumnIndexOrThrow(FtHttpColumns.DISPLAY_NAME));
					String fileTransferId = cursor.getString(cursor.getColumnIndexOrThrow(FtHttpColumns.FT_ID));
					String thumbnail = cursor.getString(cursor.getColumnIndexOrThrow(FtHttpColumns.THUMBNAIL));
					boolean isGroup = cursor.getInt(cursor.getColumnIndexOrThrow(FtHttpColumns.IS_GROUP)) != 0;
					String chatSessionId = cursor.getString(cursor.getColumnIndexOrThrow(FtHttpColumns.CHAT_SESSION_ID));
					MmContent content = ContentManager.createMmContentFromMime(file, mimeType, size);
					return new FtHttpResumeUpload(content, thumbnail, tid, contact, displayName, chatId, fileTransferId,
 							chatSessionId, isGroup);
				}
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error(e.getMessage(), e);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}

	@Override
	public FtHttpResumeDownload queryDownload(String url) {
		String selection = FtHttpColumns.IN_URL + " = ? AND " + FtHttpColumns.DIRECTION + " = ?";
		String[] selectionArgs = { url, "" + FtHttpDirection.INCOMING.ordinal() };
		Cursor cursor = null;
		try {
			cursor = cr.query(FtHttpColumns.CONTENT_URI, FtHttpColumns.FULL_PROJECTION, selection, selectionArgs, "_ID LIMIT 1");
			if (cursor != null) {
				if (cursor.moveToNext()) {
					long size = cursor.getLong(cursor.getColumnIndexOrThrow(FtHttpColumns.SIZE));
					String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(FtHttpColumns.TYPE));
					String contact = cursor.getString(cursor.getColumnIndexOrThrow(FtHttpColumns.CONTACT));
					String chatId = cursor.getString(cursor.getColumnIndexOrThrow(FtHttpColumns.CHATID));
					String file = cursor.getString(cursor.getColumnIndexOrThrow(FtHttpColumns.FILEPATH));
					String displayName = cursor.getString(cursor.getColumnIndexOrThrow(FtHttpColumns.DISPLAY_NAME));
					String fileTransferId = cursor.getString(cursor.getColumnIndexOrThrow(FtHttpColumns.FT_ID));
					String thumbnail = cursor.getString(cursor.getColumnIndexOrThrow(FtHttpColumns.THUMBNAIL));
					boolean isGroup = cursor.getInt(cursor.getColumnIndexOrThrow(FtHttpColumns.IS_GROUP)) != 0;
					String chatSessionId = cursor.getString(cursor.getColumnIndexOrThrow(FtHttpColumns.CHAT_SESSION_ID));
					MmContent content = ContentManager.createMmContentFromMime(url, mimeType, size);
					return new FtHttpResumeDownload(file, thumbnail, content, contact, displayName, chatId, fileTransferId,
 							chatSessionId, isGroup);
				}
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error(e.getMessage(), e);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}
}
