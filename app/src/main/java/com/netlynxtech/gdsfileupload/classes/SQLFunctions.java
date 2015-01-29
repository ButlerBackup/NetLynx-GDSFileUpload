package com.netlynxtech.gdsfileupload.classes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class SQLFunctions {
    public static final String TAG = "GDS[SQLi]";
    public static final String GLOBAL_ROWID = "_id";

    private static final String DATABASE_NAME = "gdsfileupload";
    private static final String TABLE_TIMELINE = "timeline";
    private static final String TABLE_TIMELINE_LOCATION = "timelineLocation";
    private static final String TABLE_TIMELINE_LOCATION_LAT = "timelineLocationLat";
    private static final String TABLE_TIMELINE_LOCATION_LONG = "timelineLocationLong";
    private static final String TABLE_TIMELINE_UNIX = "timelineTime";
    private static final String TABLE_TIMELINE_IMAGE = "timelineImage";
    private static final String TABLE_TIMELINE_VIDEO = "timelineVideo";
    private static final String TABLE_TIMELINE_MESSAGE = "timelineMessage";
    private static final String TABLE_TIMELINE_SUCCESS = "timelineSuccess";

    private static final int DATABASE_VERSION = 2;

    private DbHelper ourHelper;
    private final Context ourContext;
    private SQLiteDatabase ourDatabase;

    private static class DbHelper extends SQLiteOpenHelper {

        public DbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_TIMELINE + " (" + GLOBAL_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + TABLE_TIMELINE_UNIX + " TEXT NOT NULL, "
                    + TABLE_TIMELINE_MESSAGE + " TEXT NOT NULL, " + TABLE_TIMELINE_IMAGE + " TEXT NOT NULL, " + TABLE_TIMELINE_VIDEO + " TEXT NOT NULL, "
                    + TABLE_TIMELINE_LOCATION + " TEXT NOT NULL, " + TABLE_TIMELINE_LOCATION_LAT + " TEXT NOT NULL, " + TABLE_TIMELINE_LOCATION_LONG + " TEXT NOT NULL, " + TABLE_TIMELINE_SUCCESS + " TEXT NOT NULL);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            switch (oldVersion) {
                case 1:
                    db.execSQL("ALTER TABLE " + TABLE_TIMELINE + " ADD COLUMN " + TABLE_TIMELINE_SUCCESS + " TEXT");
                    break;
            }
        }
    }

    public SQLFunctions(Context c) {
        ourContext = c;
    }

    public SQLFunctions open() throws SQLException {
        ourHelper = new DbHelper(ourContext);
        ourDatabase = ourHelper.getWritableDatabase();
        return null;
    }

    public void close() {
        if (ourHelper != null) {
            ourHelper.close();
        } else {
            Log.e(TAG, "You did not open your database. Null error");
        }
    }

    public long unixTime() {
        return System.currentTimeMillis() / 1000L;
    }

    public boolean deleteTimelineItem(String id) {
        return ourDatabase.delete(TABLE_TIMELINE, GLOBAL_ROWID + "=" + id, null) > 0;
    }

    public boolean longerThanTwoHours(String pTime) {
        int prevTime = Integer.parseInt(pTime);
        int currentTime = (int) (System.currentTimeMillis() / 1000L);
        int seconds = currentTime - prevTime;
        int how_many;
        if (seconds > 3600 && seconds < 86400) {
            how_many = (int) seconds / 3600;
            if (how_many >= 2) { // 2 hours
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public String getLastRowId() {
        String sql = "SELECT * FROM " + TABLE_TIMELINE + " ORDER BY " + GLOBAL_ROWID + " DESC LIMIT 1";
        Cursor cursor = ourDatabase.rawQuery(sql, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String id = cursor.getString(cursor.getColumnIndex(GLOBAL_ROWID));
                cursor.close();
                Log.e("LATEST SQL ROW", id);
                return id;
            }
        }
        cursor.close();
        return "";
    }

    public boolean setUploadStatus(String id, String status) {
        String strFilter = GLOBAL_ROWID + "='" + id + "'";
        ContentValues args = new ContentValues();
        args.put(TABLE_TIMELINE_SUCCESS, status);
        if (ourDatabase.update(TABLE_TIMELINE, args, strFilter, null) > 0) {
            return true;
        } else {
            return false;
        }
    }

    public void deleteAllTimelineItem() {
        ourDatabase.delete(TABLE_TIMELINE, null, null);
    }



    /*public boolean setMessageRead(String messageId) {
        String strFilter = Consts.MESSAGES_MESSAGE_ID + "='" + messageId + "'";
        ContentValues args = new ContentValues();
        args.put(TABLE_MESSAGES_READ, "1");
        if (ourDatabase.update(TABLE_MESSAGES, args, strFilter, null) > 0) {
            return true;
        } else {
            return false;
        }
    }*/

    public ArrayList<Timeline> loadTimelineItems() {
        ArrayList<Timeline> map = new ArrayList<Timeline>();
        Cursor cursor = ourDatabase.rawQuery("SELECT * FROM " + TABLE_TIMELINE + " ORDER BY " + GLOBAL_ROWID + " DESC", null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                while (cursor.isAfterLast() == false) {
                    try {
                        Timeline t = new Timeline();
                        t.setId(cursor.getString(cursor.getColumnIndex(GLOBAL_ROWID)));
                        t.setUnixTime(cursor.getString(cursor.getColumnIndex(TABLE_TIMELINE_UNIX)));
                        t.setMessage(cursor.getString(cursor.getColumnIndex(TABLE_TIMELINE_MESSAGE)));
                        t.setImage(cursor.getString(cursor.getColumnIndex(TABLE_TIMELINE_IMAGE)));
                        t.setVideo(cursor.getString(cursor.getColumnIndex(TABLE_TIMELINE_VIDEO)));
                        t.setLocation(cursor.getString(cursor.getColumnIndex(TABLE_TIMELINE_LOCATION)));
                        t.setLocationLat(cursor.getString(cursor.getColumnIndex(TABLE_TIMELINE_LOCATION_LAT)));
                        t.setLocationLong(cursor.getString(cursor.getColumnIndex(TABLE_TIMELINE_LOCATION_LONG)));
                        t.setSuccess(cursor.getString(cursor.getColumnIndex(TABLE_TIMELINE_SUCCESS)));
                        map.add(t);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    cursor.moveToNext();
                }
            }
        }
        cursor.close();
        return map;
    }


    public int getUnreadMessage(String eventId) {
        int count = 0;
        try {
            Cursor mCount = ourDatabase.rawQuery("SELECT COUNT(*) FROM " + TABLE_TIMELINE,
                    null);
            mCount.moveToFirst();
            count = mCount.getInt(0);
            mCount.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    public long insertTimelineItem(Timeline t) {
        ContentValues cv = new ContentValues();
        String sql = "SELECT * FROM " + TABLE_TIMELINE;
        Cursor cursor = ourDatabase.rawQuery(sql, null);

        Log.e(TAG, "New Timeline Item");
        cv.put(TABLE_TIMELINE_UNIX, t.getUnixTime());
        cv.put(TABLE_TIMELINE_MESSAGE, t.getMessage());
        cv.put(TABLE_TIMELINE_IMAGE, t.getImage());
        cv.put(TABLE_TIMELINE_VIDEO, t.getVideo());
        cv.put(TABLE_TIMELINE_LOCATION, t.getLocation());
        cv.put(TABLE_TIMELINE_LOCATION_LAT, t.getLocationLat());
        cv.put(TABLE_TIMELINE_LOCATION_LONG, t.getLocationLong());
        cv.put(TABLE_TIMELINE_SUCCESS, t.getSuccess());
        try {
            return ourDatabase.insert(TABLE_TIMELINE, null, cv);
        } catch (Exception e) {
            Log.e(TAG, "Error creating timeline item entry", e);
        }
        cursor.close();
        return 0;
    }
}
