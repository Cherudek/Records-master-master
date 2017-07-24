/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.records.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.example.android.records.R;
import com.example.android.records.data.RecordContract.RecordEntry;

/**
 * {@link ContentProvider} for Records app.
 */
public class RecordProvider extends ContentProvider {

    /** Tag for the log messages */
    public static final String LOG_TAG = RecordProvider.class.getSimpleName();

    /** URI matcher code for the content URI for the records table */
    private static final int RECORDS = 100;

    /** URI matcher code for the content URI for a single record in the records table */
    private static final int RECORD_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // The content URI of the form "content://com.example.android.records/records" will map to the
        // integer code {@link #RECORDS}. This URI is used to provide access to MULTIPLE rows
        // of the records table.
        sUriMatcher.addURI(RecordContract.CONTENT_AUTHORITY, RecordContract.PATH_RECORDS, RECORDS);

        // The content URI of the form "content://com.example.android.records/records/#" will map to the
        // integer code {@link #RECORD_ID}. This URI is used to provide access to ONE single row
        // of the records table.
        //
        // In this case, the "#" wildcard is used where "#" can be substituted for an integer.
        // For example, "content://com.example.android.records/records/3" matches, but
        // "content://com.example.android.records/records" (without a number at the end) doesn't match.
        sUriMatcher.addURI(RecordContract.CONTENT_AUTHORITY, RecordContract.PATH_RECORDS + "/#", RECORD_ID);
    }

    /** Database helper object */
    private RecordDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new RecordDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case RECORDS:
                // For the RECORDS code, query the records table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the records table.
                cursor = database.query(RecordContract.RecordEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case RECORD_ID:
                // For the RECORD_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.records/records/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = RecordContract.RecordEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // This will perform a query on the records table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(RecordContract.RecordEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case RECORDS:
                return insertRecord(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a record into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertRecord(Uri uri, ContentValues values) {

        if (values == null) {
            Toast.makeText(getContext(), "Record cannot be empty", Toast.LENGTH_LONG).show();
            throw new IllegalArgumentException("Record cannot be empty");
        }
        // Check that the album name is not null
        String albumName = values.getAsString(RecordEntry.COLUMN_ALBUM_NAME);
        if (albumName == null) {
            Toast.makeText(getContext(), "Record requires an album name", Toast.LENGTH_LONG).show();
            throw new IllegalArgumentException("Record requires an album name");
        }

        // Check that the album name is not null
        String bandName = values.getAsString(RecordEntry.COLUMN_BAND_NAME);
        if (bandName == null) {
            Toast.makeText(getContext(), "Record requires a band name", Toast.LENGTH_LONG).show();
            throw new IllegalArgumentException("Record requires a band name");
        }

        // Check that the quantity is not null
        Integer quantity = values.getAsInteger(RecordEntry.COLUMN_QUANTITY);
        if (quantity == null && quantity < 0) {
            Toast.makeText(getContext(), "Record requires a quantity", Toast.LENGTH_LONG).show();
            throw new IllegalArgumentException("Record requires a quantity");
        }

        // If the price is provided, check that it's greater than or equal to 0 £
        Integer price = values.getAsInteger(RecordEntry.COLUMN_PRICE);
        if (price != null && price < 0) {
            Toast.makeText(getContext(), "Record requires a valid price", Toast.LENGTH_LONG).show();
            throw new IllegalArgumentException("Record requires valid price");
        }

        // Check that the record image is not null
        String recordCover = values.getAsString(RecordEntry.COLUMN_RECORD_COVER);
        if (recordCover == null) {
            Toast.makeText(getContext(), "Record requires an image", Toast.LENGTH_LONG).show();
            throw new IllegalArgumentException("Record requires an image");
        }

        // Check that the record contact supplier name is not null
        String supplierName = values.getAsString(RecordEntry.COLUMN_SUPPLIER_NAME);
        if (supplierName == null) {
            Toast.makeText(getContext(), "Record requires a supplier contact name", Toast.LENGTH_LONG).show();
            throw new IllegalArgumentException("Record requires a supplier contact name");
        }

        // Check that the record contact supplier email is not null
        String supplierEmail = values.getAsString(RecordEntry.COLUMN_SUPPLIER_EMAIL);
        if (supplierEmail == null) {
            Toast.makeText(getContext(), "Record requires a supplier contact email", Toast.LENGTH_LONG).show();
            throw new IllegalArgumentException("Record requires a supplier contact email");
        }


        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new RECORD with the given values
        long id = database.insert(RecordContract.RecordEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the pet content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case RECORDS:
                return updateRecord(uri, contentValues, selection, selectionArgs);
            case RECORD_ID:
                // For the RECORD_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = RecordContract.RecordEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateRecord(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update records in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more records).
     * Return the number of rows that were successfully updated.
     */
    private int updateRecord(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {

        //if there are no changes
        if (contentValues == null) {

            // Check that the album name is not null
            if (contentValues.containsKey(RecordEntry.COLUMN_ALBUM_NAME)) {
            Toast.makeText(getContext(), R.string.field_required, Toast.LENGTH_SHORT).show();
            throw new IllegalArgumentException("Record requires an album name");

            }  // Check that the album name is not null
            if (contentValues.containsKey(RecordEntry.COLUMN_BAND_NAME)) {
            throw new IllegalArgumentException("Record requires a band name");
        }
        // Check that the quantity is not null
        Integer quantity = contentValues.getAsInteger(RecordEntry.COLUMN_QUANTITY);
        if (quantity == null && quantity < 0) {
            throw new IllegalArgumentException("Record requires a quantity");
        }
        // If the price is provided, check that it's greater than or equal to 0 £
        Integer price = contentValues.getAsInteger(RecordEntry.COLUMN_PRICE);
        if (price != null && price < 0) {
            throw new IllegalArgumentException("Record requires valid price");
        }
        //Check that the record image is not null
            if (contentValues.containsKey(RecordEntry.COLUMN_RECORD_COVER)) {
            throw new IllegalArgumentException("Record requires an image");
        }
        // Check that the record contact supplier  is not null
            if (contentValues.containsKey(RecordEntry.COLUMN_SUPPLIER_NAME)) {
            throw new IllegalArgumentException("Record requires a supplier contact");
        }
        // Check that the record contact supplier  is not null
            if (contentValues.containsKey(RecordEntry.COLUMN_SUPPLIER_EMAIL)) {
            throw new IllegalArgumentException("Record requires a supplier email");
        }
        // If there are no values to update, then don't try to update the database
        if (contentValues.size() == 0) {
            return 0;
        }
        }


        // Otherwise, get writable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(RecordEntry.TABLE_NAME, contentValues, selection, selectionArgs);
        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        // Return the number of rows updated
        return rowsUpdated;


    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case RECORDS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(RecordEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case RECORD_ID:
                // Delete a single row given by the ID in the URI
                selection = RecordContract.RecordEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = database.delete(RecordContract.RecordEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        // Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case RECORDS:
                return RecordContract.RecordEntry.CONTENT_LIST_TYPE;
            case RECORD_ID:
                return RecordContract.RecordEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}
