package com.fpt.poromo.helper;

import com.fpt.poromo.note.Note;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface APIInterface {

    @POST("URL")
    Call<List<Note>> sendNotesToServer(@Body List<Note> notes);
}
