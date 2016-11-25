package com.example.firebaseapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.editable;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

public class MainActivity extends AppCompatActivity implements OnItemClickListener {

    private static final int MAX = 1000;
    private static final String DEFAULT_USERNAME = "Anonymous";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseDatabase = FirebaseDatabase.getInstance();
        messagesDatabaseReference = firebaseDatabase.getReference().child("messages");

        photoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        messageEditText = (EditText) findViewById(R.id.messageEditText);
        sendButton = (Button) findViewById(R.id.sendButton);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        chatRecyclerView = (RecyclerView) findViewById(R.id.chatRecyclerView);

        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(linearLayoutManager);

        chatMessages = new ArrayList<>();
        /*for (int i = 0; i < 50; i++) {
            ChatMessage c1 = new ChatMessage();
            c1.setMsg("Message " + i);
            c1.setName("Name " + i);
            chatMessages.add(c1);
        }*/

        chatMessageAdapter = new ChatMessageAdapter(chatMessages);
        chatRecyclerView.setAdapter(chatMessageAdapter);

        chatMessageAdapter.setClickListener(this);

        progressBar.setVisibility(ProgressBar.INVISIBLE);

        photoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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
                ChatMessage chatMessage = new ChatMessage(DEFAULT_USERNAME,
                        messageEditText.getText().toString(), null);
                messagesDatabaseReference.push().setValue(chatMessage);

                messageEditText.setText("");
            }
        });

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                chatMessageAdapter.addItem(chatMessage);
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

    @Override
    public void onItemClick(View view, int position) {

    }
}
