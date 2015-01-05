package com.netlynxtech.gdsfileupload;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;
import com.netlynxtech.gdsfileupload.adapter.TimelineAdapter;
import com.netlynxtech.gdsfileupload.classes.SQLFunctions;
import com.netlynxtech.gdsfileupload.classes.Timeline;
import com.soundcloud.android.crop.Crop;

import java.io.File;
import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends ActionBarActivity {
    @InjectView(R.id.lvTimeline)
    ListView lvTimeline;

    @InjectView(R.id.fab)
    FloatingActionButton fab;

    String pictureDirectory = "";
    ArrayList<Timeline> data = new ArrayList<Timeline>();
    TimelineAdapter adapter;

    @Override
    protected void onResume() {
        super.onResume();
        new loadTimeline().execute();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(MainActivity.this);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCameraDialog();
            }
        });
        fab.attachToListView(lvTimeline);
    }

    private class loadTimeline extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            SQLFunctions sql = new SQLFunctions(MainActivity.this);
            sql.open();
            data = sql.loadTimelineItems();
            sql.close();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter = new TimelineAdapter(MainActivity.this, data);
                    lvTimeline.setAdapter(adapter);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void showCameraDialog() {
        new MaterialDialog.Builder(this)
                .items(R.array.fabActionMain)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        switch (which) {
                            case 0: // take photo
                                Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                File dir = Environment
                                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                                pictureDirectory = System.currentTimeMillis() + ".jpeg";
                                File output = new File(dir, pictureDirectory);
                                i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(output));
                                startActivityForResult(i, Consts.CAMERA_PHOTO_REQUEST);
                                break;
                            case 2: //pick gallery
                                Crop.pickImage(MainActivity.this);
                                break;
                        }
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Consts.CAMERA_PHOTO_REQUEST) {
            if (resultCode == RESULT_OK) {
                Uri source = Uri.fromFile(new File(Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/" + pictureDirectory));
                Uri outputUri = Uri.fromFile(new File(getCacheDir(), System.currentTimeMillis() + ""));
                new Crop(source).output(outputUri).asSquare().start(this);
            }
        } else if (requestCode == Consts.CAMERA_PICK_IMAGE_FROM_GALLERY) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri _uri = data.getData();

                //User had pick an image.
                Cursor cursor = getContentResolver().query(_uri, new String[]{android.provider.MediaStore.Images.ImageColumns.DATA}, null, null, null);
                cursor.moveToFirst();

                //Link to the image
                final String imageFilePath = cursor.getString(0);
                cursor.close();
                startActivity(new Intent(MainActivity.this, NewTimelineItemActivity.class).putExtra(Consts.IMAGE_GALLERY_PASS_EXTRAS, imageFilePath));
            }
        } else if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK) {
            Uri outputUri = Uri.fromFile(new File(getCacheDir(), System.currentTimeMillis() + ""));
            new Crop(data.getData()).output(outputUri).asSquare().start(this);
        } else if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, data);
        }
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            Log.e("RESULT", Crop.getOutput(result).toString());
            startActivity(new Intent(MainActivity.this, NewTimelineItemActivity.class).putExtra(Consts.NEW_GALLERY_IMAGE_CROP_LIB_PASS_EXTRA, Crop.getOutput(result).toString()));
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        pictureDirectory = (String) savedInstanceState.get(Consts.CAMERA_SAVED_INSTANCE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(Consts.CAMERA_SAVED_INSTANCE, pictureDirectory);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
