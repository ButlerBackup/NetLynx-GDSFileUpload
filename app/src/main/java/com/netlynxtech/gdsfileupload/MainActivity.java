package com.netlynxtech.gdsfileupload;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
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
import com.melnykov.fab.FloatingActionButton;
import com.netlynxtech.gdsfileupload.adapter.TimelineAdapter;
import com.netlynxtech.gdsfileupload.classes.SQLFunctions;
import com.netlynxtech.gdsfileupload.classes.Timeline;
import com.netlynxtech.gdsfileupload.classes.Utils;

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
    Bundle saveInstanceState;
    String filePath;
    ImageChooserManager imageChooserManager;
    VideoChooserManager videoChooserManager;

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
        this.saveInstanceState = savedInstanceState;
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
                    lvTimeline.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
                    lvTimeline.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                            new MaterialDialog.Builder(MainActivity.this).title("Resend").content("Resend photo?").negativeText("No").positiveText("Yes").callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    super.onPositive(dialog);
                                    Timeline item = data.get(i);
                                    startActivity(new Intent(MainActivity.this, NewTimelineItemActivity.class).putExtra(Consts.IMAGE_SELECTED_FROM_MAINACTIVITY, item.getImage()));
                                }
                            }).build().show();
                        }
                    });
                    lvTimeline.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

                        @Override
                        public void onItemCheckedStateChanged(ActionMode mode,
                                                              int position, long id, boolean checked) {
                            // Capture total checked items
                            final int checkedCount = lvTimeline.getCheckedItemCount();
                            // Set the CAB title according to total checked items
                            mode.setTitle(checkedCount + " Selected");
                            // Calls toggleSelection method from ListViewAdapter Class
                            adapter.toggleSelection(position);
                        }

                        @Override
                        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.menu_discard:
                                    // Calls getSelectedIds method from ListViewAdapter Class
                                    SparseBooleanArray selected = adapter
                                            .getSelectedIds();
                                    for (int i = (selected.size() - 1); i >= 0; i--) {
                                        if (selected.valueAt(i)) {
                                            Timeline selecteditem = adapter
                                                    .getItem(selected.keyAt(i));
                                            adapter.remove(selecteditem);
                                        }
                                    }
                                    // Close CAB
                                    mode.finish();
                                    return true;
                                default:
                                    return false;
                            }
                        }

                        @Override
                        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                            mode.getMenuInflater().inflate(R.menu.menu_timeline_longpress, menu);
                            return true;
                        }

                        @Override
                        public void onDestroyActionMode(ActionMode mode) {
                            adapter.removeSelection();
                        }

                        @Override
                        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                            return false;
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
                                    pictureDirectory = System.currentTimeMillis() + "";
                                    File output = new File(new Utils(MainActivity.this).createFolder(), pictureDirectory);
                                    i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(output));
                                    startActivityForResult(i, Consts.CAMERA_PHOTO_REQUEST);
                                } else {
                                    Toast.makeText(MainActivity.this, "Unable to create folder", Toast.LENGTH_SHORT).show();
                                }
                                break;

                            case 1: //take video
                                /*Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                                if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
                                    startActivityForResult(takeVideoIntent, Consts.CAMERA_VIDEO_REQUEST);
                                }*/
                                videoChooserManager = new VideoChooserManager(MainActivity.this,
                                        ChooserType.REQUEST_CAPTURE_VIDEO, "gdsupload");
                                videoChooserManager.setVideoChooserListener(new VideoChooserListener() {
                                    @Override
                                    public void onVideoChosen(ChosenVideo chosenVideo) {
                                        if (chosenVideo != null) {
                                            Log.e("CHose VIDEO", chosenVideo.getVideoFilePath());
                                        }
                                    }

                                    @Override
                                    public void onError(String s) {
                                        Log.e("ERROR", s);
                                    }
                                });
                                try {
                                    filePath = videoChooserManager.choose();
                                } catch (IllegalArgumentException e) {
                                    e.printStackTrace();
                                } catch (Exception e) {
                                    e.printStackTrace();
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
                                                startActivity(new Intent(MainActivity.this, NewTimelineItemActivity.class).putExtra(Consts.IMAGE_GALLERY_PASS_EXTRAS, image.getFilePathOriginal()));
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
                                        ChooserType.REQUEST_PICK_VIDEO);
                                videoChooserManager.setVideoChooserListener(new VideoChooserListener() {
                                    @Override
                                    public void onVideoChosen(ChosenVideo chosenVideo) {
                                        if (chosenVideo != null) {
                                            Log.e("Video path", chosenVideo.getVideoFilePath());
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
            startActivity(new Intent(MainActivity.this, NewTimelineItemActivity.class).putExtra(Consts.IMAGE_CAMERA_PASS_EXTRAS, pictureDirectory));
        } else if (requestCode == Consts.CAMERA_PICK_IMAGE_FROM_GALLERY && resultCode == RESULT_OK) {
            Uri _uri = data.getData();
            Cursor cursor = getContentResolver().query(_uri, new String[]{android.provider.MediaStore.Images.ImageColumns.DATA}, null, null, null);
            cursor.moveToFirst();
            final String imageFilePath = cursor.getString(0);
            cursor.close();
            Toast.makeText(MainActivity.this, imageFilePath.toString(), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, NewTimelineItemActivity.class).putExtra(Consts.IMAGE_GALLERY_PASS_EXTRAS, imageFilePath));
        } else if (requestCode == Consts.SETTING_RESTART_CODE && resultCode == RESULT_OK) {
            Intent i = new Intent(MainActivity.this, RegisterActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        } else if (requestCode == Consts.CAMERA_VIDEO_REQUEST && resultCode == RESULT_OK) {
            Uri videoUri = data.getData();
        } else if (requestCode == ChooserType.REQUEST_PICK_PICTURE && resultCode == RESULT_OK) {
            if (imageChooserManager == null) {
                reinitializeImageChooser();
            }
            imageChooserManager.submit(requestCode, data);
        } else if (requestCode == ChooserType.REQUEST_CAPTURE_VIDEO && resultCode == RESULT_OK) {
            if (videoChooserManager == null) {
                reinitializeVideoChooser();
            }
            videoChooserManager.submit(requestCode, data);
        } else if (requestCode == ChooserType.REQUEST_PICK_VIDEO && resultCode == RESULT_OK) {
            if (videoChooserManager == null) {
                reinitializeVideoChooser();
            }
            videoChooserManager.submit(requestCode, data);
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
}
