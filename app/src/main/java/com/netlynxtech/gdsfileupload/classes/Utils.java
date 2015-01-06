package com.netlynxtech.gdsfileupload.classes;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.netlynxtech.gdsfileupload.Consts;
import com.securepreferences.SecurePreferences;

import java.io.ByteArrayOutputStream;

/**
 * Created by Probook2 on 30/12/2014.
 */
public class Utils {

    private ApiService apiService;
    private Context context;

    public Utils(Context con) {
        this.context = con;
    }

    public String getPhoneNumber() {
        if (Consts.DEBUG) {
            return "97307191";
        }
        SecurePreferences sp = new SecurePreferences(context);
        return sp.getString(Consts.REGISTER_MOBILE_NUMBER, "0");
    }

    public void storeSecurePreferenceValue(String key, String value) {
        SecurePreferences sp = new SecurePreferences(context);
        sp.edit().putString(key, value).commit();
    }

    public boolean checkIfRegistered() {
        SecurePreferences sp = new SecurePreferences(context);
        Log.e(Consts.REGISTER_LOGIN_ID, sp.getString(Consts.REGISTER_LOGIN_ID, "0"));
        Log.e(Consts.REGISTER_LOGIN_ID, sp.getString(Consts.REGISTER_MOBILE_NUMBER, "0"));
        Log.e(Consts.REGISTER_LOGIN_ID, sp.getString(Consts.REGISTER_PASSWORD, "0"));
        Log.e(Consts.REGISTER_LOGIN_ID, sp.getString(Consts.REGISTER_UDID, "0"));
        Log.e(Consts.REGISTER_LOGIN_ID, sp.getString(Consts.REGISTER_USER_GROUP, "0"));
        Log.e(Consts.REGISTER_LOGIN_ID, sp.getString(Consts.REGISTER_USER_NAME, "0"));
        if (!sp.getString(Consts.REGISTER_LOGIN_ID, "0").equals("0") && !sp.getString(Consts.REGISTER_USER_GROUP, "0").equals("0") && !sp.getString(Consts.REGISTER_MOBILE_NUMBER, "0").equals("0") && !sp.getString(Consts.REGISTER_USER_NAME, "0").equals("0") && !sp.getString(Consts.REGISTER_PASSWORD, "0").equals("0") && !sp.getString(Consts.REGISTER_UDID, "0").equals("0")) {
            return true;
        }
        return false;
    }

    public void storeUnique(String gcmid) {
        SecurePreferences sp = new SecurePreferences(context);
        sp.edit().putString(Consts.REGISTER_UDID, gcmid).commit();
    }

    public String getUnique() {
        SecurePreferences sp = new SecurePreferences(context);
        if (Consts.DEBUG) {
            sp.edit().putString(Consts.REGISTER_UDID, "1111111").commit();
            return "1111111";
        }
        if (!sp.getString(Consts.REGISTER_UDID, "0").equals("0")) {
            return sp.getString(Consts.REGISTER_UDID, "0");
        }
        return "";
    }

    public String convertBitmapToString(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, baos); //bm is the bitmap object
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }
}
