package com.netlynxtech.gdsfileupload.apiclasses;

import com.google.gson.annotations.SerializedName;

public class VerifyPin {
    @SerializedName("UDID")
    String udid;
    @SerializedName("PIN")
    String pin;
    @SerializedName("Mobile")
    String mobile;

    public VerifyPin(String udid, String pin, String mobile) {
        this.udid = udid;
        this.pin = pin;
        this.mobile = mobile;
    }
}
