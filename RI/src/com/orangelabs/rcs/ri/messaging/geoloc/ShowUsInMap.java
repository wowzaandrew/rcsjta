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
package com.orangelabs.rcs.ri.messaging.geoloc;

import java.util.ArrayList;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.GeolocMessage;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Show us in a map
 */
public class ShowUsInMap extends MapActivity {
	/**
	 * Intent parameters
	 */
	public final static String EXTRA_CONTACTS = "contacts";

	/**
	 * Map view
	 */
	private MapView mapView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Set title
		setTitle(R.string.menu_showus_map);

		// Set layout
		setContentView(R.layout.geoloc_display);

		// Set map
		mapView = (MapView)findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapView.getController().setZoom(4);
		
		// Clear the list of overlay
		mapView.getOverlays().clear();
		mapView.invalidate();		
		
		// Create an overlay
		Drawable drawable = getResources().getDrawable(R.drawable.ri_map_icon);
		GeolocOverlay overlay = new GeolocOverlay(this, drawable);
		
		// Add an overlay item for each contact having a geoloc info		
		Geoloc lastGeoloc = null;
		ArrayList<String> contacts = getIntent().getStringArrayListExtra(EXTRA_CONTACTS);
		for (int i=0; i < contacts.size(); i++) {
			// Get geoloc of a contact
			String contact = contacts.get(i);
			
			//Get the last incoming geoloc for a contact
			Geoloc geoloc = getLastGeoloc(contact);
			if (geoloc != null) {
				// Add an overlay item
				overlay.addOverlayItem(contact, geoloc.getLabel(), geoloc.getLatitude(), geoloc.getLongitude(), geoloc.getAccuracy());
				lastGeoloc = geoloc;
			}
		}
		
		// Get my last geoloc
		Geoloc lastOutgoingGeoloc = getMyLastGeoloc();
		if (lastOutgoingGeoloc != null ) {
			// Add an overlay item
			overlay.addOverlayItem(getString(R.string.label_me), lastOutgoingGeoloc.getLabel(), lastOutgoingGeoloc.getLatitude(), lastOutgoingGeoloc.getLongitude(), lastOutgoingGeoloc.getAccuracy());				
		}
		
		if (overlay.size() == 0) {
			Utils.displayLongToast(this, getString(R.string.label_geoloc_not_found));
			return;
		}		
		
		// Add overlays to the map
		mapView.getOverlays().add(overlay);

		// Center the map
		if (lastGeoloc != null ) {
			mapView.getController().setCenter(new GeoPoint((int)(lastGeoloc.getLatitude() * 1E6), (int)(lastGeoloc.getLongitude() * 1E6)));
		}
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	/**
	 * Get the last incoming geoloc for a given contact
	 * 
	 * @param contact Contact
	 * @return Geoloc info
	 */
	public Geoloc getLastGeoloc(String contact) {
		Geoloc result = null;

		String sortOrder = ChatLog.Message.TIMESTAMP + " DESC ";
		String where = ChatLog.Message.CONTACT_NUMBER + "='" + PhoneNumberUtils.formatNumber(contact) + "' AND "
				+ ChatLog.Message.MIME_TYPE + " = '" + GeolocMessage.MIME_TYPE + "' AND "
				+ ChatLog.Message.DIRECTION + " = " + ChatLog.Message.Direction.INCOMING;
		Cursor cursor = getApplicationContext().getContentResolver().query(
				ChatLog.Message.CONTENT_URI,
				new String[] { ChatLog.Message.BODY }, where, null,
				sortOrder);

		if (cursor.moveToFirst()) {
    		byte[] content = cursor.getBlob(0);	
			result = ChatLog.getGeolocFromBlob(content);
		}

		return result;
	}

	/**
	 * Get my last geoloc
	 * 
	 * @return Geoloc info
	 */
	public Geoloc getMyLastGeoloc() {
		Geoloc result = null;

		String sortOrder = ChatLog.Message.TIMESTAMP + " DESC ";
		String where = ChatLog.Message.MIME_TYPE + " = '" + GeolocMessage.MIME_TYPE + "' AND "
				+ ChatLog.Message.DIRECTION + " = " + ChatLog.Message.Direction.OUTGOING;
		Cursor cursor = getApplicationContext().getContentResolver().query(
				ChatLog.Message.CONTENT_URI,
				new String[] { ChatLog.Message.BODY }, where, null,
				sortOrder);

		if (cursor.moveToFirst()) {
    		byte[] content = cursor.getBlob(0);		
			result = ChatLog.getGeolocFromBlob(content);
		}
		cursor.close();

		return result;
	}
}
