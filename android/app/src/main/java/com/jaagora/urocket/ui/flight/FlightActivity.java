package com.jaagora.urocket.ui.flight;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.jaagora.urocket.R;
import com.jaagora.urocket.lib.Flight;
import com.jaagora.urocket.lib.Telemetry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FlightActivity extends AppCompatActivity {

    FirebaseFirestore db;
    Flight flight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Flight Statistics");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        String flight_id = intent.getStringExtra("flight");
        Log.d("DEVDEBUG", flight_id);

        db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("flights").document(flight_id);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                flight = documentSnapshot.toObject(Flight.class);
                TextView name = findViewById(R.id.flight_name_header);
                TextView time = findViewById(R.id.launch_timestamp);
                TextView max_alt = findViewById(R.id.max_alt);
                TextView flight_time = findViewById(R.id.flight_time);
                TextView max_accel = findViewById(R.id.max_accel);
                LineChart altitude_chart = (LineChart) findViewById(R.id.altitude_chart);
                LineChart acceleration_chart = (LineChart) findViewById(R.id.acceleration_chart);

                name.setText(flight.getName());
                time.setText(flight.getPrettyLaunch_timestamp());

                Double max_alt_temp = Double.NEGATIVE_INFINITY;
                Double max_timestamp_temp = 0.0;
                Double max_accel_temp = Double.NEGATIVE_INFINITY;

                List<Telemetry> telemetry = new ArrayList<>(flight.getTelemetry());
                Collections.sort(telemetry, new Comparator<Telemetry>() {
                    @Override
                    public int compare(Telemetry o1, Telemetry o2) {
                        return (int) (o1.getTimestamp() - o2.getTimestamp());
                    }
                });

                List<Entry> alti_time = new ArrayList<>();
                List<Entry> accel_time = new ArrayList<>();
                for(Telemetry t : telemetry){
                    if(t.getAltitude() > max_alt_temp) {
                        max_alt_temp = t.getAltitude();
                    }
                    if(t.getAcceleration() > max_accel_temp) max_accel_temp = t.getAcceleration() * 10;
                    if(t.getTimestamp() > max_timestamp_temp) max_timestamp_temp = t.getTimestamp();
                    alti_time.add(new Entry((float) (t.getTimestamp() / (float) 1000), t.getAltitude().floatValue()));
                    accel_time.add(new Entry((float) (t.getTimestamp() / (float) 1000), t.getAcceleration().floatValue() * (float) 10));
                }

                max_alt.setText(max_alt_temp + " m");
                flight_time.setText((max_timestamp_temp / 1000) + " s");
                max_accel.setText(max_accel_temp + " m/s²");

                LineDataSet alti_dataSet = new LineDataSet(alti_time, "Time (s)"); // add entries to dataset
                alti_dataSet.setDrawValues(false);

                LineData alti_lineData = new LineData(alti_dataSet);
                altitude_chart.getAxisRight().setDrawLabels(false);
                altitude_chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                altitude_chart.getDescription().setEnabled(false);
                altitude_chart.setData(alti_lineData);
                altitude_chart.getData().setHighlightEnabled(false);
                altitude_chart.invalidate(); // refresh

                LineDataSet accel_dataSet = new LineDataSet(accel_time, "Time (s)"); // add entries to dataset
                //accel_dataSet.setColor(Color.GREEN);
                //accel_dataSet.setCircleColor(Color.GREEN);
                accel_dataSet.setDrawValues(false);

                LineData accel_lineData = new LineData(accel_dataSet);
                acceleration_chart.getAxisRight().setDrawLabels(false);
                acceleration_chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                acceleration_chart.getDescription().setEnabled(false);
                acceleration_chart.setData(accel_lineData);
                acceleration_chart.getData().setHighlightEnabled(false);
                acceleration_chart.invalidate(); // refresh

            }
        });
    }

}
