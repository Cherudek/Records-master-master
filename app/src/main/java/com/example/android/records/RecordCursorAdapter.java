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

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.records.data.RecordContract.RecordEntry;

import static android.content.ContentValues.TAG;

/**
 * {@link RecordCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of record data as its data source. This adapter knows
 * how to create list items for each row of record data in the {@link Cursor}.
 */
public class RecordCursorAdapter extends CursorAdapter {

    public static final String LOG_TAG = RecordCursorAdapter.class.getSimpleName();

    private static Context mContext;

    private ImageView saleImageView;


    /**
     * Constructs a new {@link RecordCursorAdapter}.
     * @param context The context
     * @param cursor       The cursor from which to get the data.
     */
    public RecordCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0 /* flags */);
        mContext = context;
    }
    /**
     *  newView makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }
    /**
     * The bindView method binds the record data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current record can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {


        // Find individual views that we want to modify in the list item layout
        TextView albumNameTextView = (TextView) view.findViewById(R.id.album_name);
        TextView bandNameTextView = (TextView) view.findViewById(R.id.band_name);
        TextView quantityTextView = (TextView) view.findViewById(R.id.quantiy);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);
        saleImageView = (ImageView) view.findViewById(R.id.sale_button);


        // Find the columns of the record attributes that we're interested in
        int albumNameColumnIndex = cursor.getColumnIndex(RecordEntry.COLUMN_ALBUM_NAME);
        int bandNameColumnIndex = cursor.getColumnIndex(RecordEntry.COLUMN_BAND_NAME);
        int quantityColumnIndex = cursor.getColumnIndex(RecordEntry.COLUMN_QUANTITY);
        int priceNameColumnIndex = cursor.getColumnIndex(RecordEntry.COLUMN_PRICE);

        int idColumnIndex = cursor.getColumnIndex(RecordEntry._ID);

        // Read the record attributes from the Cursor for the current record
        final String albumName = cursor.getString(albumNameColumnIndex);
        final String bandName = cursor.getString(bandNameColumnIndex);
        final int quantity = cursor.getInt(quantityColumnIndex);
        final int price = cursor.getInt(priceNameColumnIndex);
        final long recordId = cursor.getLong(idColumnIndex);
        final int newQuantity;

        // Update the TextViews with the attributes for the current record
        albumNameTextView.setText(albumName);
        bandNameTextView.setText(bandName);
        quantityTextView.setText(Integer.toString(quantity));
        priceTextView.setText(Integer.toString(price));

        // Sale button reduces the quantity of the record in stock by -1.
        saleImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (quantity >= 1) {

                    Log.i(LOG_TAG, "TEST: On sale click Quantity is: " + quantity);
                    int newQuantity = quantity - 1;
                    Log.i(LOG_TAG, "TEST: On sale click Updated Quantity is: " + newQuantity);

                    // Update table with new stock of the product
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(RecordEntry.COLUMN_QUANTITY, newQuantity);
                    Uri recordUri = ContentUris.withAppendedId(RecordEntry.CONTENT_URI, recordId);
                    Log.i(LOG_TAG, "TEST: On sale click ContentUri is: " + RecordEntry.CONTENT_URI);
                    Log.i(LOG_TAG, "TEST: On sale click ContentUri_ID is: " + recordUri);
                    Log.i(LOG_TAG, "TEST: On sale click Album Name is: " + albumName);


                    int numRowsUpdated = context.getContentResolver().update(recordUri, contentValues, null, null);
                    Log.i(LOG_TAG, "TEST: number Rows Updated: " + numRowsUpdated);

                    if (!(numRowsUpdated > 0)) {
                        Log.e(TAG, context.getString(R.string.editor_update_record_failed));
                    }
                } else if (!(quantity >= 1)) {
                    int quantity = 0;
                    Toast.makeText(context, R.string.sold_out, Toast.LENGTH_SHORT).show();

                }
            }
        });
    }
}


