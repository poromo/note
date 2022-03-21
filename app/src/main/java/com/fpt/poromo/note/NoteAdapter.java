package com.fpt.poromo.note;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.fpt.poromo.R;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    public List<Note> listNote;
    public NoteListener noteListener;
    public Timer timer;
    public List<Note> noteSource;

    public NoteAdapter(List<Note> listNote, NoteListener noteListener) {
        this.listNote = listNote;
        this.noteListener = noteListener;
        noteSource = listNote;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_container_note,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.setNote(listNote.get(position));
        holder.layoutNote.setOnClickListener(v -> {
            noteListener.onNoteClicked(holder.layoutNote, listNote.get(position), position);
        });
        holder.layoutNote.setOnLongClickListener(v -> {
            noteListener.onNoteLongClicked(holder.layoutNote, listNote.get(position), position);
            return true;
        });
    }

    @Override
    public int getItemViewType(int position) {
        return position;

    }

    @Override
    public int getItemCount() {
        return listNote.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout layoutNote;
        TextView itemNoteTitle, itemNoteSubtitle, itemNoteDateTime;
        RoundedImageView itemNoteImage;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);

            layoutNote = itemView.findViewById(R.id.layout_note);
            itemNoteImage = itemView.findViewById(R.id.item_note_image);
            itemNoteTitle = itemView.findViewById(R.id.item_note_title);
            itemNoteSubtitle = itemView.findViewById(R.id.item_note_subtitle);
            itemNoteDateTime = itemView.findViewById(R.id.item_note_date_time);
        }

        void setNote(Note note) {
            itemNoteTitle.setText(note.getTitle());

            if (note.getSubtitle().trim().isEmpty()) {
                itemNoteSubtitle.setVisibility(View.GONE);
            } else {
                itemNoteSubtitle.setText(note.getSubtitle());
            }

            itemNoteDateTime.setText(note.getDateTime());

            GradientDrawable gradientDrawable = (GradientDrawable) layoutNote.getBackground();
            if (note.getColor() != null) {
                gradientDrawable.setColor(Color.parseColor(note.getColor()));
            } else {
                gradientDrawable.setColor(Color.parseColor(String.valueOf(R.color.colorDefaultNoteColor)));
            }

            if (note.getImagePath() != null && !note.getImagePath().isEmpty()) {
                itemNoteImage.setVisibility(View.VISIBLE);
                itemNoteImage.setImageBitmap(BitmapFactory.decodeFile(note.getImagePath()));
            } else {
                itemNoteImage.setVisibility(View.GONE);
            }
        }
    }

    public void searchNotes(final String searchKeyword) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (searchKeyword.trim().isEmpty()) {
                    listNote = noteSource;
                } else {
                    ArrayList<Note> temp = new ArrayList<>();
                    for (Note note : noteSource) {
                        if (note.getTitle().toLowerCase().contains(searchKeyword.toLowerCase())
                                || note.getSubtitle().toLowerCase().contains(searchKeyword.toLowerCase())
                                || note.getNoteText().toLowerCase().contains(searchKeyword.toLowerCase())) {
                            temp.add(note);
                        }
                    }
                    listNote = temp;
                }
                new Handler(Looper.getMainLooper()).post(() -> notifyDataSetChanged());
            }
        }, 500);
    }

    public void cancelTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
