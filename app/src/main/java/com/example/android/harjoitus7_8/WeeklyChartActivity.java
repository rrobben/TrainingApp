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
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
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
import java.util.HashMap;

import static com.example.android.harjoitus7_8.MainActivity.RC_SIGN_IN;

public class WeeklyChartActivity extends AppCompatActivity {
    private CombinedChart chart;

    public static final String ANONYMOUS = "anonymous";
    private String mUid;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mTrainingDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_chart);

        setTitle(R.string.weekly_chart);

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
        rightAxis.setAxisMinimum(0f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);


        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return ((int) value % 52 == 0 ? "52" : String.valueOf((int) value % 52));
            }
        });

        CombinedData data = new CombinedData();

        data.setData(generateLineData(dataSnapshot));
        data.setData(generateBarData(dataSnapshot));

        leftAxis.setAxisMaximum(data.getYMax() + 100f);
        rightAxis.setAxisMaximum(data.getYMax(rightAxis.getAxisDependency()) + 0.5f);
        xAxis.setAxisMinimum(data.getXMin() - 0.5f);
        xAxis.setAxisMaximum(data.getXMax() + 0.5f);

        chart.setData(data);
        chart.invalidate();
    }

    private LineData generateLineData(DataSnapshot dataSnapshot) {
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<Entry> entries2 = new ArrayList<>();
        HashMap<Integer, Integer> map = new HashMap<>();
        HashMap<Integer, Integer> results = new HashMap<>();
        int startingYear = 0;

        for (DataSnapshot entrySnapshot: dataSnapshot.getChildren()) {
            TrainingEntry entry = entrySnapshot.getValue(TrainingEntry.class);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(entry.getTime());
            int year = calendar.get(Calendar.YEAR);

            if (startingYear == 0) {
                startingYear = year;
            }

            int week = (year - startingYear) * 52 + calendar.get(Calendar.WEEK_OF_YEAR);

            if (map.containsKey(week)) {
                map.put(week, entry.getRpe() * entry.getDuration() + map.get(week));
            } else {
                map.put(week, entry.getRpe() * entry.getDuration());
            }
        }

        for (int key: map.keySet()) {
            int sum = 0;

            for (int i = 0; i < 4; i++) {
                if (map.containsKey(key - i)) {
                    sum += map.get(key - i);
                }
            }

            int average = Math.round(sum / 4);
            results.put(key, average);
        }

        for (int key: results.keySet()) {
            entries.add(new Entry(key, results.get(key)));
        }

        for (int key: results.keySet()) {
            entries2.add(new Entry(key, (float) map.get(key) / (float) results.get(key)));
        }

        LineDataSet set = new LineDataSet(entries, "4 wk average");
        set.setColor(Color.rgb(240, 238, 70));
        set.setLineWidth(2.5f);
        set.setCircleColor(Color.rgb(240, 238, 70));
        set.setCircleRadius(5f);
        set.setFillColor(Color.rgb(240, 238, 70));
        set.setMode(LineDataSet.Mode.LINEAR);
        set.setDrawValues(true);
        set.setValueTextSize(10f);
        set.setValueTextColor(Color.rgb(240, 238, 70));

        set.setAxisDependency(YAxis.AxisDependency.LEFT);


        LineDataSet set2 = new LineDataSet(entries2, "Injury forecast");
        set2.setColor(Color.rgb(240, 23, 7));
        set2.setLineWidth(2.5f);
        set2.setCircleColor(Color.rgb(240, 23, 7));
        set2.setCircleRadius(5f);
        set2.setFillColor(Color.rgb(240, 23, 7));
        set2.setMode(LineDataSet.Mode.LINEAR);
        set2.setDrawValues(true);
        set2.setValueTextSize(10f);
        set2.setValueTextColor(Color.rgb(240, 23, 7));
        set2.setAxisDependency(YAxis.AxisDependency.RIGHT);
        set2.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return String.valueOf((float) Math.round(value * 100) / 100);
            }
        });

        LineData d = new LineData(set, set2);

        return d;
    }

    private BarData generateBarData(DataSnapshot dataSnapshot) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        HashMap<Integer, Integer> map = new HashMap<>();
        int startingYear = 0;

        for (DataSnapshot entrySnapshot: dataSnapshot.getChildren()) {
            TrainingEntry entry = entrySnapshot.getValue(TrainingEntry.class);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(entry.getTime());
            int year = calendar.get(Calendar.YEAR);

            if (startingYear == 0) {
                startingYear = year;
            }

            int week = (year - startingYear) * 52 + calendar.get(Calendar.WEEK_OF_YEAR);

            if (map.containsKey(week)) {
                map.put(week, entry.getRpe() * entry.getDuration() + map.get(week));
            } else {
                map.put(week, entry.getRpe() * entry.getDuration());
            }
        }

        for (int key: map.keySet()) {
            entries.add(new BarEntry(key, map.get(key)));
        }

        BarDataSet set1 = new BarDataSet(entries, "sRPE");
        set1.setColor(Color.rgb(60, 220, 78));
        set1.setValueTextColor(Color.rgb(60, 220, 78));
        set1.setValueTextSize(10f);
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);

        float barWidth = 0.5f; // x2 dataset

        BarData d = new BarData(set1);
        d.setBarWidth(barWidth);

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
            case R.id.activity_chart_menu:
                startActivity(new Intent(this, ChartActivity.class));
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
