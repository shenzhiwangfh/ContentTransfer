package com.tct.transfer.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class FileBeanProvider extends ContentProvider {

    public final static int BEANS = 0;
    public final static String AUTHORITY = "com.tct.transfer.provider";

    private static UriMatcher uriMatcher;
    private FileBeanHelper mHelper;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, FileBeanHelper.TABLE_NAME, BEANS);
    }

    @Override
    public boolean onCreate() {
        mHelper = new FileBeanHelper(getContext(), FileBeanHelper.DB_NAME, null, 1);
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor = null;
        switch (uriMatcher.match(uri)) {
            case BEANS:
                cursor = db.query(FileBeanHelper.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                break;
        }

        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        switch (uriMatcher.match(uri)) {
            case BEANS:
                long id = db.insert(FileBeanHelper.TABLE_NAME, null, values);
                return Uri.parse("content://" + AUTHORITY + "/_id/" + id);
            default:
                break;
        }

        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        switch (uriMatcher.match(uri)) {
            case BEANS:
                db.delete(FileBeanHelper.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                break;
        }
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case BEANS:
                return "vnd.android.cursor.dir/vnd.com.tct.transfer.provider.beans";
            default:
                break;
        }
        return null;
    }
}
