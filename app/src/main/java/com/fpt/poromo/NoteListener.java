package com.fpt.poromo;

import android.view.View;

public interface NoteListener {
    void onNoteClicked(View view, Note note, int position);
    void onNoteLongClicked(View view, Note note, int position);
}
