package au.id.weston.scott.Watts;

// Copyright (c) 2009, Scott Weston <scott@weston.id.au>
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// 
//     * Redistributions of source code must retain the above copyright notice,
//       this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above copyright notice,
//       this list of conditions and the following disclaimer in the documentation
//       and/or other materials provided with the distribution.
//     * Neither the name of Scott Weston nor the names of contributors may be
//       used to endorse or promote products derived from this software without
//       specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
// ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.tts.TTS;

public class Watts extends Activity {
	
	private static final String TAG = "Watts";
	public static final String PREFS_NAME = "WattsPrefsFile";
	
	private WattsGrapher mView;
	private boolean ttsEnabled;
	private boolean ttsPaused = false;
	private TTS myTts;
	private Menu mMenu;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        Log.d(TAG, "allocating new view");
        
        GraphView gv;
        gv = new GraphView(this);
        setContentView(gv);
        
        // if we were just installed and run we'll need our service started
		startService(new Intent(this, WattsService.class));
		
		// we want battery change intents so as to tell the view to redraw
		IntentFilter batteryFilter = new IntentFilter();
        batteryFilter.addAction(android.content.Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mIntentReceiver, batteryFilter);
        
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        
        boolean ttsEnabled = settings.getBoolean("ttsEnabled", false);
        if (ttsEnabled) {
        	myTts = new TTS(this, ttsInitListener, true);
        }
        mView.setTimeWindow(settings.getLong("timeWindow", 7*60*60*1000));
        mView.setResolution(settings.getInt("resolution", 600));
        mView.setHeight(1024);
        mView.setWidth(1024);
        mView.allocate();

        if (!settings.getBoolean("info_1_2_0", false)) {
        	showAboutDialog();
        }
        
