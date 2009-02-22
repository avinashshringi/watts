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

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Path;
import android.util.Log;

// should make this class cache the map
// then have an Update() call that will append any new data to the end of the map
// should be very easy to do and speed things up...

class BatteryData {
	private static final String TAG = "BatteryData";
	
	private DatabaseHelper mOpenHelper;
	private SQLiteDatabase db;
	
	SortedMap<Long, Float> batteryData;
	
	BatteryData(Context context) {
		mOpenHelper = new DatabaseHelper(context);
		batteryData = Collections.synchronizedSortedMap(new TreeMap<Long, Float>());
	}
	
	protected SortedMap<Long, Float> getBatteryData() {
		return batteryData;
	}
	
	// returns a SortedMap of data to the requested time resolution
	protected SortedMap<Long, Float> buildDataMap(long start, long end, int timeResolution) {
		Log.d(TAG, String.format("buildDataMap(%d, %d, %d)", start, end, timeResolution));
		long  x;
		float y;
		int sampletime;
		int batterylvl;
		
		SortedMap<Long, Float> newBatteryData = new TreeMap<Long, Float>();
		
		Cursor dataCursor = null;
		SQLiteQueryBuilder qb = null;
		
		qb = new SQLiteQueryBuilder();
		qb.setTables(DatabaseHelper.LEVELS_TABLE);
		
		// sampletime is in milliseconds, that's a bit OTT so bring it down to seconds
		String[] columns = {"(sampletime/1000) as unixtime", "(avg(level)/100) as lvl"};
		
		// group by the time resolution, helps reduce points to speed up graphing
		String groupby = String.format("unixtime/%d", timeResolution);
		
		String where;
		// only data within the window start and end
		if (end == 0) {
			where = String.format("sampletime >= %d", start);
		} else {
			where = String.format("sampletime >= %d and sampletime <= %d", start, end);
		}
		
		try {
			db = mOpenHelper.getReadableDatabase();
		} catch (Exception e) {
			Log.e(TAG, "database exception in BatteryMap(): " + e);
		}
		
		try {
			dataCursor = qb.query(db, columns, where, null, groupby, null, "sampletime DESC", null);
			sampletime = dataCursor.getColumnIndex("unixtime");
			batterylvl = dataCursor.getColumnIndex("lvl");
			dataCursor.moveToFirst();
		} catch (Exception e) {
			Log.e(TAG, "Query exception: " + e);
			return null;
		}
		
		if (dataCursor.getCount() > 0) {
			x = dataCursor.getLong(sampletime);
			y = dataCursor.getFloat(batterylvl);
			newBatteryData.put(x, y);

			while(dataCursor.moveToNext()) {
				x = dataCursor.getLong(sampletime);
				y = dataCursor.getFloat(batterylvl);
				newBatteryData.put(x, y);
			}
		} else {
			Log.d(TAG, "No samples yet");
		}
		dataCursor.close();
		db.close();
		batteryData.clear();
		batteryData.putAll(newBatteryData);
		return batteryData;
	}
	
	public float xPosToTime(float x, int width) {
		long now = System.currentTimeMillis() / 1000;
		long first;
		try {
			first = batteryData.firstKey();
		} catch (Exception e) {
			return 0;
		}
		float scale = (float)(now - first) / (float)width;
		return first + x*scale;
	}
	
	// return a line array of this data rescaled to fit required dimensions
	
	float[] getLines(int width, int height) {
		float[] lines = new float[batteryData.size()*4];
		int index = 0;
		long now = System.currentTimeMillis() / 1000;
		long first;
		
		try {
			first = batteryData.firstKey();
		} catch (NoSuchElementException e) {
			return null;
		}
		float scale = (float)(now - first) / (float)width;

		Log.d(TAG, "batteryData.size=" + batteryData.size() + " scale=" + scale);

		for (long key : batteryData.keySet()) {
			if (index == 0) {
				lines[index++] = (key-first)/scale;
				lines[index++] = height - (batteryData.get(key) * height);
			} else {
				lines[index++] = (key-first)/scale;
				lines[index++] = height - (batteryData.get(key) * height);
				lines[index++] = (key-first)/scale;
				lines[index++] = height - (batteryData.get(key) * height);
				if (((key-first)/scale) > width) {
					Log.d(TAG, "damnit! we went off the edge of our specifed size: " + (key-first)/scale);
				}
			}
		}
		lines[index++] = width;
		lines[index++] = height - (batteryData.get(batteryData.lastKey()) * height);

		return lines;
	}
	
	int size() {
		return batteryData.size();
	}
	
	Path getPath(int width, int height) {
		Path p = new Path();
		long now = System.currentTimeMillis() / 1000;
		long first;
		
		try {
			first = batteryData.firstKey();
		} catch (NoSuchElementException e) {
			return null;
		}
		float scale = (float)(now - first) / (float)width;
		
		Log.d(TAG, "batteryData.size=" + batteryData.size() + " scale=" + scale);
		
		for (long key : batteryData.keySet()) {
			if (key == batteryData.firstKey()) {
				p.moveTo((key-first)/scale, height - (batteryData.get(key) * height));
			} else {
				p.lineTo((key-first)/scale, height - (batteryData.get(key) * height));
			}
		}
		
		p.lineTo(width, height - (batteryData.get(batteryData.lastKey()) * height));
		
		return p;
	}
}
