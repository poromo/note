package com.fpt.poromo.helper;

import com.fpt.poromo.note.Note;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface APIInterface {

    @POST("note")
    Call<List<Note>> sendNotesToServer(@Body List<Note> notes);

    @GET("note?")
    Call<List<Note>> getNotesByUserId(@Query("user_id") Integer userId);
}
