package au.id.weston.scott.Watts;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class DatabaseHelper extends SQLiteOpenHelper {
	static final String TAG = "Watts/DatabaseHelper";
	static final String DATABASE_NAME = "watts.db";
	static final int DATABASE_VERSION = 1;
	static final String LEVELS_TABLE = "levels";
	
	DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "DatabaseHelper onCreate called");
		db.execSQL("CREATE TABLE " + LEVELS_TABLE + " ("
				+ "sampletime INTEGER PRIMARY KEY,"
				+ "level INTEGER,"
				+ "voltage INTEGER,"
				+ "temperature INTEGER,"
				+ "plugged INTEGER"
				+ ");");
	}

	@Override
	public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "database upgrade requested.  ignored.");
	}
}