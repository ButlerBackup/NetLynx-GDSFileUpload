package com.netlynxtech.gdsfileupload;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kbeanie.imagechooser.api.ChooserType;
import com.kbeanie.imagechooser.api.ChosenImage;
import com.kbeanie.imagechooser.api.ChosenVideo;
import com.kbeanie.imagechooser.api.ImageChooserListener;
import com.kbeanie.imagechooser.api.ImageChooserManager;
import com.kbeanie.imagechooser.api.VideoChooserListener;
import com.kbeanie.imagechooser.api.VideoChooserManager;
import com.manuelpeinado.multichoiceadapter.extras.actionbarcompat.MultiChoiceBaseAdapter;
import com.melnykov.fab.FloatingActionButton;
import com.netlynxtech.gdsfileupload.adapter.TimelineAdapter;
import com.netlynxtech.gdsfileupload.classes.SQLFunctions;
import com.netlynxtech.gdsfileupload.classes.Timeline;
import com.netlynxtech.gdsfileupload.classes.Utils;

import java.io.File;
import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;


public class MainActivity extends ActionBarActivity {
    @InjectView(R.id.lvTimeline)
    ListView lvTimeline;

    @InjectView(R.id.fab)
    FloatingActionButton fab;

    String pictureDirectory = "";
    ArrayList<Timeline> data = new ArrayList<Timeline>();
    MultiChoiceBaseAdapter adapter;
    Bundle saveInstanceState;
    String filePath;
    ImageChooserManager imageChooserManager;
    VideoChooserManager videoChooserManager;

