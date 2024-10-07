package com.example.tunematev2;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/receive_text")
    Call<PlaylistResponse> sendText(@Body TextRequest textRequest);
}

