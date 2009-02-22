package au.id.weston.scott.Watts;

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
