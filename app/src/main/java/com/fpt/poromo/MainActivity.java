package com.fpt.poromo;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.fpt.poromo.database.NotesDatabase;
import com.fpt.poromo.helper.APIConnection;
import com.fpt.poromo.helper.APIInterface;
import com.fpt.poromo.note.Note;
import com.fpt.poromo.note.NoteActivity;
import com.fpt.poromo.note.NoteAdapter;
import com.fpt.poromo.note.NoteListener;
import com.fpt.poromo.schedule.SyncJobService;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import maes.tech.intentanim.CustomIntent;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity implements NoteListener {

    private static final String TAG = "MainActivity";

    ConstraintLayout contentView;
    ImageView imgEmpty;
    TextView txtEmpty;
    EditText edtSearch;
    RecyclerView recyclerViewNote;
    BottomAppBar bottomAppBar;
    FloatingActionButton fabAddNote;

    List<Note> listNote;

    public List<Note> getListNote() {
        return listNote;
    }

    public void setListNote(List<Note> listNote) {
        this.listNote = listNote;
    }

    NoteAdapter noteAdapter;

    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTES = 3;
    public static final int REQUEST_CODE_TAKE_PHOTO = 4;
    public static final int REQUEST_CODE_SELECT_IMAGE = 5;
    public static final int REQUEST_CODE_VOICE_NOTE = 6;

    int noteClickedPosition = -1;
    ActionMode actionMode;

    AlertDialog dialogAddURL;
    AlertDialog dialogAddImage;

    AppUpdateManager mAppUpdateManager;
    InstallStateUpdatedListener installStateUpdatedListener;
    int RC_APP_UPDATE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setNavigationBarColor(ContextCompat.getColor(MainActivity.this, R.color.colorQuickActionsBackground));

        initViews();
        setActionOnViews();
        startJob();
        notificationChannel();
        getNotes(REQUEST_CODE_SHOW_NOTES, false);
    }

    public void startJob(){
        Log.d(TAG, "Start job");
        ComponentName componentName = new ComponentName(this, SyncJobService.class);
        JobInfo info = new JobInfo.Builder(123, componentName)
                .setRequiresCharging(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setPersisted(true)
                .setPeriodic(15 * 60 * 1000)
                .build();

        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = scheduler.schedule(info);
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Job scheduled");
        } else {
            Log.d(TAG, "Job scheduling failed");
        }
    }


    public void initViews() {
        contentView = findViewById(R.id.content_view);
        imgEmpty = findViewById(R.id.image_empty);
        txtEmpty = findViewById(R.id.text_empty);
        edtSearch = findViewById(R.id.input_search);
        recyclerViewNote = findViewById(R.id.notes_recycler_view);
        bottomAppBar = findViewById(R.id.main_bottom_app_bar);
        fabAddNote = findViewById(R.id.floating_action_add_notes_btn);
    }

    public void setActionOnViews() {
        KeyboardVisibilityEvent.setEventListener(MainActivity.this, isOpen -> {
            if (!isOpen) {
                edtSearch.clearFocus();
            }
        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                noteAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (listNote.size() != 0) {
                    noteAdapter.searchNotes(s.toString());
                }
            }
        });

        recyclerViewNote.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        listNote = new ArrayList<>();
        noteAdapter = new NoteAdapter(listNote, this);
        recyclerViewNote.setAdapter(noteAdapter);

        bottomAppBar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_image:
                    UIUtil.hideKeyboard(MainActivity.this);
                    showAddImageDialog();
                    break;
                case R.id.menu_voice:
                    UIUtil.hideKeyboard(MainActivity.this);
                    voiceNote();
                    break;
                case R.id.menu_web_link:
                    showAddURLDialog();
                    break;
            }
            return false;
        });

        fabAddNote.setOnClickListener(v -> {
            UIUtil.hideKeyboard(MainActivity.this);
            Intent intent = new Intent(getApplicationContext(), NoteActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
            CustomIntent.customType(MainActivity.this, "left-to-right");
            edtSearch.setText(null);
        });
    }

    public void showAddImageDialog() {
        if (dialogAddImage == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_add_image,
                    (ViewGroup) findViewById(R.id.layout_add_image_container)
            );
            builder.setView(view);

            dialogAddImage = builder.create();
            if (dialogAddImage.getWindow() != null) {
                dialogAddImage.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.layout_take_photo).setOnClickListener(v -> {
                takePhoto();
                dialogAddImage.dismiss();
            });

            view.findViewById(R.id.layout_add_image).setOnClickListener(v -> {
                selectImage();
                dialogAddImage.dismiss();
            });
        }
        dialogAddImage.show();
    }

    public void takePhoto() {
        ImagePicker.Companion.with(MainActivity.this)
                .cameraOnly()
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .start(REQUEST_CODE_TAKE_PHOTO);
    }

    public void selectImage() {
        ImagePicker.Companion.with(MainActivity.this)
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
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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
                    Toast.makeText(MainActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                } else if (!Patterns.WEB_URL.matcher(inputURL.getText().toString().trim()).matches()) {
                    Toast.makeText(MainActivity.this, "Enter Valid URL", Toast.LENGTH_SHORT).show();
                } else {
                    dialogAddURL.dismiss();
                    UIUtil.hideKeyboard(view.getContext(), inputURL);
                    Intent intent = new Intent(getApplicationContext(), NoteActivity.class);
                    intent.putExtra("isFromQuickActions", true);
                    intent.putExtra("quickActionType", "URL");
                    intent.putExtra("URL", inputURL.getText().toString().trim());
                    startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                    CustomIntent.customType(MainActivity.this, "left-to-right");
                    edtSearch.setText(null);
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

    private void notificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(Utils.CHANNEL_ID, Utils.CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(Utils.CHANNEL_DESC);

            NotificationManager managerCompat = getSystemService(NotificationManager.class);
            managerCompat.createNotificationChannel(channel);
        }
    }


    public void getNotes(final int requestCode, final boolean isNoteDeleted) {

        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask<Void, Void, List<Note>> {

            @Override
            protected List<Note> doInBackground(Void... voids) {
                List<Note> listNotes = NotesDatabase
                        .getDatabase(getApplicationContext())
                        .noteDao().getAllNotes();
                return listNotes;
            }

            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
                if (requestCode == REQUEST_CODE_SHOW_NOTES) {
                    listNote.addAll(notes);
                    noteAdapter.notifyDataSetChanged();
                } else if (requestCode == REQUEST_CODE_ADD_NOTE) {
                    listNote.add(0, notes.get(0));
                    noteAdapter.notifyItemInserted(0);
                    recyclerViewNote.smoothScrollToPosition(0);
                } else if (requestCode == REQUEST_CODE_UPDATE_NOTE) {
                    listNote.remove(noteClickedPosition);
                    if (isNoteDeleted) {
                        noteAdapter.notifyItemRemoved(noteClickedPosition);
                    } else {
                        listNote.add(noteClickedPosition, notes.get(noteClickedPosition));
                        noteAdapter.notifyItemChanged(noteClickedPosition);
                    }
                }

                if (listNote.size() != 0) {
                    imgEmpty.setVisibility(View.GONE);
                    txtEmpty.setVisibility(View.GONE);
                } else {
                    imgEmpty.setVisibility(View.VISIBLE);
                    txtEmpty.setVisibility(View.VISIBLE);
                }
            }
        }

        new GetNotesTask().execute();
    }

    @Override
    public void onNoteClicked(View view, Note note, int position) {
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), NoteActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note", note);
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
        CustomIntent.customType(MainActivity.this, "left-to-right");
        edtSearch.setText(null);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onNoteLongClicked(View view, Note note, int position) {
        noteClickedPosition = position;
        view.setForeground(getDrawable(R.drawable.foreground_selected_note));
        if (actionMode != null) {
            return;
        }

        actionMode = startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_note_action, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.note_menu_edit:
                        Intent intent = new Intent(getApplicationContext(), NoteActivity.class);
                        intent.putExtra("isViewOrUpdate", true);
                        intent.putExtra("note", note);
                        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
                        CustomIntent.customType(MainActivity.this, "left-to-right");
                        edtSearch.setText(null);
                        mode.finish();
                        return true;
                    case R.id.note_menu_share:
                        if (note.getImagePath() == null) {
                            String content = note.getTitle() + "\n\n" + note.getSubtitle() + "\n\n" + note.getNoteText();
                            Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                            shareIntent.setType("text/plain");
                            shareIntent.putExtra(Intent.EXTRA_SUBJECT, note.getTitle());
                            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, content);
                            startActivity(Intent.createChooser(shareIntent, "Share via"));
                        } else {
                            String textContent = note.getTitle() + "\n\n" + note.getSubtitle() + "\n\n" + note.getNoteText();
                            Bitmap bitmap = BitmapFactory.decodeFile(note.getImagePath());
                            String bitmapPath = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "title", null);
                            Uri bitmapUri = Uri.parse(bitmapPath);
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("image/png");
                            shareIntent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
                            shareIntent.putExtra(Intent.EXTRA_TEXT, textContent);
                            startActivity(Intent.createChooser(shareIntent, "Share"));
                        }
                        mode.finish();
                        return true;
                    case R.id.note_menu_delete:
                        showDeleteNoteDialog(note);
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onDestroyActionMode(ActionMode mode) {
                actionMode = null;
                view.setForeground(null);
            }
        });
    }

    public void showDeleteNoteDialog(Note note) {
        MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
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
                                    .deleteNote(note);
                            Call<Note> noteCall = APIConnection.getClient().create(APIInterface.class)
                                    .deleteNoteById(note.getId(), 1);
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
                            showNoti();
                            listNote.remove(noteClickedPosition);
                            noteAdapter.notifyItemRemoved(noteClickedPosition);

                            if (listNote.size() != 0) {
                                imgEmpty.setVisibility(View.GONE);
                                txtEmpty.setVisibility(View.GONE);
                            } else {
                                imgEmpty.setVisibility(View.VISIBLE);
                                txtEmpty.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    new DeleteNoteTask().execute();
                    dialogInterface.dismiss();
                })
                .setNegativeButton("Cancel", R.drawable.ic_material_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss())
                .build();
        materialDialog.show();
    }

    private void showNoti() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, Utils.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(Utils.NOTI_TITLE)
                .setContentText(Utils.NOTI_CONTENT)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(MainActivity.this);
        managerCompat.notify(Utils.NOTI_ID ,builder.build());
    }


    @Override
    protected void onStart() {
        super.onStart();

        mAppUpdateManager = AppUpdateManagerFactory.create(MainActivity.this);

        installStateUpdatedListener = state -> {
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackbarForCompleteUpdate();
            } else if (state.installStatus() == InstallStatus.INSTALLED) {
                if (mAppUpdateManager != null) {
                    mAppUpdateManager.unregisterListener(installStateUpdatedListener);
                }

            } else {
                Log.i(TAG, "InstallStateUpdatedListener: state: " + state.installStatus());
            }
        };

        mAppUpdateManager.registerListener(installStateUpdatedListener);

        mAppUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {

                try {
                    mAppUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.FLEXIBLE, MainActivity.this, RC_APP_UPDATE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }

            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackbarForCompleteUpdate();
            } else {
                Log.e(TAG, "checkForAppUpdateAvailability: something else");
            }
        });
    }

    public void popupSnackbarForCompleteUpdate() {
        Snackbar snackbar =
                Snackbar.make(
                        MainActivity.this.findViewById(R.id.bottom_bar_container_layout),
                        "New update is ready!",
                        Snackbar.LENGTH_INDEFINITE);

        snackbar.setAction("Install", view -> {
            if (mAppUpdateManager != null) {
                mAppUpdateManager.completeUpdate();
            }
        });

        snackbar.setActionTextColor(getResources().getColor(R.color.colorPrimary));
        snackbar.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
            getNotes(REQUEST_CODE_ADD_NOTE, false);
        } else if (requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK) {
            if (data != null) {
                getNotes(REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDeleted", false));
            }
        } else if (requestCode == REQUEST_CODE_TAKE_PHOTO && resultCode == RESULT_OK) {
            if (data != null) {
                Uri takePhotoUri = data.getData();
                if (takePhotoUri != null) {
                    try {
                        String selectedImagePath = getPathFromUri(takePhotoUri);
                        Intent intent = new Intent(getApplicationContext(), NoteActivity.class);
                        intent.putExtra("isFromQuickActions", true);
                        intent.putExtra("quickActionType", "image");
                        intent.putExtra("imagePath", selectedImagePath);
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                        CustomIntent.customType(MainActivity.this, "left-to-right");
                        edtSearch.setText(null);
                    } catch (Exception exception) {
                        Alerter.create(MainActivity.this)
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
                        String selectedImagePath = getPathFromUri(selectedImageUri);
                        Intent intent = new Intent(getApplicationContext(), NoteActivity.class);
                        intent.putExtra("isFromQuickActions", true);
                        intent.putExtra("quickActionType", "image");
                        intent.putExtra("imagePath", selectedImagePath);
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                        CustomIntent.customType(MainActivity.this, "left-to-right");
                        edtSearch.setText(null);
                    } catch (Exception exception) {
                        Alerter.create(MainActivity.this)
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
                Intent intent = new Intent(getApplicationContext(), NoteActivity.class);
                intent.putExtra("isFromQuickActions", true);
                intent.putExtra("quickActionType", "voiceNote");
                intent.putExtra("inputText", voiceResult.get(0));
                startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                CustomIntent.customType(MainActivity.this, "left-to-right");
                edtSearch.setText(null);
            }
        } else if (requestCode == RC_APP_UPDATE) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(MainActivity.this, "App Update Failed!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAppUpdateManager != null) {
            mAppUpdateManager.unregisterListener(installStateUpdatedListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAppUpdateManager != null) {
            mAppUpdateManager.unregisterListener(installStateUpdatedListener);
        }
    }
}