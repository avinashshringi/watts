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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class WattsService extends Service {
	private static final String TAG = "WattsService";
	private static int lastlevel = 100;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
        IntentFilter batteryFilter = new IntentFilter();
        batteryFilter.addAction(android.content.Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mIntentReceiver, batteryFilter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mIntentReceiver);
	}
	
	BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(android.content.Intent.ACTION_BATTERY_CHANGED)) {
				Log.d(TAG, "ACTION_BATTERY_CHANGED");
				Bundle extras = intent.getExtras();
				Log.d(TAG, "keys: " + extras.keySet());
				ContentValues values = new ContentValues();
				Long now = Long.valueOf(System.currentTimeMillis());
				values.put("sampletime", now);
				values.put("level", extras.getInt("level"));
				values.put("voltage", extras.getInt("voltage"));
				values.put("temperature", extras.getInt("temperature"));
				values.put("plugged", extras.getInt("plugged"));
				insertBatteryData(values);
				// if we're not charging
				if (extras.getInt("plugged") != 2) {
					warnUser(extras.getInt("level"));
				}
				lastlevel = extras.getInt("level");
			}
		}
	};
	
	public void warnUser(int level) {
		SharedPreferences settings = getSharedPreferences(Watts.PREFS_NAME, 0);
        boolean sndEnabled = settings.getBoolean("sndEnabled", false);
        if (!sndEnabled) return;

        if (level <= 15) {
			MediaPlayer mp = null;

			switch(level) {
			case 15:
			case 14:
			case 13:
			case 12:
			case 11:
				if (lastlevel > 15)	mp = MediaPlayer.create(this, R.raw.start);
				break;
			case 10:
			case 9:
			case 8:
			case 7:
			case 6:
				if (lastlevel > 10) mp = MediaPlayer.create(this, R.raw.middle);
				break;
			case 5:
			case 4:
			case 3:
			case 2:
			case 1:
				mp = MediaPlayer.create(this, R.raw.end);
				break;
			}
			if (mp != null)	mp.start();
		}
	}

	public boolean insertBatteryData(ContentValues values) {
		Log.d(TAG, "insertBatteryData: " + values);
		
		DatabaseHelper mOpenHelper = null;
		SQLiteDatabase db = null;
		boolean rc = true;
		
		try {
			mOpenHelper = new DatabaseHelper(this);
			db = mOpenHelper.getReadableDatabase();
			Long rowid = db.insert(DatabaseHelper.LEVELS_TABLE, "watts", values);
			if (rowid < 0) {
				Log.e(TAG, "database insert failed: " + rowid);
				rc = false;
			} else {
				Log.d(TAG, "sample collected, rowid=" + rowid);
			}
		} catch (Exception e) {
			Log.e(TAG, "database exception");
			rc = false;
		}
		if (db != null) db.close();
		if (mOpenHelper != null) mOpenHelper.close();
		return rc;
	}
}
