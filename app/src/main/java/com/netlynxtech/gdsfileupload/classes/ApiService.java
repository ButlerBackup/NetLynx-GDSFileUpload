package com.netlynxtech.gdsfileupload.classes;

import com.netlynxtech.gdsfileupload.apiclasses.RegisterUser;
import com.netlynxtech.gdsfileupload.apiclasses.SubmitMessage;
import com.netlynxtech.gdsfileupload.apiclasses.VerifyPin;
import retrofit.http.Body;
import retrofit.http.POST;

public interface ApiService {

    @POST("/SubmitMessage")
    public WebAPIOutput uploadContentWithMessage(@Body SubmitMessage body);

    @POST("/Register")
    public WebAPIOutput registerUser(@Body RegisterUser body);

    @POST("/VerifyPIN")
    public WebAPIOutput verifyPin(@Body VerifyPin body);
}
