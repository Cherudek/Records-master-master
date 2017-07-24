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
package com.example.android.records;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.records.data.RecordContract.RecordEntry;

/**
 * Displays list of records that were entered and stored in the app.
 */
public class CatalogActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = CatalogActivity.class.getSimpleName();

    /**
     * Identifier for the record data loader
     */
    private static final int RECORD_LOADER = 0;
    /**
     * Adapter for the ListView
     */
    RecordCursorAdapter mCursorAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find the ListView which will be populated with the record data
        ListView recordListView = (ListView) findViewById(R.id.list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        recordListView.setEmptyView(emptyView);

        // Setup an Adapter to create a list item for each row of record data in the Cursor.
        // There is no record data yet (until the loader finishes) so pass in null for the Cursor.
        mCursorAdapter = new RecordCursorAdapter(this, null);
        recordListView.setAdapter(mCursorAdapter);

        // Setup the item click listener
        recordListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Create new intent to go to {@link EditorActivity}
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);

                // Form the content URI that represents the specific pet that was clicked on,
                // by appending the "id" (passed as input to this method) onto the
                // {@link RecordEntry#CONTENT_URI}.
                // For example, the URI would be "content://com.example.android.records/records/2"
                // if the pet with ID 2 was clicked on.
                Uri currentRecordUri = ContentUris.withAppendedId(RecordEntry.CONTENT_URI, id);

                // Set the URI on the data field of the intent
                intent.setData(currentRecordUri);

                // Launch the {@link EditorActivity} to display the data for the current record.
                startActivity(intent);
            }
        });

        // Kick off the loader
        getLoaderManager().initLoader(RECORD_LOADER, null, this);

    }


    /**
     * Helper method to insert hardcoded record data into the database. For debugging purposes only.
     */
    private void insertRecord() {
        // Create a ContentValues object where column names are the keys,
        // and Final Countdown's attributes are the values.


        Uri path = Uri.parse("android.resource://com.example.android.records/" + R.drawable.the_final_countdown_single);
        String imgPath = path.toString();

        ContentValues values = new ContentValues();
        values.put(RecordEntry.COLUMN_ALBUM_NAME, "Final Countdown");
        values.put(RecordEntry.COLUMN_BAND_NAME, "Europe");
        values.put(RecordEntry.COLUMN_QUANTITY, 10);
        values.put(RecordEntry.COLUMN_PRICE, 5);
        values.put(RecordEntry.COLUMN_RECORD_COVER, imgPath);
        values.put(RecordEntry.COLUMN_SUPPLIER_NAME, "Virgin");
        values.put(RecordEntry.COLUMN_SUPPLIER_EMAIL, "order@virgin.com");

        // Insert a new row for Final CountDown into the provider using the ContentResolver.
        // Use the {@link RecordEntry#CONTENT_URI} to indicate that we want to insert
        // into the records database table.
        // Receive the new content URI that will allow us to access Final Countdown data in the future.
        getContentResolver().insert(RecordEntry.CONTENT_URI, values);
    }

    /**
     * Helper method to delete all records in the database.
     */
    private void deleteAllRecords() {
        int rowsDeleted = getContentResolver().delete(RecordEntry.CONTENT_URI, null, null);
        Log.v("CatalogActivity", rowsDeleted + " rows deleted from records database");
    }

    /**
     * Prompt the user to confirm that they want to delete this record.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the record.
                deleteAllRecords();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the record.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertRecord();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                showDeleteConfirmationDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Define a projection that specifies the columns from the table we care about.
        String[] projection = {
                RecordEntry._ID,
                RecordEntry.COLUMN_ALBUM_NAME,
                RecordEntry.COLUMN_BAND_NAME,
                RecordEntry.COLUMN_QUANTITY,
                RecordEntry.COLUMN_PRICE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                RecordEntry.CONTENT_URI,   // Provider content URI to query
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update {@link RecordCursorAdapter} with this new cursor containing updated record data
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback called when the data needs to be deleted
        mCursorAdapter.swapCursor(null);
    }
}
