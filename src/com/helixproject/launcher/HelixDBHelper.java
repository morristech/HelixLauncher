package com.helixproject.launcher;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

class HelixDBHelper extends SQLiteOpenHelper {
	private static final String LOG_TAG = "HelixDBHelper";
    private static final boolean LOGD = true;
	
    private final Context mContext;

    HelixDBHelper(Context context) {
		//TABLE_FAVORITES
        super(context, LauncherProvider.DATABASE_NAME, null, LauncherProvider.DATABASE_VERSION);
        mContext = context;
    }

	@Override
    public void onCreate(SQLiteDatabase db) {
		/*if (LOGD) Log.d(LOG_TAG, "creating new Helix database");
        
        db.execSQL("CREATE TABLE helix_extras (" +
                "_id INTEGER PRIMARY KEY," +
                "title TEXT," +
                "intent TEXT," +
                "container INTEGER," +
                "screen INTEGER," +
                "cellX INTEGER," +
                "cellY INTEGER," +
                "spanX INTEGER," +
                "spanY INTEGER," +
                "itemType INTEGER," +
                "appWidgetId INTEGER NOT NULL DEFAULT -1," +
                "isShortcut INTEGER," +
                "iconType INTEGER," +
                "iconPackage TEXT," +
                "iconResource TEXT," +
                "icon BLOB," +
                "uri TEXT," +
                "displayMode INTEGER" +
                "quickSort INTEGER" +
                ");");*/
	}
	
	@Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
}
