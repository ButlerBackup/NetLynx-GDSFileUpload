package com.netlynxtech.gdsfileupload;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.afollestad.materialdialogs.MaterialDialog;
import com.netlynxtech.gdsfileupload.classes.SQLFunctions;

public class SettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_activity);
        findPreference("pref_messages_clear").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog pd;
                pd = new MaterialDialog.Builder(SettingsActivity.this).title("Delete").content("Delete all messages?").cancelable(false).positiveText("Yes").negativeText("No").callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        new deleteAllMessages().execute();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                    }
                }).build();
                pd.show();
                return true;
            }
        });
    }

    class deleteAllMessages extends AsyncTask<Void, Void, Void> {
        MaterialDialog pd;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new MaterialDialog.Builder(SettingsActivity.this).title("Deleting..").cancelable(false).customView(R.layout.loading, false).build();
            pd.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (pd != null && pd.isShowing()) {
                pd.dismiss();
            }

        }

        @Override
        protected Void doInBackground(Void... voids) {
            SQLFunctions sql = new SQLFunctions(SettingsActivity.this);
            sql.open();
            sql.deleteAllTimelineItem();
            sql.close();
            return null;
        }
    }

}
