package com.netlynxtech.gdsfileupload.apiclasses;

import com.google.gson.annotations.SerializedName;

public class SubmitMessage {
    @SerializedName("UDID")
    String udid;
    @SerializedName("Message")
    String message;
    @SerializedName("Filename")
    String fileName;
    @SerializedName("Base64String")
    String base64String;
    @SerializedName("Lat")
    String locationLat;
    @SerializedName("Long")
    String locationLong;

    @SerializedName("GPSLocation")
    String gpsLocation;

    public SubmitMessage(String udid, String message, String fileName, String base64String, String locationLat, String locationLong, String gpsLocation) {
        this.udid = udid;
        this.message = message;
        this.fileName = fileName;
        this.base64String = base64String;
        this.locationLat = locationLat;
        this.locationLong = locationLong;
        this.gpsLocation = gpsLocation;
    }
}