    @InjectView(R.id.ptr_layout)
    PullToRefreshLayout mPullToRefreshLayout;

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
        // Now setup the PullToRefreshLayout
        ActionBarPullToRefresh.from(this)
                // Mark All Children as pullable
                .allChildrenArePullable()
                        // Set a OnRefreshListener
                .listener(new OnRefreshListener() {
                    @Override
                    public void onRefreshStarted(View view) {
                        new loadTimeline().execute();
                    }
                }).setup(mPullToRefreshLayout);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCameraDialog();
            }
        });
        fab.attachToListView(lvTimeline);
        this.saveInstanceState = savedInstanceState;
    }

    private class loadTimeline extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            SQLFunctions sql = new SQLFunctions(MainActivity.this);
            sql.open();
            data.clear();
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
                    lvTimeline.setVisibility(View.GONE);
                    try {
                        mPullToRefreshLayout.setRefreshComplete();
                        Log.e("DONE", "ONREFRESHCOMPLETE");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    adapter = new TimelineAdapter(saveInstanceState, MainActivity.this, data);
                    lvTimeline.setAdapter(adapter);
                    adapter.setAdapterView(lvTimeline);
                    try {
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (MainApplication.lvTop != 0 && MainApplication.lvIndex != 0) {
                        lvTimeline.setSelectionFromTop(MainApplication.lvIndex, MainApplication.lvTop);
                    }
                    lvTimeline.setVisibility(View.VISIBLE);
                    lvTimeline.setOnScrollListener(new AbsListView.OnScrollListener() {
                        @Override
                        public void onScrollStateChanged(AbsListView view, int scrollState) {

                        }

                        @Override
                        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                            MainApplication.lvIndex = lvTimeline.getFirstVisiblePosition();
                            View v = lvTimeline.getChildAt(0);
                            MainApplication.lvTop = (v == null) ? 0 : (v.getTop() - lvTimeline.getPaddingTop());
                        }
                    });
                    adapter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                            new MaterialDialog.Builder(MainActivity.this).title("Resend").content("Resend item?").negativeText("No").positiveText("Yes").callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    super.onPositive(dialog);
                                    Timeline item = data.get(i);
                                    Intent i;
                                    if (!item.getImage().equals("")) {
                                        i = new Intent(MainActivity.this, NewTimelineItemPhotoActivity.class);
                                    } else {
                                        i = new Intent(MainActivity.this, NewTimelineItemVideoActivity.class);
                                    }
                                    i.putExtra(Consts.TIMELINE_ITEM_SELECTED_FROM_MAINACTIVITY, item);
                                    startActivity(i);
                                }
                            }).build().show();
                        }
                    });
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
                                if (!new Utils(MainActivity.this).createFolder().equals("")) {
                                    pictureDirectory = System.currentTimeMillis() + ".jpg";
                                    File output = new File(new Utils(MainActivity.this).createFolder(), pictureDirectory);
                                    i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(output));
                                    startActivityForResult(i, Consts.CAMERA_PHOTO_REQUEST);
                                } else {
                                    Toast.makeText(MainActivity.this, "Unable to create folder", Toast.LENGTH_SHORT).show();
                                }
                                break;

                            case 1:
                                Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                                if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
                                    if (new Utils(MainActivity.this).compressVideoMMS()) {
                                        takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
                                    }
                                    if (new Utils(MainActivity.this).videoFileSizeLimit()) {
                                        takeVideoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 2097152L); // 2 mb
                                    }
                                    takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 15);
                                    startActivityForResult(takeVideoIntent, Consts.CAMERA_VIDEO_REQUEST);
                                }
                                break;
                            case 2: //pick gallery
                                if (!new Utils(MainActivity.this).createFolder().equals("")) {
                                    imageChooserManager = new ImageChooserManager(MainActivity.this,
                                            ChooserType.REQUEST_PICK_PICTURE, "gdsupload", false);
                                    imageChooserManager.setImageChooserListener(new ImageChooserListener() {
                                        @Override
                                        public void onImageChosen(ChosenImage image) {
                                            if (image != null) {
                                                startActivity(new Intent(MainActivity.this, NewTimelineItemPhotoActivity.class).putExtra(Consts.IMAGE_GALLERY_PASS_EXTRAS, image.getFilePathOriginal()));
                                            } else {
                                                Log.e("Error", "Error");
                                            }
                                        }

                                        @Override
                                        public void onError(String s) {
                                            Log.e("ERROR", s);
                                        }
                                    });
                                    try {
                                        filePath = imageChooserManager.choose();
                                    } catch (IllegalArgumentException e) {
                                        e.printStackTrace();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    /*
                                    Intent intent = new Intent();
                                    intent.setType("image/*");
                                    intent.setAction(Intent.ACTION_GET_CONTENT);
                                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), Consts.CAMERA_PICK_IMAGE_FROM_GALLERY);
                                */
                                } else {
                                    Toast.makeText(MainActivity.this, "Unable to create folder", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case 3: //choose video from gallery
                                videoChooserManager = new VideoChooserManager(MainActivity.this,
                                        ChooserType.REQUEST_PICK_VIDEO, false);
                                videoChooserManager.setVideoChooserListener(new VideoChooserListener() {
                                    @Override
                                    public void onVideoChosen(ChosenVideo chosenVideo) {
                                        if (chosenVideo != null) {
                                            Log.e("Video path", chosenVideo.getVideoFilePath());
                                            startActivity(new Intent(MainActivity.this, NewTimelineItemVideoActivity.class).putExtra(Consts.VIDEO_CAMERA_PASS_EXTRAS, chosenVideo.getVideoFilePath()));
                                        }
                                    }

                                    @Override
                                    public void onError(String s) {
                                        Log.e("ERROR", s);
                                    }
                                });
                                try {
                                    videoChooserManager.choose();
                                } catch (IllegalArgumentException e) {
                                    e.printStackTrace();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                break;
                        }
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Consts.CAMERA_PHOTO_REQUEST && resultCode == RESULT_OK) {
            startActivity(new Intent(MainActivity.this, NewTimelineItemPhotoActivity.class).putExtra(Consts.IMAGE_CAMERA_PASS_EXTRAS, pictureDirectory));
        } else if (requestCode == Consts.SETTING_RESTART_CODE && resultCode == RESULT_OK) {
            Intent i = new Intent(MainActivity.this, RegisterActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        } else if (requestCode == ChooserType.REQUEST_PICK_PICTURE && resultCode == RESULT_OK) {
            if (imageChooserManager == null) {
                reinitializeImageChooser();
            }
            imageChooserManager.submit(requestCode, data);
        } else if (requestCode == ChooserType.REQUEST_PICK_VIDEO && resultCode == RESULT_OK) {
            if (videoChooserManager == null) {
                reinitializeVideoChooser();
            }
            videoChooserManager.submit(requestCode, data);
        } else if (requestCode == Consts.CAMERA_VIDEO_REQUEST && resultCode == RESULT_OK) {
            try {
                Uri _uri = data.getData();
                Cursor cursor = getContentResolver().query(_uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
                cursor.moveToFirst();
                final String videoFilePath = cursor.getString(0);
                cursor.close();
                startActivity(new Intent(MainActivity.this, NewTimelineItemVideoActivity.class).putExtra(Consts.VIDEO_CAMERA_PASS_EXTRAS_PURE, videoFilePath.toString()));
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Unable to retreive captured video. Please select video from gallery", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void reinitializeVideoChooser() {
        videoChooserManager = new VideoChooserManager(MainActivity.this, ChooserType.REQUEST_CAPTURE_VIDEO,
                "gdsupload", true);
        videoChooserManager.setVideoChooserListener(new VideoChooserListener() {
            @Override
            public void onVideoChosen(ChosenVideo chosenVideo) {

            }

            @Override
            public void onError(String s) {

            }
        });
        videoChooserManager.reinitialize(filePath);
    }


    // Should be called if for some reason the ImageChooserManager is null (Due
    // to destroying of activity for low memory situations)
    private void reinitializeImageChooser() {
        imageChooserManager = new ImageChooserManager(this, ChooserType.REQUEST_PICK_PICTURE,
                "gdsupload", true);
        imageChooserManager.setImageChooserListener(new ImageChooserListener() {
            @Override
            public void onImageChosen(ChosenImage chosenImage) {

            }

            @Override
            public void onError(String s) {

            }
        });
        imageChooserManager.reinitialize(filePath);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        pictureDirectory = (String) savedInstanceState.get(Consts.CAMERA_SAVED_INSTANCE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(Consts.CAMERA_SAVED_INSTANCE, pictureDirectory);
        adapter.save(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_settings) {
            startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), Consts.SETTING_RESTART_CODE);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainApplication.lvIndex = lvTimeline.getFirstVisiblePosition();
        View v = lvTimeline.getChildAt(0);
        MainApplication.lvTop = (v == null) ? 0 : (v.getTop() - lvTimeline.getPaddingTop());
    }
}
