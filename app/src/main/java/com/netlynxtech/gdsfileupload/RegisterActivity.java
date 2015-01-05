package com.netlynxtech.gdsfileupload;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Button;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.mrengineer13.snackbar.SnackBar;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (new Utils(RegisterActivity.this).checkIfRegistered()) {
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        }
        setContentView(R.layout.activity_register);
        ButterKnife.inject(this);
    }

    @OnClick(R.id.bRegister)
    public void register() {
        new registerUser().execute();
    }

    private class registerUser extends AsyncTask<Void, Void, Void> {
        WebAPIOutput res;
        MaterialDialog pd;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new MaterialDialog.Builder(RegisterActivity.this).cancelable(false).title("Contacting server..").customView(R.layout.loading, false).build();
            pd.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            RegisterUser user = new RegisterUser(etPhoneNumber.getText().toString(), etLoginId.getText().toString(), etPassword.getText().toString(), new Utils(RegisterActivity.this).getUnique());
            res = MainApplication.apiService.registerUser(user);
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
                    if (res.getStatusCode() == 1) {
                        new Utils(RegisterActivity.this).storeSecurePreferenceValue(Consts.REGISTER_MOBILE_NUMBER, etPhoneNumber.getText().toString());
                        new Utils(RegisterActivity.this).storeSecurePreferenceValue(Consts.REGISTER_LOGIN_ID, etLoginId.getText().toString());
                        new Utils(RegisterActivity.this).storeSecurePreferenceValue(Consts.REGISTER_PASSWORD, etPassword.getText().toString());
                        startActivity(new Intent(RegisterActivity.this, VerifyPinActivity.class));
                        finish();
                    } else {
                        new SnackBar.Builder(RegisterActivity.this).withMessage(res.getStatusDescription()).withStyle(SnackBar.Style.ALERT).withDuration(SnackBar.LONG_SNACK).show();
                    }
                }
            });
        }
    }
}
