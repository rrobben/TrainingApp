package com.example.android.harjoitus7_8;

import android.content.Intent;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import static com.example.android.harjoitus7_8.MainActivity.RC_SIGN_IN;
import static com.example.android.harjoitus7_8.R.id.activity_chart;

public class ChartActivity extends AppCompatActivity {

    private CombinedChart chart;
    private final int count = 12;

    public static final String ANONYMOUS = "anonymous";
    private String mUid;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mTrainingDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        setTitle(R.string.activity_chart);

        mUid = ANONYMOUS;

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mTrainingDatabaseReference = mFirebaseDatabase.getReference().child("entries");

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

    private void drawChart(DataSnapshot dataSnapshot) {
        chart = findViewById(R.id.activity_chart);
        chart.getDescription().setEnabled(false);
        chart.setBackgroundColor(Color.WHITE);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setHighlightFullBarEnabled(false);

        // draw bars behind lines
        chart.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE
        });

        Legend l = chart.getLegend();
        l.setWordWrapEnabled(true);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setDrawGridLines(false);
        rightAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)
        rightAxis.setAxisMaximum(10f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)
        //leftAxis.setAxisMaximum(2000f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        //xAxis.setAxisMinimum(f);
        xAxis.setGranularity(86400000f);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis((long)value);
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM");
                return dateFormat.format(calendar.getTime());
            }
        });

        CombinedData data = new CombinedData();

        data.setData(generateLineData(dataSnapshot));
        data.setData(generateBarData(dataSnapshot));
        //data.setValueTypeface(tfLight);

        leftAxis.setAxisMaximum(data.getYMax() + 100f);
        //xAxis.setAxisMinimum(1545256800000f);
        //xAxis.setAxisMaximum(1545516000000f);
        xAxis.setAxisMinimum(data.getXMin() - 46400000f);
        xAxis.setAxisMaximum(data.getXMax() + 46400000f);

        chart.setData(data);
        chart.invalidate();
    }

    private LineData generateLineData(DataSnapshot dataSnapshot) {
        LineData d = new LineData();
        ArrayList<Entry> entries = new ArrayList<>();

        for (DataSnapshot entrySnapshot: dataSnapshot.getChildren()) {
            TrainingEntry entry = entrySnapshot.getValue(TrainingEntry.class);

            entries.add(new Entry(entry.getTime(), entry.getRpe() * entry.getDuration()));
        }

        //for (int index = 0; index < count; index++)
        //    mEntries.add(new Entry("20.12.2018",10));

        LineDataSet set = new LineDataSet(entries, "sRPE");
        set.setColor(Color.rgb(240, 238, 70));
        set.setLineWidth(2.5f);
        set.setCircleColor(Color.rgb(240, 238, 70));
        set.setCircleRadius(5f);
        set.setFillColor(Color.rgb(240, 238, 70));
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(true);
        set.setValueTextSize(10f);
        set.setValueTextColor(Color.rgb(240, 238, 70));

        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        d.addDataSet(set);

        return d;
    }

    private BarData generateBarData(DataSnapshot dataSnapshot) {
        ArrayList<BarEntry> entries = new ArrayList<>();

        for (DataSnapshot entrySnapshot: dataSnapshot.getChildren()) {
            TrainingEntry entry = entrySnapshot.getValue(TrainingEntry.class);
            entries.add(new BarEntry(entry.getTime(), entry.getSharpness()));
        }

       // for (int index = 0; index < count; index++) {
        //    entries1.add(new BarEntry(0,25));

            // stacked
       //     entries2.add(new BarEntry(0, new float[]{12, 13}));
       // }

        BarDataSet set1 = new BarDataSet(entries, "Sharpness");
        set1.setColor(Color.rgb(60, 220, 78));
        set1.setValueTextColor(Color.rgb(60, 220, 78));
        set1.setValueTextSize(10f);
        set1.setAxisDependency(YAxis.AxisDependency.RIGHT);

       // BarDataSet set2 = new BarDataSet(entries2, "");
       // set2.setStackLabels(new String[]{"Stack 1", "Stack 2"});
       // set2.setColors(Color.rgb(61, 165, 255), Color.rgb(23, 197, 255));
       // set2.setValueTextColor(Color.rgb(61, 165, 255));
       // set2.setValueTextSize(10f);
       // set2.setAxisDependency(YAxis.AxisDependency.LEFT);

        float groupSpace = 0.06f;
        float barSpace = 0.02f; // x2 dataset
        float barWidth = 46400000f; // x2 dataset
        // (0.45 + 0.02) * 2 + 0.06 = 1.00 -> interval per "group"

        BarData d = new BarData(set1);
        d.setBarWidth(barWidth);

        // make this BarData object grouped
       // d.groupBars(0, groupSpace, barSpace); // start at x = 0

        return d;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_activity_menu:
                startActivity(new Intent(this, MainActivity.class));
                return true;
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                   drawChart(dataSnapshot);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w("TAG", "loadPost:onCancelled", databaseError.toException());
                }
            });
        }
    }

    private void onSignedInInitialize(String uid) {
        mUid = uid;
    }

    private void onSignedOutCleanup() {
        mUid = ANONYMOUS;
    }
}
