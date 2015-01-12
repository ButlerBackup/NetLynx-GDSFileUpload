package com.netlynxtech.gdsfileupload;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.netlynxtech.gdsfileupload.apiclasses.RegisterUser;
import com.netlynxtech.gdsfileupload.classes.Utils;
import com.netlynxtech.gdsfileupload.classes.WebAPIOutput;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class RegisterActivity extends ActionBarActivity {
    @InjectView(R.id.etPhoneNumber)
    EditText etPhoneNumber;
    @InjectView(R.id.etLoginId)
    EditText etLoginId;
    @InjectView(R.id.etPassword)
    EditText etPassword;
    @InjectView(R.id.bRegister)
    Button bRegister;
    GoogleCloudMessaging gcm;
    String regid;
    String PROJECT_NUMBER = "601395162853";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);
        if (new Utils(RegisterActivity.this).checkIfRegistered()) {
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        }
        setContentView(R.layout.activity_register);
        ButterKnife.inject(this);
        etPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    bRegister.performClick();
                    return true;
                }
                return false;
            }
        });
    }

    @OnClick(R.id.bRegister)
    public void register() {
        if (etLoginId.getText().toString().length() > 0 && etPassword.getText().toString().length() > 0 && etPhoneNumber.getText().toString().length() > 0) {
            new registerUser().execute();
        } else {
            Toast.makeText(RegisterActivity.this, "Some fields are empty", Toast.LENGTH_LONG).show();
        }
    }

    private class registerUser extends AsyncTask<Void, Void, Void> {
        WebAPIOutput res;
        MaterialDialog pd;
        boolean gcmIdSuccess = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new MaterialDialog.Builder(RegisterActivity.this).cancelable(false).title("Contacting server..").customView(R.layout.loading, false).build();
            pd.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
                }
                regid = gcm.register(PROJECT_NUMBER);
                Log.e("GCM", regid);
                // RegisterUser user = new RegisterUser(etPhoneNumber.getText().toString(), etLoginId.getText().toString(), etPassword.getText().toString(), new Utils(RegisterActivity.this).getUnique());
                if (regid != null && regid.length() > 0) {
                    gcmIdSuccess = true;
                    RegisterUser user = new RegisterUser(etPhoneNumber.getText().toString(), etLoginId.getText().toString(), etPassword.getText().toString(), regid);
                    res = MainApplication.apiService.registerUser(user);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            RegisterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (pd != null && pd.isShowing()) {
                        pd.dismiss();
                    }
                    if (gcmIdSuccess) {
                        if (res != null && res.getStatusCode() == 1) {
                            new Utils(RegisterActivity.this).storeUnique(regid);
                            Toast.makeText(RegisterActivity.this, res.getStatusDescription(), Toast.LENGTH_LONG).show();
                            new Utils(RegisterActivity.this).storeSecurePreferenceValue(Consts.REGISTER_MOBILE_NUMBER, etPhoneNumber.getText().toString());
                            new Utils(RegisterActivity.this).storeSecurePreferenceValue(Consts.REGISTER_LOGIN_ID, etLoginId.getText().toString());
                            new Utils(RegisterActivity.this).storeSecurePreferenceValue(Consts.REGISTER_PASSWORD, etPassword.getText().toString());
                            startActivity(new Intent(RegisterActivity.this, VerifyPinActivity.class));
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, res.getStatusDescription(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Unable to get GCM ID from Google", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }
}
