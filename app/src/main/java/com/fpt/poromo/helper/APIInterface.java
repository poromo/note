package com.fpt.poromo.helper;

import com.fpt.poromo.note.Note;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface APIInterface {

    @POST("note/")
    Call<Note[]> sendNotesToServer(@Body Note... notes);

    @GET("note?")
    Call<List<Note>> getNotesByUserId(@Query("user_id") Integer userId);
}
