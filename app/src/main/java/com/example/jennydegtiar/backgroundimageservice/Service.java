package com.example.jennydegtiar.backgroundimageservice;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Created by Jenny.Degtiar on 12/20/2017.
 */

public interface  Service {

    @Multipart
    @POST("/en-creep-t/classify/?phoneNumber=1")
    Call<ResponseBody> postImage(@Part MultipartBody.Part image, @Part("name") RequestBody name);
}
