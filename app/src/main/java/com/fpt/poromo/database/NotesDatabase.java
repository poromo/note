package com.fpt.poromo.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.fpt.poromo.note.Note;
import com.fpt.poromo.note.NoteDao;

@Database(entities = Note.class, version = 1, exportSchema = false)
public abstract class NotesDatabase extends RoomDatabase {

    public static NotesDatabase notesDatabase;

    public static synchronized NotesDatabase getDatabase(Context context) {
        if (notesDatabase == null) {
            notesDatabase = Room.databaseBuilder(
                    context,
                    NotesDatabase.class,
                    "notes_db"
            ).allowMainThreadQueries()
                    .build();
        }
        return notesDatabase;
    }

    public abstract NoteDao noteDao();
}
