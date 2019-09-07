package com.jaagora.urocket.lib;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.jaagora.urocket.R;

import java.text.SimpleDateFormat;
import java.util.List;

public class Flight {
    private Timestamp launch_timestamp;
    private DocumentReference rocket;
    private List<Telemetry> telemetry;

    public Flight() {
    }

    public Flight(Timestamp launch_timestamp, DocumentReference rocket, List<Telemetry> telemetry) {
        this.launch_timestamp = launch_timestamp;
        this.rocket = rocket;
        this.telemetry = telemetry;
    }

    public Timestamp getLaunch_timestamp() {
        return launch_timestamp;
    }

    public String getPrettyLaunch_timestamp() {
        SimpleDateFormat simpleDate =  new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
        return simpleDate.format(launch_timestamp.toDate());
    }

    public DocumentReference getRocket() {
        return rocket;
    }

    public List<Telemetry> getTelemetry() {
        return telemetry;
    }
}