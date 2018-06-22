package com.tct.transfer.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.tct.transfer.log.LogUtils;

public class FileBeanHelper extends SQLiteOpenHelper {

    private final static String TAG = "FileBeanHelper";

    public final static String _ID = "_ID";
    public final static String PATH = "path";
    public final static String NAME = "name";
    public final static String MD5 = "md5";
    public final static String SIZE = "size";
    public final static String TRANSFER_SIZE = "transferSize";
    public final static String TRANSFER_ACTION = "transferAction"; //0:upload, 1:download
    public final static String TIME = "time";
    public final static String ELAPSED = "elapsed";
    public final static String TYPE = "type"; //0:picture, 1:video, 2:text, 3:radio, 4:other
    public final static String STATUS = "status"; //0:start, 1:ing, 2:end
    public final static String RESULT = "result"; //0:succeed, 1:failed,
    public final static String URI = "uri"; //0:succeed, 1:failed,

    public final static String DB_NAME = "p2p.db";
    public final static String TABLE_NAME = "p2p";

    private final static String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " " +
            "(" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            PATH + "," +
            NAME + "," +
            MD5 + "," +
            SIZE + " LONG," +
            TRANSFER_SIZE + " LONG," +
            TRANSFER_ACTION + " INTEGER," +
            TIME + " LONG," +
            ELAPSED + " LONG," +
            TYPE + " INTEGER," +
            STATUS + " INTEGER," +
            RESULT + " INTEGER," +
            URI + ")";
    private final static String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

    public FileBeanHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE);
        db.execSQL(CREATE_TABLE);
    }
}
