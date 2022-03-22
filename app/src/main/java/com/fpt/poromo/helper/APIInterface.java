package com.fpt.poromo.helper;

import com.fpt.poromo.Utils;
import com.fpt.poromo.constant.URLConstant;
import com.fpt.poromo.note.Note;

import java.util.List;

import okhttp3.internal.Util;
import retrofit2.Call;
import retrofit2.http.*;

public interface APIInterface {
    final String URL_PATH = "note/";
    @POST("URL")
    Call<List<Note>> sendNotesToServer(@Body List<Note> notes);

    @GET(URLConstant.BASE_URL + URL_PATH)
    Call<List<Note>> getAllNotes(@Query("user_id") String userId);
}
