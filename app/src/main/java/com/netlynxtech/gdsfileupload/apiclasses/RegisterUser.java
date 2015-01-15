package com.netlynxtech.gdsfileupload.apiclasses;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Probook2 on 30/12/2014.
 */
public class RegisterUser {
    @SerializedName("Mobile")
    String mobile;
    @SerializedName("LoginID")
    String loginId;
    @SerializedName("Password")
    String password;
    @SerializedName("UDID")
    String udid;
    @SerializedName("MobileType")
    String mobileType;

    public RegisterUser(String mobile, String loginId, String password, String udid, String mobileType) {
        this.mobile = mobile;
        this.loginId = loginId;
        this.password = password;
        this.udid = udid;
        this.mobileType = mobileType;
    }
}
