package com.fpt.poromo.schedule;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.fpt.poromo.database.NotesDatabase;
import com.fpt.poromo.helper.APIConnection;
import com.fpt.poromo.helper.APIInterface;
import com.fpt.poromo.note.Note;
import com.fpt.poromo.note.NoteDao;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncJobService extends JobService {
    private static final String TAG = "SyncJobService";
    private boolean jobCancelled = false;
    APIInterface apiInterface = null;

    @Override
    public boolean onStartJob(JobParameters params) {
        apiInterface = APIConnection.getClient().create(APIInterface.class);

        Log.d(TAG, "Job started");
        doBackgroundWork(params);

        return true;
    }

    private void doBackgroundWork(final JobParameters params) {
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void run() {
                while (!jobCancelled) {
                    Log.d(TAG, "Syncing........");
                    try {
                        syncDataToDb();
                    }catch (Exception ex){
                        Log.d(TAG, "Sync data to db failed");
                    }
                    try {
                        syncDataToServer();
                    }catch (Exception ex){
                        Log.d(TAG, "Sync data to server failed");
                    }


                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void syncDataToDb(){
        Call<List<Note>> call = apiInterface.getNotesByUserId(1);
        call.enqueue(new Callback<List<Note>>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onResponse(Call<List<Note>> call, Response<List<Note>> response) {
                Log.d(TAG, "syncDataToDb => Call API success");
                List<Note> notes = response.body();
                notes.forEach(note -> note.setIsSync(1));
                Log.d(TAG, "syncDataToDb => Data: "+Arrays.toString(notes.toArray()));
                Note[] noteArr = notes.toArray(new Note[0]);

                Log.d(TAG, "syncDataToDb => Array size: "+ noteArr.length);
                try {
                    NotesDatabase
                            .getDatabase(getApplicationContext())
                            .noteDao().insertAllNote(noteArr);
                    Log.d(TAG, "syncDataToDb => Save to db success");
                }catch (Exception ex){
                    Log.d(TAG, "syncDataToDb => Save to db failed: "+ ex.getMessage());
                }

            }

            @Override
            public void onFailure(Call<List<Note>> call, Throwable t) {
                Log.d(TAG, "Call API failed");
                call.cancel();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void syncDataToServer(){
        List<Note> noteList = NotesDatabase
                .getDatabase(getApplicationContext())
                .noteDao().getAllNotesNotSync();
        Log.d(TAG, "syncDataToServer => List note to sync:"+ Arrays.toString(noteList.toArray()));
        if(noteList.isEmpty()){
            Log.d(TAG, "syncDataToServer => No data to sync");
        }else {
            //change is_sync status
            noteList.forEach(note -> {note.setIsSync(1); note.setCreatedBy(1);});

            Log.d(TAG, "syncDataToServer => Start sync "+ noteList.size()+" note");
            Call<Note[]> call = apiInterface.sendNotesToServer(noteList.toArray(new Note[0]));
            call.enqueue(new Callback<Note[]>() {
                @Override
                public void onResponse(Call<Note[]> call, Response<Note[]> response) {
                    NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertAllNote(noteList.toArray(new Note[0]));
                    Log.d(TAG, "syncDataToServer => Sync success");
                }

                @Override
                public void onFailure(Call<Note[]> call, Throwable t) {
                    Log.d(TAG, "syncDataToServer => Sync failed: " + t.toString());
                    call.cancel();
                }
            });
        }
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job cancelled before completion");
        jobCancelled = true;
        return true;
    }

}
