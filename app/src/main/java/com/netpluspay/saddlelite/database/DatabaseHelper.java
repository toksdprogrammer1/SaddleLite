package com.netpluspay.saddlelite.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import com.netpluspay.saddlelite.database.model.Cash;

/**
 * Created by ravi on 15/03/18.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "cash_db";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {

        // create notes table
        db.execSQL(Cash.CREATE_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + Cash.TABLE_NAME);

        // Create tables again
        onCreate(db);
    }

    public long insertCash(String orderNo, String amount, String narrative, String email, String merchantId, String status) {
        // get writable database as we want to write data
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        // `id` and `timestamp` will be inserted automatically.
        // no need to add them
        values.put(Cash.COLUMN_MERCHANT_ID, merchantId);
        values.put(Cash.COLUMN_ORDER_NO, orderNo);
        values.put(Cash.COLUMN_AMOUNT, amount);
        values.put(Cash.COLUMN_NARRATIVE, narrative);
        values.put(Cash.COLUMN_EMAIL, email);
        values.put(Cash.COLUMN_STATUS, status);

        // insert row
        long id = db.insert(Cash.TABLE_NAME, null, values);

        // close db connection
        db.close();

        // return newly inserted row id
        return id;
    }

    public Cash getCash(long id) {
        // get readable database as we are not inserting anything
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(Cash.TABLE_NAME,
                new String[]{Cash.COLUMN_ID, Cash.COLUMN_MERCHANT_ID, Cash.COLUMN_ORDER_NO, Cash.COLUMN_AMOUNT, Cash.COLUMN_NARRATIVE, Cash.COLUMN_EMAIL, Cash.COLUMN_STATUS},
                Cash.COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        if (cursor != null)
            cursor.moveToFirst();

        // prepare note object
        Cash cash = new Cash(
                cursor.getInt(cursor.getColumnIndex(Cash.COLUMN_ID)),
                cursor.getString(cursor.getColumnIndex(Cash.COLUMN_MERCHANT_ID)),
                cursor.getString(cursor.getColumnIndex(Cash.COLUMN_ORDER_NO)),
                cursor.getString(cursor.getColumnIndex(Cash.COLUMN_AMOUNT)),
                cursor.getString(cursor.getColumnIndex(Cash.COLUMN_NARRATIVE)),
                cursor.getString(cursor.getColumnIndex(Cash.COLUMN_EMAIL)),
                cursor.getString(cursor.getColumnIndex(Cash.COLUMN_STATUS)));

        // close the db connection
        cursor.close();

        return cash;
    }

    public List<Cash> getPendingCash() {
        // get readable database as we are not inserting anything
        SQLiteDatabase db = this.getReadableDatabase();
        List<Cash> allcash = new ArrayList<>();
        Cursor cursor = db.query(Cash.TABLE_NAME,
                new String[]{Cash.COLUMN_ID, Cash.COLUMN_MERCHANT_ID, Cash.COLUMN_ORDER_NO, Cash.COLUMN_AMOUNT, Cash.COLUMN_NARRATIVE, Cash.COLUMN_EMAIL, Cash.COLUMN_STATUS},
                Cash.COLUMN_STATUS + "=?",
                new String[]{String.valueOf("PENDING")}, null, null, null, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Cash cash = new Cash();
                cash.setId(cursor.getInt(cursor.getColumnIndex(Cash.COLUMN_ID)));
                cash.setMerchantId(cursor.getString(cursor.getColumnIndex(Cash.COLUMN_MERCHANT_ID)));
                cash.setOrderNo(cursor.getString(cursor.getColumnIndex(Cash.COLUMN_ORDER_NO)));
                cash.setAmount(cursor.getString(cursor.getColumnIndex(Cash.COLUMN_AMOUNT)));
                cash.setNarrative(cursor.getString(cursor.getColumnIndex(Cash.COLUMN_NARRATIVE)));
                cash.setEmail(cursor.getString(cursor.getColumnIndex(Cash.COLUMN_EMAIL)));
                cash.setStatus(cursor.getString(cursor.getColumnIndex(Cash.COLUMN_STATUS)));

                allcash.add(cash);
            } while (cursor.moveToNext());
        }

        // close db connection
        cursor.close();
        db.close();

        // return notes list
        return allcash;
    }

    public List<Cash> getAllCash() {
        List<Cash> allcash = new ArrayList<>();

        // Select All Query
        String selectQuery = "SELECT  * FROM " + Cash.TABLE_NAME + " ORDER BY " +
                Cash.COLUMN_ID + " DESC";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Cash cash = new Cash();
                cash.setId(cursor.getInt(cursor.getColumnIndex(Cash.COLUMN_ID)));
                cash.setMerchantId(cursor.getString(cursor.getColumnIndex(Cash.COLUMN_MERCHANT_ID)));
                cash.setOrderNo(cursor.getString(cursor.getColumnIndex(Cash.COLUMN_ORDER_NO)));
                cash.setAmount(cursor.getString(cursor.getColumnIndex(Cash.COLUMN_AMOUNT)));
                cash.setNarrative(cursor.getString(cursor.getColumnIndex(Cash.COLUMN_NARRATIVE)));
                cash.setEmail(cursor.getString(cursor.getColumnIndex(Cash.COLUMN_EMAIL)));
                cash.setStatus(cursor.getString(cursor.getColumnIndex(Cash.COLUMN_STATUS)));

                allcash.add(cash);
            } while (cursor.moveToNext());
        }

        // close db connection
        db.close();

        // return notes list
        return allcash;
    }

    public int getAllCashCount() {
        String countQuery = "SELECT  * FROM " + Cash.TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);

        int count = cursor.getCount();
        cursor.close();


        // return count
        return count;
    }

    public int updateCash(Cash cash) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Cash.COLUMN_STATUS, cash.getStatus());

        // updating row
        return db.update(Cash.TABLE_NAME, values, Cash.COLUMN_ID + " = ?",
                new String[]{String.valueOf(cash.getId())});
    }

    public void deleteCash(Cash cash) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(Cash.TABLE_NAME, Cash.COLUMN_ID + " = ?",
                new String[]{String.valueOf(cash.getId())});
        db.close();
    }
}
