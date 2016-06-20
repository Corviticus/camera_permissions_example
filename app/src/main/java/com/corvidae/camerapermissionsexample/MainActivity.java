package com.corvidae.camerapermissionsexample;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ProgressBar;
import java.io.File;
import java.io.IOException;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.widget.Toolbar;
import com.squareup.picasso.Picasso;

/**
 * The only Activity for this app
 */
public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private static final int PERMISSIONS_REQUEST_CAMERA = 0;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private FloatingActionButton _fab;
    private ImageView _imageView;
    private String _imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            Log.e(TAG, "onCreate called...");
        setContentView(R.layout.activity_main);

        setActionBar((Toolbar) findViewById(R.id.toolbar));
        if (getActionBar() != null) {
            try {
                getActionBar().setDisplayShowHomeEnabled(false);
                getActionBar().setDisplayHomeAsUpEnabled(false);
                getActionBar().setDisplayShowCustomEnabled(true);
                getActionBar().setDisplayShowTitleEnabled(true);
                getActionBar().setTitle(getResources().getString(R.string.app_title));
            } catch (Exception e) {
                Log.e(TAG, "getSupportActionBar() exception " + e.getMessage());
            }
        }

        // get reference to the camera's image view
        _imageView = (ImageView) findViewById(R.id.cameraPreviewImageView);

        // add listener to the Floating Action Button
        _fab = (FloatingActionButton) findViewById(R.id.fabAddPhoto);
        _fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // MUST check permissions first before accessing 'unsafe' features (i.e. camera)
                // On pre-Marshmallow (< API 23) permissions are set on app install
                // On Marshmallow and above the permissions are checked at runtime
                if (checkPermissions()) {
                    return;
                }

                // start camera activity
                openCamera();
            }
        });

        getFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {

                        /** MAIN Activity **/
                        if (getFragmentManager().getBackStackEntryCount() == 0) {

                            // load image into imageView
                            if (_imagePath.length() > 1) {
                                loadImage();
                            }

                            // show the hidden Floating Action Button
                            _fab.show();

                            // un-hide the ActionBar
                            if (getActionBar() != null) {
                                try {
                                    getActionBar().show();
                                } catch (Exception e) {
                                    Log.e(TAG, "getSupportActionBar() exception " + e.getMessage());
                                }
                            }
                        }
                    }
                });
    }

    /**
     *
     */
    @Override
    public void onResume(){
        super.onResume();

        // make sure we can get the ActionBar
        if (getActionBar() != null) {

            // only show Floating Action Button and ActionBar if no fragments on backstack
            if (getFragmentManager().getBackStackEntryCount() > 0) {
                 _fab.hide();
                getActionBar().hide();
            } else {
                _fab.show();
                getActionBar().show();
            }
        }
    }

    /**
     * Creates an 'overflow' options menu
     * @param menu The menu into which the menu xml is inflated
     * @return True to show the menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu and add items to the action bar
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    /**
     * Method is called when a menu item has been selected from the ActionBar
     * @param item The menu item selected
     * @return Default behavior is to return False to allow 'normal processing' of item selection
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // there is only one menu item, but good to check as we may add others...
        int id = item.getItemId();

        /** ABOUT menu item **/
        if (id == R.id.action_settings) {

            // hide the Floating Action Button and ActionBar
            if (_fab != null) { _fab.hide(); }
            if (getActionBar() != null) { getActionBar().hide(); }

            // swap in the About fragment and add to the backstack
            Fragment _aboutFragment = new AboutFragment();
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            fragmentTransaction.replace(R.id.mainCoordLayout, _aboutFragment);
            fragmentTransaction.addToBackStack(getString(R.string.fragment_menu_name_about));
            fragmentTransaction.commit();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * This method gets called when the camera intent is closed
     * This is the time to do any image processing, etc.
     * @param requestCode Request code passed to startActivityForResult()
     * @param resultCode Result returned from activity
     * @param data Intent data returned from activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // did everything happen as we expected
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            // load image into imageView
            if (_imagePath != null) {
                loadImage();
            }
        }
    }

    /**
     * Called when a user has selected 'Deny' or 'Allow' from the permissions dialog
     * @param requestCode Integer representing the 'Request Code'
     * @param permissions String array containing the requested permissions
     * @param grantResults Integer array containing the permissions results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA: {

                showStoragePermissionRationale();


                // If request is cancelled, the result arrays are empty so check this first
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // start camera activity
                    openCamera();

                } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    showStoragePermissionRationale();
                } else {

                    // give user a chance to go to the settings activity and grant permissions
                    Snackbar snackbar = Snackbar.make(findViewById(R.id.mainCoordLayout),
                            getResources().getString(R.string.message_no_camera_permissions), Snackbar.LENGTH_LONG);
                    snackbar.setAction(getResources().getString(R.string.settings), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            // user wants to go to the settings activity
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        }
                    });

                    snackbar.show();
                }
            }
        }
    }

    /**
     * Check device permissions to see if user has allowed use of camera hardware
     * This will end up calling onRequestPermissionsResult() to handle permissions for API 6
     * @return True or False depending on wha the user decided to do
     */
    private boolean checkPermissions() {

        // get the current permissions for the camera hardware
        int granted = ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.CAMERA);

        // application DOES NOT have permission to use the camera
        if (granted != PackageManager.PERMISSION_GRANTED) {

            // open stock google system dialog to ask user for permission
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.CAMERA },
                    PERMISSIONS_REQUEST_CAMERA);

            // cannot open camera at this time
            return true;

        } else {
            // we have permission to open the camera
            return false;
        }
    }

    /**
     * Use an intent to "simply" open a camera activity
     */
    private void openCamera() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            // Create the File to save the camera image
            File imageFile = null;
            try {
                imageFile = createImageFile();
            } catch (IOException ioEx) {
                Log.e(TAG, ioEx.toString());
            }

            // Continue only if the File was successfully created
            if (imageFile != null) {
                Uri imageURI =
                    FileProvider
                        .getUriForFile(this, "com.corvidae.camerapermissionsexample.fileprovider", imageFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void showStoragePermissionRationale() {
        // TODO - maybe a nice HTML page to encourage the feature's use
    }

    /**
     * Create a file to be used for saving a full res photo
     * @return A file object of the image location
     * @throws IOException
     */
    private File createImageFile() throws IOException {

        // Create an image file name
        String imageFileName = "Example_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // we are using Picasso to load the imageView, so the 'absolute path' is needed
        _imagePath = imageFile.getAbsolutePath();

        return imageFile;
    }

    /**
     * Use Square's Picasso library to load the image from the file into the image view
     */
    private void loadImage() {

        // start progress spinner so user knows something is happening
        final ProgressBar spinner =
                (ProgressBar) findViewById(R.id.mainCoordLayout).findViewById(R.id.progress_indicator);
        spinner.setVisibility(View.VISIBLE);

        // load image using Square Picasso library
        // image does not need to be bigger than the device display so resize it to save memory
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        Picasso.with(getApplicationContext())
            .load(new File(_imagePath))
            .resize(metrics.widthPixels, metrics.heightPixels)
            .centerCrop()
            .into(_imageView, new com.squareup.picasso.Callback() {
                @Override
                public void onSuccess() {
                    // hide the progress spinner and show the image
                    spinner.setVisibility(View.INVISIBLE);
                    _imageView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onError() {
                    Log.e(TAG, "Error loading image");
                }
            });
    }
}
