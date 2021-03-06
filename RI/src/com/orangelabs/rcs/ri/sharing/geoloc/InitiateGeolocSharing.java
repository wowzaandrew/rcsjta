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

package com.orangelabs.rcs.ri.sharing.geoloc;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.gsh.GeolocSharing;
import com.gsma.services.rcs.gsh.GeolocSharingListener;
import com.gsma.services.rcs.gsh.GeolocSharingService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.geoloc.DisplayGeoloc;
import com.orangelabs.rcs.ri.messaging.geoloc.EditGeoloc;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Initiate geoloc sharing
 * 
 * @author vfml3370
 */
public class InitiateGeolocSharing extends Activity implements JoynServiceListener {
	/**
	 * Activity result constants
	 */
	private final static int SELECT_GEOLOCATION = 0;

	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();
	
    /**
     * Progress dialog
     */
    private Dialog progressDialog = null;    
    
    /**
     * Geoloc info
     */
    private Geoloc geoloc;
    
	/**
	 * Geoloc sharing API
	 */
    private GeolocSharingService gshApi;
    
	/**
     * Geoloc sharing session
     */
    private GeolocSharing geolocSharing = null;
    
    /**
     * Remote contact
     */
    private String contact;
    
    /**
     * Geoloc sharing listener
     */
    private MyGeolocSharingListener gshListener = new MyGeolocSharingListener();        
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.geoloc_sharing_initiate);
        
        // Set title
        setTitle(R.string.menu_initiate_geoloc_sharing);
        
        // Set contact selector
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        spinner.setAdapter(Utils.createRcsContactListAdapter(this));

        // Set buttons callback
        Button inviteBtn = (Button)findViewById(R.id.invite_btn);
        inviteBtn.setOnClickListener(btnInviteListener);
        inviteBtn.setEnabled(false);
        Button selectBtn = (Button)findViewById(R.id.select_btn);
        selectBtn.setOnClickListener(btnSelectListener);
        selectBtn.setEnabled(false);
        Button dialBtn = (Button)findViewById(R.id.dial_btn);
        dialBtn.setOnClickListener(btnDialListener);
        dialBtn.setEnabled(false);        
        
        // Instanciate API
		gshApi = new GeolocSharingService(getApplicationContext(), this);
		
		// Connect API
		gshApi.connect();
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();

        // Remove geoloc sharing listener
        if (geolocSharing != null) {
        	try {
        		geolocSharing.removeEventListener(gshListener);
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
        }

        // Disconnect API
        gshApi.disconnect();
    }
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
        // Disable button if no contact available
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        Button dialBtn = (Button)findViewById(R.id.dial_btn);
        Button selectBtn = (Button)findViewById(R.id.select_btn);
        if (spinner.getAdapter().getCount() != 0) {
        	dialBtn.setEnabled(true);
        	selectBtn.setEnabled(true);
        }
    }
    
    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
		Utils.showMessageAndExit(InitiateGeolocSharing.this, getString(R.string.label_api_disabled));
    }      
    
    /**
     * Dial button listener
     */
    private OnClickListener btnDialListener = new OnClickListener() {
        public void onClick(View v) {
        	// Get the remote contact
            Spinner spinner = (Spinner)findViewById(R.id.contact);
            MatrixCursor cursor = (MatrixCursor)spinner.getSelectedItem();
            String remote = cursor.getString(1);

            // Initiate a GSM call before to be able to share content
            Intent intent = new Intent(Intent.ACTION_CALL);
        	intent.setData(Uri.parse("tel:"+remote));
            startActivity(intent);
        }
    };

    /**
     * Invite button listener
     */
    private OnClickListener btnInviteListener = new OnClickListener() {
        public void onClick(View v) {
            // Check if the service is available
        	boolean registered = false;
        	try {
        		if ((gshApi != null) && gshApi.isServiceRegistered()) {
        			registered = true;
        		}
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
            if (!registered) {
    	    	Utils.showMessage(InitiateGeolocSharing.this, getString(R.string.label_service_not_available));
    	    	return;
            }    
            
            // Get the remote contact
            Spinner spinner = (Spinner)findViewById(R.id.contact);
            MatrixCursor cursor = (MatrixCursor)spinner.getSelectedItem();
            contact = cursor.getString(1);
            final String remote = contact;

        	try {
                // Initiate location share
        		geolocSharing = gshApi.shareGeoloc(remote, geoloc, gshListener);
        	} catch(Exception e) {
        		e.printStackTrace();
				hideProgressDialog();
				Utils.showMessageAndExit(InitiateGeolocSharing.this, getString(R.string.label_invitation_failed));
        	}

            // Display a progress dialog
            progressDialog = Utils.showProgressDialog(InitiateGeolocSharing.this, getString(R.string.label_command_in_progress));            
            progressDialog.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					Toast.makeText(InitiateGeolocSharing.this, getString(R.string.label_sharing_cancelled), Toast.LENGTH_SHORT).show();
					quitSession();
				}
			});
            
            // Disable UI
            spinner.setEnabled(false);

            // Hide buttons
            Button inviteBtn = (Button)findViewById(R.id.invite_btn);
        	inviteBtn.setVisibility(View.INVISIBLE);
            Button selectBtn = (Button)findViewById(R.id.select_btn);
            selectBtn.setVisibility(View.INVISIBLE);
            Button dialBtn = (Button)findViewById(R.id.dial_btn);
            dialBtn.setVisibility(View.INVISIBLE);        	
        }
    };
       
    /**
     * Select location button listener
     */
    private OnClickListener btnSelectListener = new OnClickListener() {
        public void onClick(View v) {
    		// Start a new activity to send a geolocation
        	startActivityForResult(new Intent(InitiateGeolocSharing.this, EditGeoloc.class), SELECT_GEOLOCATION);
        }
    };

    /**
     * On activity result
     * 
     * @param requestCode Request code
     * @param resultCode Result code
     * @param data Data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (resultCode != RESULT_OK) {
    		return;
    	}
    	
        switch(requestCode) {
			case SELECT_GEOLOCATION: {
				// Get selected geoloc
				geoloc = data.getParcelableExtra(EditGeoloc.EXTRA_GEOLOC); 
				
                // Enable invite button
                Button inviteBtn = (Button)findViewById(R.id.invite_btn);
            	inviteBtn.setEnabled(true);  
			}             
            break;
        }
    }
    
	/**
	 * Hide progress dialog
	 */
    public void hideProgressDialog() {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
			progressDialog = null;
		}
    }       
    
    /**
     * Geoloc sharing event listener
     */
    private class MyGeolocSharingListener extends GeolocSharingListener {
    	// Sharing started
    	public void onSharingStarted() {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
					
					// Display session status
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("started");
				}
			});
    	}
    	
    	// Sharing aborted
    	public void onSharingAborted() {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
					
					// Display session status
					Utils.showMessageAndExit(InitiateGeolocSharing.this, getString(R.string.label_sharing_aborted));
				}
			});
    	}

    	// Sharing error
    	public void onSharingError(final int error) {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
					
					// Display error
                    if (error == GeolocSharing.Error.INVITATION_DECLINED) {
                        Utils.showMessageAndExit(InitiateGeolocSharing.this,
                                getString(R.string.label_sharing_declined));
                    } else {
                        Utils.showMessageAndExit(InitiateGeolocSharing.this,
                                getString(R.string.label_sharing_failed, error));
                    }
				}
			});
    	}
    	
    	// Sharing progress
    	public void onSharingProgress(final long currentSize, final long totalSize) {
			handler.post(new Runnable() { 
    			public void run() {
					// Display sharing progress
    				updateProgressBar(currentSize, totalSize);
    			}
    		});
    	}

    	// Geoloc shared
    	public void onGeolocShared(final Geoloc geoloc) {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();

					// Display sharing progress
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("transferred");
					
					// Make sure progress bar is at the end
			        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
			        progressBar.setProgress(progressBar.getMax());
					
			        // Show the shared geoloc
					Intent intent = new Intent(InitiateGeolocSharing.this, DisplayGeoloc.class);
			    	intent.putExtra(DisplayGeoloc.EXTRA_CONTACT, contact);
			    	intent.putExtra(DisplayGeoloc.EXTRA_GEOLOC, (Parcelable)geoloc);
					startActivity(intent);
				}
			});
    	}
    };
    
    /**
     * Show the sharing progress
     * 
     * @param currentSize Current size transferred
     * @param totalSize Total size to be transferred
     */
    private void updateProgressBar(long currentSize, long totalSize) {
    	TextView statusView = (TextView)findViewById(R.id.progress_status);
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
    	
		String value = "" + (currentSize/1024);
		if (totalSize != 0) {
			value += "/" + (totalSize/1024);
		}
		value += " Kb";
		statusView.setText(value);
	    
	    if (currentSize != 0) {
	    	double position = ((double)currentSize / (double)totalSize)*100.0;
	    	progressBar.setProgress((int)position);
	    } else {
	    	progressBar.setProgress(0);
	    }
    }    
    
    /**
     * Quit the session
     */
    private void quitSession() {
		// Stop session
		try {
	        if (geolocSharing != null) {
	        	geolocSharing.removeEventListener(gshListener);
	        	geolocSharing.abortSharing();
	        }
		} catch(Exception e) {
			e.printStackTrace();
		}
		geolocSharing = null;
		
	    // Exit activity
		finish();
    }    
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            	// Quit the session
            	quitSession();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater=new MenuInflater(getApplicationContext());
		inflater.inflate(R.menu.menu_geoloc_sharing, menu);
		return true;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_close_session:
				// Quit the session
				quitSession();
				break;
		}
		return true;
	}    
}    
