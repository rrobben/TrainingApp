package com.example.android.harjoitus7_8;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public static final String ANONYMOUS = "anonymous";
    public static final int RC_SIGN_IN = 1;

    private EditText mDate;
    private EditText mSport;
    private EditText mDuration;
    private EditText mRpe;
    private EditText mSharpness;

    private RecyclerView mRecyclerView;
    private FirebaseRecyclerAdapter mTrainingAdapter;

    private String mUid;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mTrainingDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle(R.string.training_entries);

        mUid = ANONYMOUS;

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mTrainingDatabaseReference = mFirebaseDatabase.getReference().child("entries");

        mDate = (EditText) findViewById(R.id.et_date);
        mSport = (EditText) findViewById(R.id.et_sport);
        mDuration = (EditText) findViewById(R.id.et_duration);
        mRpe = (EditText) findViewById(R.id.et_rpe);
        mSharpness = (EditText) findViewById(R.id.et_sharpness);

        mRecyclerView = (RecyclerView) findViewById(R.id.all_entries_list_view);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

        mDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showDatePickerDialog(v);
                }
            }
        });

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                deleteEntry(viewHolder.getAdapterPosition());
            }
        }).attachToRecyclerView(mRecyclerView);

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    onSignedInInitialize(user.getUid());
                } else {
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.GoogleBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mFirebaseAuth.getCurrentUser();

        if (user != null) {
            mUid = user.getUid();

            Query query = mTrainingDatabaseReference.child(mUid).orderByChild("time");

            FirebaseRecyclerOptions<TrainingEntry> options =
                    new FirebaseRecyclerOptions.Builder<TrainingEntry>()
                            .setQuery(query, TrainingEntry.class)
                            .build();

            mTrainingAdapter = new TrainingAdapter(options);
            mRecyclerView.setAdapter(mTrainingAdapter);
            mTrainingAdapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mTrainingAdapter != null) {
            mTrainingAdapter.stopListening();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.activity_chart_menu:
                startActivity(new Intent(this, ChartActivity.class));
                return true;
            case R.id.weekly_chart_menu:
                startActivity(new Intent(this, WeeklyChartActivity.class));
                return true;
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onSignedInInitialize(String uid) {
        mUid = uid;
    }

    private void onSignedOutCleanup() {
        mUid = ANONYMOUS;
    }

    public void addNewEntry(View view) {
        if (mDate.getText().length() == 0 ||
                mDuration.getText().length() == 0 ||
                mRpe.getText().length() == 0 ||
                mSharpness.getText().length() == 0) {
            return;
        }

        float duration = 1;
        int durationMinutes = 0;
        int rpe = 1;
        int sharpness = 1;

        try {
            duration = Float.parseFloat(mDuration.getText().toString());
            rpe = Integer.parseInt(mRpe.getText().toString());
            sharpness = Integer.parseInt(mSharpness.getText().toString());
            durationMinutes = Math.round(duration * 60);

            SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
            Date date = df.parse(mDate.getText().toString());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            // Validate input values
            if (calendar.getTimeInMillis() >= 0 && durationMinutes > 0 && rpe > 0 && rpe <= 10 && sharpness > 0 && sharpness <= 10) {
                createNewEntry(calendar.getTimeInMillis(), durationMinutes, rpe, sharpness, mSport.getText().toString());
                Toast toast = Toast.makeText(this, R.string.entry_created, Toast.LENGTH_LONG);
                toast.show();
            } else {
                // Give error message about inputs
                Toast toast = Toast.makeText(this, R.string.error_entry, Toast.LENGTH_LONG);
                toast.show();
            }
        } catch (Exception ex) {
            Toast toast = Toast.makeText(this, R.string.error_entry, Toast.LENGTH_LONG);
            toast.show();
            Log.e("error", ex.getMessage());
        }
    }

    private void createNewEntry(long time, int duration, int rpe, int sharpness, String sport) {
        TrainingEntry trainingEntry = new TrainingEntry(time, duration, rpe, sharpness, sport);
        mTrainingDatabaseReference.child(mUid).push().setValue(trainingEntry);
    }

    private void deleteEntry(int position) {
        mTrainingAdapter.getRef(position).removeValue();
    }

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getFragmentManager(), "datePicker");
    }
}
