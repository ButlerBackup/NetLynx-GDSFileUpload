package com.netlynxtech.gdsfileupload;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Button;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class RegistrationDoneActivity extends ActionBarActivity {
    @InjectView(R.id.tvRegistrationDone)
    TextView tvRegistrationDone;
    @InjectView(R.id.bRegistrationDone)
    Button bRegistrationDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_done);
        ButterKnife.inject(this);
        if (getIntent().hasExtra(Consts.REGISTER_USER_NAME) && getIntent().hasExtra(Consts.REGISTER_USER_GROUP)) {
            tvRegistrationDone.setText("Welcome " + getIntent().getStringExtra(Consts.REGISTER_USER_NAME) + ". You are registered in the " + getIntent().getStringExtra(Consts.REGISTER_USER_GROUP) + " group");
        }
    }

    @OnClick(R.id.bRegistrationDone)
    public void registrationDone() {
        startActivity(new Intent(RegistrationDoneActivity.this, MainActivity.class));
        finish();
    }
}
