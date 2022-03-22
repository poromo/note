package com.fpt.poromo.note;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.fpt.poromo.Utils;
import com.fpt.poromo.database.NotesDatabase;
import com.fpt.poromo.R;
import com.fpt.poromo.helper.APIConnection;
import com.fpt.poromo.helper.APIInterface;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import maes.tech.intentanim.CustomIntent;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NoteActivity extends AppCompatActivity {

    private static final String TAG = "NoteActivity";

    ImageButton imgBack, imgSave, imgRemoveURL, imgRemove, imgAdd, imgMoreAction;
    View viewSubtitleIndicator;
    EditText edtTitle, edtSubtitle, edtNote;
    ImageView imgNote;
    TextView txtDateTime, txtUrl, txtReadingNote;

    BottomSheetBehavior<ConstraintLayout> bottomSheetAddActions;
    BottomSheetBehavior<ConstraintLayout> bottomSheetMiscellaneous;

    String selectedNoteColor;
    String selectedImagePath;

    AlertDialog dialogAddURL;
    AlertDialog dialogReadingNote;

    Note alreadyAvailableNote;

    public static final int REQUEST_CODE_TAKE_PHOTO = 4;
    public static final int REQUEST_CODE_SELECT_IMAGE = 5;
    public static final int REQUEST_CODE_VOICE_NOTE = 6;

    TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        loadAdBanner();
        initViews();
        setActionOnViews();
        // push notification
        notificationChannel();

        selectedNoteColor = "#" + Integer.toHexString(ContextCompat.getColor(getApplicationContext(), R.color.colorDefaultNoteColor) & 0x00ffffff);
        selectedImagePath = "";

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        }

        imgRemoveURL.setOnClickListener(v -> {
            txtUrl.setText(null);
            txtUrl.setVisibility(View.GONE);
            imgRemoveURL.setVisibility(View.GONE);
        });

        imgRemove.setOnClickListener(v -> {
            imgNote.setImageBitmap(null);
            imgNote.setVisibility(View.GONE);
            imgRemove.setVisibility(View.GONE);
            selectedImagePath = "";
        });

        if (getIntent().getBooleanExtra("isFromQuickActions", false)) {
            String type = getIntent().getStringExtra("quickActionType");
            if (type != null) {
                if (type.equals("image")) {
                    selectedImagePath = getIntent().getStringExtra("imagePath");
                    Glide.with(imgNote.getContext()).load(selectedImagePath).centerCrop().into(imgNote);
                    imgNote.setVisibility(View.VISIBLE);
                    imgRemove.setVisibility(View.VISIBLE);
                } else if (type.equals("URL")) {
                    txtUrl.setText(getIntent().getStringExtra("URL"));
                    txtUrl.setVisibility(View.VISIBLE);
                    imgRemoveURL.setVisibility(View.VISIBLE);
                } else if (type.equals("voiceNote")) {
                    edtNote.setText(getIntent().getStringExtra("inputText"));
                }
            }
        }

        initAddActions();
        initMiscellaneous();
        setSubtitleIndicatorColor();
    }

    private void notificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(Utils.CHANNEL_ID, Utils.CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(Utils.CHANNEL_DESC);

            NotificationManager managerCompat = getSystemService(NotificationManager.class);
            managerCompat.createNotificationChannel(channel);
        }
    }
    
    public void loadAdBanner() {
        AdView adView = new AdView(this);
        adView.setAdUnitId(getString(R.string.banner_id));
        FrameLayout adViewContainer = findViewById(R.id.ad_view_container);
        adViewContainer.addView(adView);

        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);
        float widthPixels = displayMetrics.widthPixels;
        float density = displayMetrics.density;
        int adWidth = (int) (widthPixels / density);
        AdSize adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
        adView.setAdSize(adSize);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    public void initViews() {
        imgBack = findViewById(R.id.create_note_back_btn);
        imgSave = findViewById(R.id.create_note_save_btn);
        imgRemoveURL = findViewById(R.id.remove_url_btn);
        imgRemove = findViewById(R.id.remove_img_btn);
        imgAdd = findViewById(R.id.create_note_add_actions);
        imgMoreAction = findViewById(R.id.create_note_options_menu);
        viewSubtitleIndicator = findViewById(R.id.view_indicator_subtitle);
        edtTitle = findViewById(R.id.input_note_title);
        edtSubtitle = findViewById(R.id.input_note_subtitle);
        edtNote = findViewById(R.id.input_note);
        imgNote = findViewById(R.id.image_note);
        txtDateTime = findViewById(R.id.text_date_time);
        txtUrl = findViewById(R.id.text_url);
    }

    public void setActionOnViews() {
        imgBack.setOnClickListener(v -> onBackPressed());

        edtTitle.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        edtSubtitle.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        edtTitle.setRawInputType(InputType.TYPE_CLASS_TEXT);
        edtSubtitle.setRawInputType(InputType.TYPE_CLASS_TEXT);

        KeyboardVisibilityEvent.setEventListener(NoteActivity.this, isOpen -> {
            if (!isOpen) {
                edtTitle.clearFocus();
                edtSubtitle.clearFocus();
                edtNote.clearFocus();
            }
        });

        txtDateTime.setText(
                String.format("Edited %s", new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault())
                        .format(new Date()))
        );

        imgSave.setOnClickListener(v -> saveNote());
    }

    public void setViewOrUpdateNote() {
        edtTitle.setText(alreadyAvailableNote.getTitle());
        edtSubtitle.setText(alreadyAvailableNote.getSubtitle());
        edtNote.setText(alreadyAvailableNote.getNoteText());
        txtDateTime.setText(String.format("Edited %s", new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date())));
        if (alreadyAvailableNote.getImagePath() != null && !alreadyAvailableNote.getImagePath().trim().isEmpty()) {
            Glide.with(imgNote.getContext()).load(alreadyAvailableNote.getImagePath()).centerCrop().into(imgNote);
            imgNote.setVisibility(View.VISIBLE);
            imgRemove.setVisibility(View.VISIBLE);
            selectedImagePath = alreadyAvailableNote.getImagePath();
        }

        if (alreadyAvailableNote.getWebLink() != null && !alreadyAvailableNote.getWebLink().trim().isEmpty()) {
            txtUrl.setText(alreadyAvailableNote.getWebLink());
            txtUrl.setVisibility(View.VISIBLE);
            imgRemoveURL.setVisibility(View.VISIBLE);
        }
    }


    public void saveNote() {
        UIUtil.hideKeyboard(NoteActivity.this);
        if (edtTitle.getText().toString().trim().isEmpty()) {
            Alerter.create(NoteActivity.this)
                    .setText("Note Title empty!")
                    .setTextAppearance(R.style.ErrorAlert)
                    .setBackgroundColorRes(R.color.warningColor)
                    .setIcon(R.drawable.ic_error)
                    .setDuration(1500)
                    .enableIconPulse(true)
                    .enableVibration(true)
                    .enableProgress(true)
                    .setProgressColorInt(getResources().getColor(android.R.color.white))
                    .show();
            return;
        }

        final Note note = new Note();
        note.setTitle(edtTitle.getText().toString().trim());
        note.setSubtitle(edtSubtitle.getText().toString().trim());
        note.setNoteText(edtNote.getText().toString().trim());
        note.setDateTime(txtDateTime.getText().toString().trim());
        note.setColor(selectedNoteColor);

        if (imgNote.getVisibility() == View.VISIBLE) {
            note.setImagePath(selectedImagePath);
            System.out.println(selectedImagePath + "DEBUG =============3");
        }

        if (txtUrl.getVisibility() == View.VISIBLE && imgRemoveURL.getVisibility() == View.VISIBLE) {
            note.setWebLink(txtUrl.getText().toString().trim());
        }

        if (alreadyAvailableNote != null) {
            note.setId(alreadyAvailableNote.getId());
        }

        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertNote(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
                // create an notification here
                NotificationCompat.Builder builder = new NotificationCompat.Builder(NoteActivity.this, Utils.CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(Utils.NOTI_TITLE)
                        .setContentText(Utils.NOTI_CONTENT)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);
                NotificationManagerCompat managerCompat = NotificationManagerCompat.from(NoteActivity.this);
                managerCompat.notify(Utils.NOTI_ID ,builder.build());
            }
        }

        new SaveNoteTask().execute();
    }

    public void initAddActions() {
        final ConstraintLayout layoutAddActions = findViewById(R.id.layout_add_actions);
        bottomSheetAddActions = BottomSheetBehavior.from(layoutAddActions);

        imgAdd.setOnClickListener(v -> {
            if (bottomSheetAddActions.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetAddActions.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetAddActions.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }

            if (bottomSheetMiscellaneous.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetMiscellaneous.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        layoutAddActions.findViewById(R.id.layout_take_photo).setOnClickListener(v -> {
            UIUtil.hideKeyboard(NoteActivity.this);
            bottomSheetAddActions.setState(BottomSheetBehavior.STATE_COLLAPSED);
            takePhoto();
        });

        layoutAddActions.findViewById(R.id.layout_add_image).setOnClickListener(v -> {
            UIUtil.hideKeyboard(NoteActivity.this);
            bottomSheetAddActions.setState(BottomSheetBehavior.STATE_COLLAPSED);
            selectImage();
        });

        layoutAddActions.findViewById(R.id.layout_voice_note).setOnClickListener(v -> {
            UIUtil.hideKeyboard(NoteActivity.this);
            bottomSheetAddActions.setState(BottomSheetBehavior.STATE_COLLAPSED);
            voiceNote();
        });

        layoutAddActions.findViewById(R.id.layout_add_url).setOnClickListener(v -> {
            bottomSheetAddActions.setState(BottomSheetBehavior.STATE_COLLAPSED);
            showAddURLDialog();
        });
    }

    public void takePhoto() {
        ImagePicker.Companion.with(NoteActivity.this)
                .cameraOnly()
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .start(REQUEST_CODE_TAKE_PHOTO);
    }

    public void selectImage() {
        ImagePicker.Companion.with(NoteActivity.this)
                .galleryOnly()
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .start(REQUEST_CODE_SELECT_IMAGE);
    }

    public String getPathFromUri(Uri contentUri) {
        String filePath;
        Cursor cursor = getContentResolver()
                .query(contentUri, null, null, null, null);
        if (cursor == null) {
            filePath = contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }

    public void voiceNote() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak something to add note!");
        startActivityForResult(intent, REQUEST_CODE_VOICE_NOTE);
    }

    public void showAddURLDialog() {
        if (dialogAddURL == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(NoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_add_url,
                    (ViewGroup) findViewById(R.id.layout_add_url_container)
            );
            builder.setView(view);

            dialogAddURL = builder.create();
            if (dialogAddURL.getWindow() != null) {
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL = view.findViewById(R.id.input_url);
            inputURL.requestFocus();

            view.findViewById(R.id.dialog_add_btn).setOnClickListener(v -> {
                if (inputURL.getText().toString().trim().isEmpty()) {
                    Toast.makeText(NoteActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                } else if (!Patterns.WEB_URL.matcher(inputURL.getText().toString().trim()).matches()) {
                    Toast.makeText(NoteActivity.this, "Enter Valid URL", Toast.LENGTH_SHORT).show();
                } else {
                    UIUtil.hideKeyboard(view.getContext(), inputURL);
                    txtUrl.setText(inputURL.getText().toString().trim());
                    txtUrl.setVisibility(View.VISIBLE);
                    imgRemoveURL.setVisibility(View.VISIBLE);
                    dialogAddURL.dismiss();
                }
            });

            view.findViewById(R.id.dialog_cancel_btn).setOnClickListener(v -> {
                UIUtil.hideKeyboard(view.getContext(), inputURL);
                dialogAddURL.dismiss();
            });
        }
        dialogAddURL.setCancelable(false);
        dialogAddURL.show();
    }

    public void initMiscellaneous() {
        final ConstraintLayout layoutMoreAction = findViewById(R.id.layout_miscellaneous);
        bottomSheetMiscellaneous = BottomSheetBehavior.from(layoutMoreAction);

        imgMoreAction.setOnClickListener(v -> {
            if (bottomSheetMiscellaneous.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetMiscellaneous.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetMiscellaneous.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }

            if (bottomSheetAddActions.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetAddActions.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        final ImageView checkColor1 = layoutMoreAction.findViewById(R.id.check_color1);
        final ImageView checkColor2 = layoutMoreAction.findViewById(R.id.check_color2);
        final ImageView checkColor3 = layoutMoreAction.findViewById(R.id.check_color3);
        final ImageView checkColor4 = layoutMoreAction.findViewById(R.id.check_color4);
        final ImageView checkColor5 = layoutMoreAction.findViewById(R.id.check_color5);
        final ImageView checkColor6 = layoutMoreAction.findViewById(R.id.check_color6);
        final ImageView checkColor7 = layoutMoreAction.findViewById(R.id.check_color7);
        final ImageView checkColor8 = layoutMoreAction.findViewById(R.id.check_color8);

        layoutMoreAction.findViewById(R.id.view_color1).setOnClickListener(v -> {
            UIUtil.hideKeyboard(NoteActivity.this);
            selectedNoteColor = "#" + Integer.toHexString(ContextCompat.getColor(getApplicationContext(), R.color.colorDefaultNoteColor) & 0x00ffffff);
            checkColor1.setImageResource(R.drawable.ic_check);
            checkColor2.setImageResource(0);
            checkColor3.setImageResource(0);
            checkColor4.setImageResource(0);
            checkColor5.setImageResource(0);
            checkColor6.setImageResource(0);
            checkColor7.setImageResource(0);
            checkColor8.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMoreAction.findViewById(R.id.view_color2).setOnClickListener(v -> {
            UIUtil.hideKeyboard(NoteActivity.this);
            selectedNoteColor = "#E7D84C";
            checkColor1.setImageResource(0);
            checkColor2.setImageResource(R.drawable.ic_check);
            checkColor3.setImageResource(0);
            checkColor4.setImageResource(0);
            checkColor5.setImageResource(0);
            checkColor6.setImageResource(0);
            checkColor7.setImageResource(0);
            checkColor8.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMoreAction.findViewById(R.id.view_color3).setOnClickListener(v -> {
            UIUtil.hideKeyboard(NoteActivity.this);
            selectedNoteColor = "#3B81FF";
            checkColor1.setImageResource(0);
            checkColor2.setImageResource(0);
            checkColor3.setImageResource(R.drawable.ic_check);
            checkColor4.setImageResource(0);
            checkColor5.setImageResource(0);
            checkColor6.setImageResource(0);
            checkColor7.setImageResource(0);
            checkColor8.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMoreAction.findViewById(R.id.view_color4).setOnClickListener(v -> {
            UIUtil.hideKeyboard(NoteActivity.this);
            selectedNoteColor = "#FF4E4E";
            checkColor1.setImageResource(0);
            checkColor2.setImageResource(0);
            checkColor3.setImageResource(0);
            checkColor4.setImageResource(R.drawable.ic_check);
            checkColor5.setImageResource(0);
            checkColor6.setImageResource(0);
            checkColor7.setImageResource(0);
            checkColor8.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMoreAction.findViewById(R.id.view_color5).setOnClickListener(v -> {
            UIUtil.hideKeyboard(NoteActivity.this);
            selectedNoteColor = "#13A662";
            checkColor1.setImageResource(0);
            checkColor2.setImageResource(0);
            checkColor3.setImageResource(0);
            checkColor4.setImageResource(0);
            checkColor5.setImageResource(R.drawable.ic_check);
            checkColor6.setImageResource(0);
            checkColor7.setImageResource(0);
            checkColor8.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMoreAction.findViewById(R.id.view_color6).setOnClickListener(v -> {
            UIUtil.hideKeyboard(NoteActivity.this);
            selectedNoteColor = "#FF388E";
            checkColor1.setImageResource(0);
            checkColor2.setImageResource(0);
            checkColor3.setImageResource(0);
            checkColor4.setImageResource(0);
            checkColor5.setImageResource(0);
            checkColor6.setImageResource(R.drawable.ic_check);
            checkColor7.setImageResource(0);
            checkColor8.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMoreAction.findViewById(R.id.view_color7).setOnClickListener(v -> {
            UIUtil.hideKeyboard(NoteActivity.this);
            selectedNoteColor = "#118E9C";
            checkColor1.setImageResource(0);
            checkColor2.setImageResource(0);
            checkColor3.setImageResource(0);
            checkColor4.setImageResource(0);
            checkColor5.setImageResource(0);
            checkColor6.setImageResource(0);
            checkColor7.setImageResource(R.drawable.ic_check);
            checkColor8.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMoreAction.findViewById(R.id.view_color8).setOnClickListener(v -> {
            UIUtil.hideKeyboard(NoteActivity.this);
            selectedNoteColor = "#FF822E";
            checkColor1.setImageResource(0);
            checkColor2.setImageResource(0);
            checkColor3.setImageResource(0);
            checkColor4.setImageResource(0);
            checkColor5.setImageResource(0);
            checkColor6.setImageResource(0);
            checkColor7.setImageResource(0);
            checkColor8.setImageResource(R.drawable.ic_check);
            setSubtitleIndicatorColor();
        });

        if (alreadyAvailableNote != null && alreadyAvailableNote.getColor() != null && !alreadyAvailableNote.getColor().trim().isEmpty()) {
            if (alreadyAvailableNote.getColor().equals("#E7D84C")) {
                layoutMoreAction.findViewById(R.id.view_color2).performClick();
            } else if (alreadyAvailableNote.getColor().equals("#3B81FF")) {
                layoutMoreAction.findViewById(R.id.view_color3).performClick();
            } else if (alreadyAvailableNote.getColor().equals("#FF4E4E")) {
                layoutMoreAction.findViewById(R.id.view_color4).performClick();
            } else if (alreadyAvailableNote.getColor().equals("#13A662")) {
                layoutMoreAction.findViewById(R.id.view_color5).performClick();
            } else if (alreadyAvailableNote.getColor().equals("#FF388E")) {
                layoutMoreAction.findViewById(R.id.view_color6).performClick();
            } else if (alreadyAvailableNote.getColor().equals("#118E9C")) {
                layoutMoreAction.findViewById(R.id.view_color7).performClick();
            } else if (alreadyAvailableNote.getColor().equals("#FF822E")) {
                layoutMoreAction.findViewById(R.id.view_color8).performClick();
            }
        }

        layoutMoreAction.findViewById(R.id.layout_read_note).setOnClickListener(v -> {
            if (edtTitle.getText().toString().trim().isEmpty() &&
                    edtSubtitle.getText().toString().trim().isEmpty() &&
                    edtNote.getText().toString().trim().isEmpty()) {
                UIUtil.hideKeyboard(NoteActivity.this);
                bottomSheetMiscellaneous.setState(BottomSheetBehavior.STATE_COLLAPSED);
                Alerter.create(NoteActivity.this)
                        .setText("Whoops! There's nothing to read!")
                        .setTextAppearance(R.style.ErrorAlert)
                        .setBackgroundColorRes(R.color.warningColor)
                        .setIcon(R.drawable.ic_error)
                        .setDuration(3000)
                        .enableIconPulse(true)
                        .enableVibration(true)
                        .disableOutsideTouch()
                        .enableProgress(true)
                        .setProgressColorInt(getResources().getColor(android.R.color.white))
                        .show();
                return;
            } else {
                textToSpeech = new TextToSpeech(NoteActivity.this, status -> {
                    if (status == TextToSpeech.SUCCESS) {
                        int result = textToSpeech.setLanguage(Locale.ENGLISH);

                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            bottomSheetMiscellaneous.setState(BottomSheetBehavior.STATE_COLLAPSED);
                            Toast.makeText(NoteActivity.this, "Sorry, language not supported!", Toast.LENGTH_SHORT).show();
                        } else {
                            UIUtil.hideKeyboard(NoteActivity.this);
                            bottomSheetMiscellaneous.setState(BottomSheetBehavior.STATE_COLLAPSED);
                            showDialogReadingNote();
                        }
                    } else {
                        bottomSheetMiscellaneous.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        Toast.makeText(NoteActivity.this, "Initialization Failed!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        layoutMoreAction.findViewById(R.id.layout_share_note).setOnClickListener(v -> {
            if (edtTitle.getText().toString().trim().isEmpty() &&
                    edtSubtitle.getText().toString().trim().isEmpty() &&
                    edtNote.getText().toString().trim().isEmpty() &&
                    txtUrl.getVisibility() == View.GONE &&
                    imgNote.getVisibility() == View.GONE) {
                UIUtil.hideKeyboard(NoteActivity.this);
                bottomSheetMiscellaneous.setState(BottomSheetBehavior.STATE_COLLAPSED);
                Alerter.create(NoteActivity.this)
                        .setText("Whoops! There's nothing to share!")
                        .setTextAppearance(R.style.ErrorAlert)
                        .setBackgroundColorRes(R.color.warningColor)
                        .setIcon(R.drawable.ic_error)
                        .setDuration(3000)
                        .enableIconPulse(true)
                        .enableVibration(true)
                        .disableOutsideTouch()
                        .enableProgress(true)
                        .setProgressColorInt(getResources().getColor(android.R.color.white))
                        .show();
                return;
            } else if (imgNote.getVisibility() == View.GONE) {
                UIUtil.hideKeyboard(NoteActivity.this);
                String content = edtTitle.getText().toString().trim() + "\n\n" +
                        edtSubtitle.getText().toString().trim() + "\n\n" +
                        edtNote.getText().toString().trim();
                Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, edtTitle.getText().toString().trim());
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, content);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            } else {
                UIUtil.hideKeyboard(NoteActivity.this);
                String textContent = edtTitle.getText().toString().trim() + "\n\n" +
                        edtSubtitle.getText().toString().trim() + "\n\n" +
                        edtNote.getText().toString().trim();
                Bitmap bitmap = BitmapFactory.decodeFile(selectedImagePath);
                String bitmapPath = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "title", null);
                Uri bitmapUri = Uri.parse(bitmapPath);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
                shareIntent.putExtra(Intent.EXTRA_TEXT, textContent);
                startActivity(Intent.createChooser(shareIntent, "Share"));
            }
        });

        if (alreadyAvailableNote != null) {
            layoutMoreAction.findViewById(R.id.layout_delete_note).setVisibility(View.VISIBLE);
            layoutMoreAction.findViewById(R.id.layout_delete_note).setOnClickListener(v -> {
                UIUtil.hideKeyboard(NoteActivity.this);
                bottomSheetMiscellaneous.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showDeleteNoteDialog();
            });
        }
    }

    public void showDialogReadingNote() {
        if (dialogReadingNote == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(NoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_reading_note,
                    (ViewGroup) findViewById(R.id.layout_reading_note_container)
            );
            builder.setView(view);

            dialogReadingNote = builder.create();
            if (dialogReadingNote.getWindow() != null) {
                dialogReadingNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            txtReadingNote = view.findViewById(R.id.text_reading_text);

            String textToRead1 = edtTitle.getText().toString().trim();
            String textToRead2 = edtSubtitle.getText().toString().trim();
            String textToRead3 = edtNote.getText().toString().trim();

            view.findViewById(R.id.start_reading).setOnClickListener(v -> {
                txtReadingNote.setText("Reading Note...");
                textToSpeech.speak(textToRead1, TextToSpeech.QUEUE_ADD, null);
                textToSpeech.speak(textToRead2, TextToSpeech.QUEUE_ADD, null);
                textToSpeech.speak(textToRead3, TextToSpeech.QUEUE_ADD, null);
            });

            view.findViewById(R.id.stop_reading).setOnClickListener(v -> {
                txtReadingNote.setText("Do you want Note to read the note for you?");
                if (textToSpeech != null) {
                    textToSpeech.stop();
                }
                dialogReadingNote.dismiss();
            });
        }
        dialogReadingNote.setCancelable(false);
        dialogReadingNote.show();
    }

    public void showDeleteNoteDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(NoteActivity.this)
                .setTitle("Are you sure?")
                .setMessage("Are you sure you want to delete this note?")
                .setAnimation(R.raw.lottie_delete)
                .setCancelable(false)
                .setPositiveButton("Delete", R.drawable.ic_material_dialog_delete, (dialogInterface, which) -> {
                    @SuppressLint("StaticFieldLeak")
                    class DeleteNoteTask extends AsyncTask<Void, Void, Void> {

                        @Override
                        protected Void doInBackground(Void... voids) {
                            NotesDatabase.getDatabase(getApplicationContext()).noteDao()
                                    .deleteNote(alreadyAvailableNote);
                            Call<Note> noteCall = APIConnection.getClient().create(APIInterface.class).deleteNoteById(alreadyAvailableNote.getId(), 1);
                            noteCall.enqueue(new Callback<Note>() {
                                @Override
                                public void onResponse(Call<Note> call, Response<Note> response) {
                                    Log.d(TAG, "Delete note: "+ response.body().toString()+" success");
                                }

                                @Override
                                public void onFailure(Call<Note> call, Throwable t) {
                                    Log.d(TAG, "Can not delete note in server: "+ t.getMessage());
                                    call.cancel();
                                }
                            });
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            Intent intent = new Intent();
                            intent.putExtra("isNoteDeleted", true);
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }

                    new DeleteNoteTask().execute();
                    dialogInterface.dismiss();
                })
                .setNegativeButton("Cancel", R.drawable.ic_material_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss())
                .build();
        materialDialog.show();
    }

    @SuppressLint("ResourceType")
    public void setSubtitleIndicatorColor() {
        GradientDrawable gradientDrawable = (GradientDrawable) viewSubtitleIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_TAKE_PHOTO && resultCode == RESULT_OK) {
            if (data != null) {
                Uri takePhotoUri = data.getData();
                if (takePhotoUri != null) {
                    try {
                        Glide.with(imgNote.getContext()).load(takePhotoUri).centerCrop().into(imgNote);
                        imgNote.setVisibility(View.VISIBLE);
                        imgRemove.setVisibility(View.VISIBLE);

                        selectedImagePath = getPathFromUri(takePhotoUri);
                    } catch (Exception exception) {
                        Alerter.create(NoteActivity.this)
                                .setText("Some ERROR occurred!")
                                .setTextAppearance(R.style.ErrorAlert)
                                .setBackgroundColorRes(R.color.warningColor)
                                .setIcon(R.drawable.ic_error)
                                .setDuration(3000)
                                .enableIconPulse(true)
                                .enableVibration(true)
                                .disableOutsideTouch()
                                .enableProgress(true)
                                .setProgressColorInt(getResources().getColor(android.R.color.white))
                                .show();
                    }
                }
            }
        } else if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    try {
                        Glide.with(imgNote.getContext()).load(selectedImageUri).centerCrop().into(imgNote);
                        imgNote.setVisibility(View.VISIBLE);
                        imgRemove.setVisibility(View.VISIBLE);

                        selectedImagePath = getPathFromUri(selectedImageUri);
                    } catch (Exception exception) {
                        Alerter.create(NoteActivity.this)
                                .setText("Some ERROR occurred!")
                                .setTextAppearance(R.style.ErrorAlert)
                                .setBackgroundColorRes(R.color.warningColor)
                                .setIcon(R.drawable.ic_error)
                                .setDuration(3000)
                                .enableIconPulse(true)
                                .enableVibration(true)
                                .disableOutsideTouch()
                                .enableProgress(true)
                                .setProgressColorInt(getResources().getColor(android.R.color.white))
                                .show();
                    }
                }
            }
        } else if (requestCode == REQUEST_CODE_VOICE_NOTE && resultCode == RESULT_OK) {
            if (data != null) {
                ArrayList<String> voiceResult = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                edtNote.setText(voiceResult.get(0));
            }
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Alerter.create(NoteActivity.this)
                    .setText("Some ERROR occurred!")
                    .setTextAppearance(R.style.ErrorAlert)
                    .setBackgroundColorRes(R.color.warningColor)
                    .setIcon(R.drawable.ic_error)
                    .setDuration(3000)
                    .enableIconPulse(true)
                    .enableVibration(true)
                    .disableOutsideTouch()
                    .enableProgress(true)
                    .setProgressColorInt(getResources().getColor(android.R.color.white))
                    .show();
        } else {
            return;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (textToSpeech != null) {
            textToSpeech.stop();
            dialogReadingNote.dismiss();
            txtReadingNote.setText("Do you want Note to read the note for you?");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            dialogReadingNote.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            dialogReadingNote.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(NoteActivity.this, "right-to-left");
    }
}