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
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.records.data.RecordContract.RecordEntry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.ContentValues.TAG;

/**
 * Allows user to create a new record or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = EditorActivity.class.getSimpleName();
    /**
     * Identifier for the record album image data loader
     */
    public static final int IMAGE_GALLERY_REQUEST = 20;
    /** Identifier for the record data loader */
    private static final int EXISTING_RECORD_LOADER = 0;
    /**
     * Identifier for the record album image URI loader
     */
    private static final String STATE_IMAGE_URI = "STATE_IMAGE_URI";

    final Context mContext = this;
    /**
     * Content URI for the existing record cover image(null if it's a new record)
     */
    private Uri mImageUri;
    /**
     * Image Path of the record fetched from the Uri
     */
    private String imagePath;

    /** Bitmap value of the image fetched from the Uri */
    private Bitmap image;

    /** Content URI for the existing record (null if it's a new record) */
    private Uri mCurrentRecordUri;

    /** EditText field to enter the album name */
    @BindView(R.id.edit_album_name) EditText mAlbumNameEditText;

    /** EditText field to enter the record's band name */
    @BindView(R.id.edit_band_name) EditText mBandNameEditText;

    /** EditText field to enter the Record quantity */
    @BindView(R.id.edit_quantity) EditText mQuantityEditText;

    /** EditText field to enter the Record price */
    @BindView(R.id.edit_price) EditText mPriceEditText;

    /** ImageView field to insert the Record Cover */
    @BindView(R.id.edit_image_cover) ImageView mRecordCover;
    /**
     * EditText field to enter the Record supplier Name
     */
    @BindView(R.id.edit_supplier_name) EditText mContactNameEditText;
    /**
     * EditText field to enter the Record supplier Email
     */
    @BindView(R.id.edit_supplier_email) EditText mContactEmailEditText;
    /** Button to add an image to the edit record activity */
    @BindView(R.id.add_image) Button mAddImage;
    /**
     * Button to order more records from the supplier
     */
     @BindView(R.id.email_button)  Button mOrder;
    //Button to increase Stock
     @BindView(R.id.plus)  Button mAddStock;
    //Button to decrease Stock
     @BindView(R.id.minus)  Button mMinusStock;


    /** Boolean flag that keeps track of whether the record has been edited (true) or not (false) */
    private boolean mRecordHasChanged = false;
    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mRecordHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mRecordHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        ButterKnife.bind(this);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new record or editing an existing one.
        Intent intent = getIntent();
        mCurrentRecordUri = intent.getData();

        // If the intent DOES NOT contain a record content URI, then we know that we are
        // creating a new record.
        if (mCurrentRecordUri == null) {
            // This is a new record, so change the app bar to say "Add a Record"
            setTitle(getString(R.string.editor_activity_title_new_record));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a record that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing record, so change app bar to say "Edit Record"
            setTitle(getString(R.string.editor_activity_title_edit_record));

            // Initialize a loader to read the record data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_RECORD_LOADER, null, this);
        }

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mAlbumNameEditText.setOnTouchListener(mTouchListener);
        mBandNameEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mRecordCover.setOnTouchListener(mTouchListener);
        mContactNameEditText.setOnTouchListener(mTouchListener);
        mContactEmailEditText.setOnTouchListener(mTouchListener);
        mAddImage.setOnTouchListener(mTouchListener);
        mOrder.setOnTouchListener(mTouchListener);


        //Open camera when you press on Add image button
        mAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Invoke an implicit intent to open the photo gallery
                Intent openPhotoGallery = new Intent(Intent.ACTION_OPEN_DOCUMENT);

                //Where do we find the data?
                File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

                //Get a String of the pictureDirectoryPath
                String pictureDirectoryPath = pictureDirectory.getPath();

                //Get the Uri representation
                Uri data = Uri.parse(pictureDirectoryPath);

                //Set the data and type
                openPhotoGallery.setDataAndType(data, "image/*");

                //We will invoke this activity and get something back from it
                startActivityForResult(openPhotoGallery, IMAGE_GALLERY_REQUEST);
            }

        });

        //Open the email app to send a message with pre populated fields
        mOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Invoke an implicit intent to send an email
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);

                String to = mContactEmailEditText.getText().toString();
                String albumName = mAlbumNameEditText.getText().toString();
                String bandName = mBandNameEditText.getText().toString();
                String subject = "Order: " + albumName + " by " + bandName;
                String supplier = mContactNameEditText.getText().toString();
                String sep = System.getProperty("line.separator");
                String message = "Dear " + supplier + "," + sep + "I would like to order 10 more copies of " + albumName + " by " + bandName + " . " + sep + "Regards," + sep + "Gregorio";
                emailIntent.setData(Uri.parse("mailto:" + to));
                //email.putExtra(Intent.EXTRA_CC, new String[]{ to});
                //email.putExtra(Intent.EXTRA_BCC, new String[]{to});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                emailIntent.putExtra(Intent.EXTRA_TEXT, message);

                try {
                    startActivity(emailIntent);
                    finish();
                    Log.i(LOG_TAG, "Finished sending email...");
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(EditorActivity.this, "There is no email client installed.", Toast.LENGTH_SHORT).show();
                }
            }

        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mImageUri != null)
            outState.putString(STATE_IMAGE_URI, mImageUri.toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(STATE_IMAGE_URI) &&
                !savedInstanceState.getString(STATE_IMAGE_URI).equals("")) {
            mImageUri = Uri.parse(savedInstanceState.getString(STATE_IMAGE_URI));

            ViewTreeObserver viewTreeObserver = mRecordCover.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mRecordCover.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    mRecordCover.setImageBitmap(getBitmapFromUri(mImageUri, mContext, mRecordCover));
                }
            });
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //if we are here our request was successful
        if (requestCode == IMAGE_GALLERY_REQUEST && (resultCode == RESULT_OK)) {
            try {
                //this is the address of the image on the sd cards
                mImageUri = data.getData();
                int takeFlags = data.getFlags();
                takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                imagePath = mImageUri.toString();
                //Declare a stream to read the data from the card
                InputStream inputStream;
                //We are getting an input stream based on the Uri of the image
                inputStream = getContentResolver().openInputStream(mImageUri);
                //Get a bitmap from the stream
                image = BitmapFactory.decodeStream(inputStream);
                //Show the image to the user
                mRecordCover.setImageBitmap(image);
                imagePath = mImageUri.toString();
                try {
                    getContentResolver().takePersistableUriPermission(mImageUri, takeFlags);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                mRecordCover.setImageBitmap(getBitmapFromUri(mImageUri, mContext, mRecordCover));

                } catch (Exception e) {
                    e.printStackTrace();
                    //Show the user a Toast mewssage that the Image is not available
                    Toast.makeText(EditorActivity.this, "Unable to open image", Toast.LENGTH_LONG).show();
                }
            }
        }

    /**
     * Method to add clear top flag so it doesn't create new instance of parent
     *
     * @return intent
     */
    @Override
    public Intent getSupportParentActivityIntent() {
        Intent intent = super.getSupportParentActivityIntent();
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        return intent;
    }

    public Bitmap getBitmapFromUri(Uri uri, Context context, ImageView imageView) {

        if (uri == null || uri.toString().isEmpty())
            return null;

        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();
            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e(LOG_TAG, "Failed to load image.", fne);
            return null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {

            }
        }
    }

    /**
     * Get user input from editor and save record into database.
     */
    private void saveRecord() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String albumNameString = mAlbumNameEditText.getText().toString().trim();
        String bandNameString = mBandNameEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String supplierNameString = mContactNameEditText.getText().toString().trim();
        String supplierEmailString = mContactEmailEditText.getText().toString().trim();


        if ((!TextUtils.isEmpty(albumNameString)) && (!TextUtils.isEmpty(bandNameString)) && (!TextUtils.isEmpty(imagePath)) &&
                (!TextUtils.isEmpty(quantityString)) && (!TextUtils.isEmpty(priceString)) &&
                        (!TextUtils.isEmpty(supplierNameString)) && (!TextUtils.isEmpty(supplierEmailString))) {
            // Exit activity only when all the fields have been filled
            finish();

        } else {
            // Check if this is supposed to be a new record
            // and check if all the fields in the editor are blank
            if (mCurrentRecordUri == null ||
                    TextUtils.isEmpty(albumNameString) || TextUtils.isEmpty(bandNameString) ||
                    TextUtils.isEmpty(quantityString) || TextUtils.isEmpty(priceString) ||
                    TextUtils.isEmpty(supplierNameString) || TextUtils.isEmpty(supplierEmailString)) {
                // if any of the fields are empty le the user know with a Toast message
                Toast.makeText(getApplicationContext(), "Please fill in all the missing entry fields", Toast.LENGTH_LONG).show();
            }
        }
        //make sure the image uri is not null
        if (mImageUri == null) {
            return;
        }

        // Get the imagePath
        imagePath = mImageUri.toString();

        Log.i(LOG_TAG, "TEST: Album Cover string is: " + imagePath);

        // Create a ContentValues object where column names are the keys,
        // and record attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(RecordEntry.COLUMN_ALBUM_NAME, albumNameString);
        values.put(RecordEntry.COLUMN_BAND_NAME, bandNameString);
        // If the weight is not provided by the user, don't try to parse the string into an
        // integer value. Use 0 by default.
        int quantity = 0;
        if (!TextUtils.isEmpty(quantityString)) {
            quantity = Integer.parseInt(quantityString);
        }
        values.put(RecordEntry.COLUMN_QUANTITY, quantity);
        // If the price is not provided by the user, don't try to parse the string into an
        // integer value. Use 0 by default.
        int price = 0;
        if (!TextUtils.isEmpty(priceString)) {
            price = Integer.parseInt(priceString);
        }
        values.put(RecordEntry.COLUMN_PRICE, price);
        values.put(RecordEntry.COLUMN_RECORD_COVER, imagePath);
        values.put(RecordEntry.COLUMN_SUPPLIER_NAME, supplierNameString);
        values.put(RecordEntry.COLUMN_SUPPLIER_EMAIL, supplierEmailString );


        // Determine if this is a new or existing record by checking if mCurrentRecordUri is null or not
        if (mCurrentRecordUri == null) {
            // This is a NEW record, so insert a new record into the provider,
            // returning the content URI for the new record.
            Uri newUri = getContentResolver().insert(RecordEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_record_failed),
                        Toast.LENGTH_SHORT).show();

            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_record_successful),
                        Toast.LENGTH_SHORT).show();

            }
        } else {
            // Otherwise this is an EXISTING record, so update the record with content URI: mCurrentRecordUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentRecordUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentRecordUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_record_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_record_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new record, hide the "Delete" menu item.
        if (mCurrentRecordUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save record to database
                saveRecord();

                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the record hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mRecordHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }
                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };
                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the record hasn't changed, continue with handling back button press
        if (!mRecordHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all record attributes, define a projection that contains
        // all columns from the record table
        String[] projection = {
                RecordEntry._ID,
                RecordEntry.COLUMN_ALBUM_NAME,
                RecordEntry.COLUMN_BAND_NAME,
                RecordEntry.COLUMN_QUANTITY,
                RecordEntry.COLUMN_PRICE,
                RecordEntry.COLUMN_RECORD_COVER,
                RecordEntry.COLUMN_SUPPLIER_NAME,
                RecordEntry.COLUMN_SUPPLIER_EMAIL};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentRecordUri,      // Query the content URI for the current record
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        ViewTreeObserver viewTreeObserver = mRecordCover.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mRecordCover.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mRecordCover.setImageBitmap(getBitmapFromUri(mImageUri, mContext, mRecordCover));
            }
        });

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of record attributes that we're interested in
            int idColumnIndex = cursor.getColumnIndex(RecordEntry._ID);
            int albumNameColumnIndex = cursor.getColumnIndex(RecordEntry.COLUMN_ALBUM_NAME);
            int bandNameColumnIndex = cursor.getColumnIndex(RecordEntry.COLUMN_BAND_NAME);
            int quantityColumnIndex = cursor.getColumnIndex(RecordEntry.COLUMN_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(RecordEntry.COLUMN_PRICE);
            int imageColumnIndex = cursor.getColumnIndex(RecordEntry.COLUMN_RECORD_COVER);
            int supplierNameColumnIndex = cursor.getColumnIndex(RecordEntry.COLUMN_SUPPLIER_NAME);
            int supplierEmailColumnIndex = cursor.getColumnIndex(RecordEntry.COLUMN_SUPPLIER_EMAIL);


            // Extract out the value from the Cursor for the given column index
            final long recordId = cursor.getLong(idColumnIndex);
            String albumName = cursor.getString(albumNameColumnIndex);
            String bandName = cursor.getString(bandNameColumnIndex);
            final int quantity = cursor.getInt(quantityColumnIndex);
            int price = cursor.getInt(priceColumnIndex);
            final String cover = cursor.getString(imageColumnIndex);
            String supplierName = cursor.getString(supplierNameColumnIndex);
            String supplierEmail = cursor.getString(supplierEmailColumnIndex);


            // Update the views on the screen with the values from the database
            mAlbumNameEditText.setText(albumName);
            mBandNameEditText.setText(bandName);
            mQuantityEditText.setText(Integer.toString(quantity));
            mPriceEditText.setText(Integer.toString(price));
            mContactNameEditText.setText(supplierName);
            mContactEmailEditText.setText(supplierEmail);
            mRecordCover.setImageBitmap(getBitmapFromUri(Uri.parse(cover), mContext, mRecordCover));
            mImageUri = Uri.parse(cover);


            mAddStock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (quantity >= 0) {
                        int newQuantity = quantity + 1;
                        ContentValues values = new ContentValues();
                        values.put(RecordEntry.COLUMN_QUANTITY, newQuantity);
                        Uri recordUri = ContentUris.withAppendedId(RecordEntry.CONTENT_URI, recordId);
                        int numRowsUpdated = EditorActivity.this.getContentResolver().update(recordUri, values, null, null);
                        if (!(numRowsUpdated > 0)) {
                            Log.e(TAG, EditorActivity.this.getString(R.string.editor_update_record_failed));
                        }
                    }
                    int newQuantity = 0;

                }
            });

            mMinusStock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (quantity >= 1) {
                        int newQuantity = quantity - 1;
                        ContentValues values = new ContentValues();
                        values.put(RecordEntry.COLUMN_QUANTITY, newQuantity);
                        Uri recordUri = ContentUris.withAppendedId(RecordEntry.CONTENT_URI, recordId);
                        int numRowsUpdated = EditorActivity.this.getContentResolver().update(recordUri, values, null, null);
                        if (!(numRowsUpdated > 0)) {
                            Log.e(TAG, EditorActivity.this.getString(R.string.editor_update_record_failed));
                        }
                    } else if (!(quantity >= 1)) {
                        Toast.makeText(EditorActivity.this, getString(R.string.negative_stock), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mAlbumNameEditText.setText("");
        mBandNameEditText.setText("");
        mQuantityEditText.setText("");
        mPriceEditText.setText("");
        mContactNameEditText.setText("");
        mContactEmailEditText.setText("");}

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
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

    /**
     * Prompt the user to confirm that they want to delete this record.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the record.
                deleteRecord();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);


        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the record in the database.
     */
    private void deleteRecord() {
        // Only perform the delete if this is an existing record.
        if (mCurrentRecordUri != null) {
            // Call the ContentResolver to delete the record at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentRecordUri
            // content URI already identifies the record that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentRecordUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_record_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_record_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }
}