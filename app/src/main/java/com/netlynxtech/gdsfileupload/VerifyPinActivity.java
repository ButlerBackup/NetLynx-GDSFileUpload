package com.netlynxtech.gdsfileupload;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Button;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.mrengineer13.snackbar.SnackBar;
import com.netlynxtech.gdsfileupload.apiclasses.VerifyPin;
import com.netlynxtech.gdsfileupload.classes.Utils;
import com.netlynxtech.gdsfileupload.classes.WebAPIOutput;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class VerifyPinActivity extends ActionBarActivity {
    @InjectView(R.id.etPin)
    EditText etPin;
    @InjectView(R.id.bVerifyPin)
    Button bRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_pin);
        ButterKnife.inject(this);
    }

    @OnClick(R.id.bVerifyPin)
    public void verify() {
        new verifyPin().execute();
    }

    private class verifyPin extends AsyncTask<Void, Void, Void> {
        WebAPIOutput res;
        MaterialDialog pd;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new MaterialDialog.Builder(VerifyPinActivity.this).cancelable(false).title("Contacting server..").customView(R.layout.loading, false).build();
            pd.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            VerifyPin pin = new VerifyPin(new Utils(VerifyPinActivity.this).getUnique(), etPin.getText().toString(), new Utils(VerifyPinActivity.this).getPhoneNumber());
            res = MainApplication.apiService.verifyPin(pin);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            VerifyPinActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (pd != null && pd.isShowing()) {
                        pd.dismiss();
                    }
                    if (res.getStatusCode() == 1) {
                        new Utils(VerifyPinActivity.this).storeSecurePreferenceValue(Consts.REGISTER_USER_NAME, res.getVerifyPinUsername());
                        new Utils(VerifyPinActivity.this).storeSecurePreferenceValue(Consts.REGISTER_USER_GROUP, res.getVerifyPinUserGroup());
                        startActivity(new Intent(VerifyPinActivity.this, RegistrationDoneActivity.class).putExtra(Consts.REGISTER_USER_NAME, res.getVerifyPinUsername()).putExtra(Consts.REGISTER_USER_GROUP, res.getVerifyPinUserGroup()));
                        finish();
                    } else {
                        new SnackBar.Builder(VerifyPinActivity.this).withMessage(res.getStatusDescription()).withStyle(SnackBar.Style.ALERT).withDuration(SnackBar.LONG_SNACK).show();
                    }
                }
            });
        }
    }
}
