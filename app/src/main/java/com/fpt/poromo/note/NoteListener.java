package com.fpt.poromo.note;

import android.view.View;

public interface NoteListener {
    void onNoteClicked(View view, Note note, int position);
    void onNoteLongClicked(View view, Note note, int position);
}
