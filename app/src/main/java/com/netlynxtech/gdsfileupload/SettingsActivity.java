package com.netlynxtech.gdsfileupload;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.afollestad.materialdialogs.MaterialDialog;
import com.netlynxtech.gdsfileupload.classes.SQLFunctions;
import com.securepreferences.SecurePreferences;

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
                }).build();
                pd.show();
                return true;
            }
        });

        findPreference("pref_about").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(SettingsActivity.this, AboutActivity.class));
                return true;
            }
        });
        findPreference("pref_reregister").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog pd;
                pd = new MaterialDialog.Builder(SettingsActivity.this).title("Register").content("Delete data and register again?").cancelable(false).positiveText("Yes").negativeText("No").callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        new deleteAllMessages().execute();
                        SecurePreferences sp = new SecurePreferences(SettingsActivity.this);
                        // if (!sp.getString(Consts.REGISTER_LOGIN_ID, "0").equals("0") && !sp.getString(Consts.REGISTER_USER_GROUP, "0").equals("0") && !sp.getString(Consts.REGISTER_MOBILE_NUMBER, "0").equals("0") && !sp.getString(Consts.REGISTER_USER_NAME, "0").equals("0") && !sp.getString(Consts.REGISTER_PASSWORD, "0").equals("0") && !sp.getString(Consts.REGISTER_UDID, "0").equals("0")) {
                        sp.edit().remove(Consts.REGISTER_LOGIN_ID).commit();
                        sp.edit().remove(Consts.REGISTER_MOBILE_NUMBER).commit();
                        sp.edit().remove(Consts.REGISTER_PASSWORD).commit();
                        sp.edit().remove(Consts.REGISTER_UDID).commit();
                        sp.edit().remove(Consts.REGISTER_USER_GROUP).commit();
                        sp.edit().remove(Consts.REGISTER_USER_NAME).commit();
                        sp.edit().clear().commit();
                        Intent data = new Intent();
                        data.setData(Uri.parse(Consts.SETTINGS_RESTART));
                        setResult(RESULT_OK, data);
                        finish();
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