        Log.d(TAG, String.format("prefs: ttsEnabled=%s, timeWindow=%d, resolution=%d", 
        		ttsEnabled ? "true" : "false", 
        				mView.getTimeWindow(), 
        				mView.getResolution()));
    }
	
	private TTS.InitListener ttsInitListener = new TTS.InitListener() {
		public void onInit(int version) {
			ttsEnabled = true;
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch(keyCode) {
			case 8:
				mView.setResolution(300);
				return true;
			case 9:
				mView.setResolution(600);
				return true;
			case 10:
				mView.setResolution(1200);
				return true;
			case 11:
				mView.setResolution(2400);
				return true;
			case 12:
				mView.setResolution(3600);
				return true;
			case 13:
				mView.setResolution(4800);
				return true;
			case 14:
				mView.setResolution(9600);
				return true;
			case 15:
				mView.setResolution(14400);
				return true;
			case 16:
				mView.setResolution(24000);
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(android.content.Intent.ACTION_BATTERY_CHANGED)) {
				Log.d(TAG, "ACTION_BATTERY_CHANGED");
				Bundle extras = intent.getExtras();
				if (!ttsPaused && ttsEnabled && myTts != null)
					myTts.speak(String.format("battery %s, %d percent %s",
						extras.getInt("plugged") == 2 ? "charging" : "discharging",
						extras.getInt("level"),
						extras.getInt("plugged") == 2 ? "charged" : "remaining"), 1, null);
				mView.interruptDrawingThread();
			}
		}
	};
	
	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy()");
		unregisterReceiver(mIntentReceiver);
		mView.destroyDrawingThread();
		if (myTts != null) myTts.shutdown();
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause()");
		mView.pauseDrawingThread(true);
		ttsPaused = true;
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume()");
		mView.pauseDrawingThread(false);
		ttsPaused = false;
		super.onResume();
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "onStart()");
		mView.startDrawingThread();
		ttsPaused = false;
		super.onStart();
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "onStop()");
		mView.pauseDrawingThread(true);
		savePrefs();
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(TAG, "onCreateOptionsMenu");
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.layout.menu, menu);
		mMenu = menu;
		
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		MenuItem tts = mMenu.findItem(R.id.enabletts);
		tts.setChecked(settings.getBoolean("ttsEnabled", false));
		MenuItem snd = mMenu.findItem(R.id.enablesnd);
		snd.setChecked(settings.getBoolean("sndEnabled", false));
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.show1:
			mView.setTimeWindow(24*60*60*1000);
			return true;
		case R.id.show2:
			mView.setTimeWindow(2*24*60*60*1000);
			return true;
		case R.id.show3:
			mView.setTimeWindow(3*24*60*60*1000);
			return true;
		case R.id.show4:
			mView.setTimeWindow(4*24*60*60*1000);
			return true;
		case R.id.show5:
			mView.setTimeWindow(5*24*60*60*1000);
			return true;
		case R.id.showAllTime:
			long now = System.currentTimeMillis();
			mView.setTimeWindow(now);
			return true;
		case R.id.enabletts:
			if (ttsEnabled == false) {
				ttsEnabled = true;
				Toast.makeText(this, 
						"Press back and restart Watts to enable text-to-speech", 
						Toast.LENGTH_LONG).show();
			} else {
				if (myTts != null) {
					myTts.shutdown();
					myTts = null;
				}
				ttsEnabled = false;
				Toast.makeText(this, 
						"Text-to-speech disabled", 
						Toast.LENGTH_LONG).show();
			}
			MenuItem tts = mMenu.findItem(R.id.enabletts);
			tts.setChecked(ttsEnabled);
			return true;
		case R.id.enablesnd:
			MenuItem snd = mMenu.findItem(R.id.enablesnd);
			snd.setChecked(!snd.isChecked());
			savePrefs();
			return true;
		case R.id.about:
			showAboutDialog();
			return true;
		case R.id.finish:
			finish();
			return true;
		}
		return false;
	}
	
	void showAboutDialog() {
    	new AlertDialog.Builder(this)
    	.setIcon(R.drawable.watts)
    	.setTitle("Welcome!")
    	.setMessage("New in this version:" +
    			"\n - touch screen scrolling" +
    			"\n - text-to-speech support" +
    			"\n\nPlease be aware that this is a learning project for me and I am not a professional Java developer, so expect bugs." +
    			"\n\nIn this version I have completely re-written the graphing functions, they are still not optimised so at times it may take a few seconds to update the screen, hopefully I'll get that fixed soon, but in the meantime please be patient with it." +
    			"\n\nIf you find a bug that you can replicate please log it as an issue at:" +
    			"\nhttp://watts.googlecode.com/" +
    			"\n\nThanks!\n")
    	.setPositiveButton("ok", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    			/* User clicked OK  */
    		}
    	})
    	.create().show();
	}
	
	private void savePrefs() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();

		editor.putBoolean("info_1_2_0", true);
		editor.putLong("timeWindow", mView.getTimeWindow());
		editor.putInt("resolution", mView.getResolution());
		if (mMenu != null) {
			MenuItem tts = mMenu.findItem(R.id.enabletts);
			editor.putBoolean("ttsEnabled", tts.isChecked());
			MenuItem snd = mMenu.findItem(R.id.enablesnd);
			editor.putBoolean("sndEnabled", snd.isChecked());
		}
		editor.commit();
	}
	
	private class GraphView extends LinearLayout {
		TextView mAxisX;
		
		public GraphView(Context context) {
			super(context);
			
			this.setOrientation(VERTICAL);
			setBackgroundColor(Color.WHITE);
			
			mAxisX = new TextView(context);
	        mAxisX.setText("touch the graph to see time at that point");
	        mAxisX.setBackgroundColor(Color.BLUE);
	        mAxisX.setTextColor(Color.WHITE);
	        mAxisX.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD), Typeface.BOLD);
	        mAxisX.setGravity(Gravity.CENTER_HORIZONTAL);
	        mAxisX.setPadding(0, 5, 0, 5);
	        addView(mAxisX, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			
			mView = new WattsGrapher(context, mAxisX);
	        addView(mView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	        mView.setBackgroundColor(Color.YELLOW);
		}
	}
}
