package com.example.firebaseapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnItemClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MAX = 1000;
    private static final String MSG_LENGTH_KEY = "msg_max_length";
    private static final String DEFAULT_USERNAME = "Anonymous";
    private static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER = 5;

    private String userName;
    private List<ChatMessage> chatMessages;
    private RecyclerView chatRecyclerView;
    private ChatMessageAdapter chatMessageAdapter;
    private LinearLayoutManager linearLayoutManager;

    private ImageButton photoPickerButton;
    private EditText messageEditText;
    private Button sendButton;
    private ProgressBar progressBar;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference messagesDatabaseReference;
    private ChildEventListener childEventListener;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseStorage firebaseStorage;
    private StorageReference photosStorageReference;
    private FirebaseRemoteConfig firebaseRemoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize firebase objects
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        messagesDatabaseReference = firebaseDatabase.getReference().child("messages");
        photosStorageReference = firebaseStorage.getReference().child("chat_photos");

        photoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        messageEditText = (EditText) findViewById(R.id.messageEditText);
        sendButton = (Button) findViewById(R.id.sendButton);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        chatRecyclerView = (RecyclerView) findViewById(R.id.chatRecyclerView);

        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(linearLayoutManager);

        userName = DEFAULT_USERNAME;
        chatMessages = new ArrayList<>();
        chatMessageAdapter = new ChatMessageAdapter(chatMessages);
        chatRecyclerView.setAdapter(chatMessageAdapter);
        chatMessageAdapter.setClickListener(this);
        progressBar.setVisibility(ProgressBar.INVISIBLE);

        photoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    sendButton.setEnabled(true);
                }
                else {
                    sendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        messageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX)});

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatMessage chatMessage = new ChatMessage(userName, messageEditText.getText().toString(), null);
                messagesDatabaseReference.push().setValue(chatMessage);
                messageEditText.setText("");
            }
        });

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // Signed in
                    onSignedInInitialize(user.getDisplayName());
                }
                else {
                    // Signed out
                    onSignedOutCleanUp();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setProviders(Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                            new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };

        FirebaseRemoteConfigSettings remoteConfigSettings = new FirebaseRemoteConfigSettings.Builder()
                                                                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                                                                .build();
        firebaseRemoteConfig.setConfigSettings(remoteConfigSettings);

        Map<String, Object> configMap = new HashMap<>();
        configMap.put(MSG_LENGTH_KEY, MAX);
        firebaseRemoteConfig.setDefaults(configMap);

        fetchConfig();
    }

    private void fetchConfig() {
        long cacheExpiration = 3600;
        if (firebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        firebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        firebaseRemoteConfig.activateFetched();
                        applyRetrievedConfig();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error fetching config", e);
                        applyRetrievedConfig();
                    }
                });
    }

    private void applyRetrievedConfig() {
        Long msg_max_length = firebaseRemoteConfig.getLong(MSG_LENGTH_KEY);
        messageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(msg_max_length.intValue())});
        Log.i(TAG, MSG_LENGTH_KEY + " = " + msg_max_length);
    }

    private void onSignedInInitialize(String displayName) {
        userName = displayName;
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanUp() {
        userName = DEFAULT_USERNAME;
        chatMessageAdapter.clearAll();
        detachDatabaseReadListener();
    }

    private void detachDatabaseReadListener() {
        if (childEventListener != null) {
            messagesDatabaseReference.removeEventListener(childEventListener);
            childEventListener = null;
        }
    }

    private void attachDatabaseReadListener() {
        if (childEventListener == null) {
            // Listen to changes in the database
            childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    ChatMessage cm = dataSnapshot.getValue(ChatMessage.class);
                    chatMessageAdapter.addItem(cm);
                    chatRecyclerView.scrollToPosition(chatMessageAdapter.getItemCount() - 1);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            };
            messagesDatabaseReference.addChildEventListener(childEventListener);
        }
    }

    @Override
    protected void onPause() {
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
        detachDatabaseReadListener();
        chatMessageAdapter.clearAll();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Signed in", Toast.LENGTH_SHORT).show();
            }
            else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        else if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            StorageReference photoRef = photosStorageReference.child(imageUri.getLastPathSegment());
            // Upload file to storage
            photoRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    if (downloadUrl != null) {
                        ChatMessage chatMessage = new ChatMessage(userName, null, downloadUrl.toString());
                        messagesDatabaseReference.push().setValue(chatMessage);
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sign_out:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(View view, int position) {
    }
}
