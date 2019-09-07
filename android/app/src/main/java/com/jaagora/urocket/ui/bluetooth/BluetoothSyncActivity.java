package com.jaagora.urocket.ui.bluetooth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jaagora.urocket.MainActivity;
import com.jaagora.urocket.R;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;

import static app.akexorcist.bluetotohspp.library.BluetoothState.REQUEST_ENABLE_BT;

public class BluetoothSyncActivity extends AppCompatActivity {

    BluetoothSPP bt;
    FirebaseFunctions functions;
    DocumentReference rocket;
    FirebaseFirestore db;
    boolean over;
    List<JSONObject> telemetry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_sync);
        startLogic();
    }

    public void startLogic() {
        db = FirebaseFirestore.getInstance();
        over = false;
        telemetry = new ArrayList<>();

        Intent intent = getIntent();
        String rocket_id = intent.getStringExtra("rocket");
        Log.d("DEVDEBUG", "Rocket_ID: " + rocket_id);
        rocket = db.collection("rockets").document(rocket_id);

        functions = FirebaseFunctions.getInstance();

        bt = new BluetoothSPP(this);
        final TextView attempting = findViewById(R.id.attempting_tv);
        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                Log.d("DEVDEBUG", "onConnect");
                attempting.setText("Connection successful\n      Transferring data");
                bt.send("[" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "]", true);
            }

            @Override
            public void onDeviceDisconnected() {
                Log.d("DEVDEBUG", "Disconnected");
                attempting.setText("Attempting to connect");
                restart();
            }

            @Override
            public void onDeviceConnectionFailed() {
                Log.d("DEVDEBUG", "Failed");
                attempting.setText("Attempting to connect");
                restart();
            }
        });


        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                Log.d("DEVDEBUG", "BT Received: " + message);
                if (message.length() >= 9 && message.substring(0, 9).equals("Connected")) {
                    bt.send("[" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "]", true);
                } else if (message.length() >= 4 && message.substring(0, 4).equals("NULL")) {
                    showEmpty();
                } else if (message.length() >= 5 && message.substring(0, 5).equals("ERROR")) {
                    showFail();
                } else if (message.length() >= 4 && message.substring(0, 4).equals("OVER")) {
                    over = true;
                    showSuccess();
                } else if (!over) {
                    try {
                        telemetry.add(new JSONObject(message));
                    } catch (Exception e) {
                        // TODO
                    }
                }
            }
        });

        if (!bt.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            bt.setupService();
            bt.startService(BluetoothState.DEVICE_OTHER);

            rocket.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d("DEVDEBUG", "DocumentSnapshot data: " + document.getData());
                            String addr = (String) document.getData().get("address");
                            bt.connect(addr);
                        } else {
                            Log.d("DEVDEBUG", "No such document");
                        }
                    } else {
                        Log.d("DEVDEBUG", "get failed with ", task.getException());
                    }
                }
            });
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            restart();
        }
    }

    public void submit(View v) {
        EditText name = findViewById(R.id.flight_name_edit);
        if (name.getText().length() == 0) {
            Snackbar.make(v, "Name field cannot be empty!",
                    Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }

        TextView attempting = findViewById(R.id.attempting_tv);
        attempting.setText("Saving information");
        showLoading();

        final Context c = this;
        addNewFlight(name.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        startActivity(new Intent(c, MainActivity.class));
                        finish();
                    }
                });
    }

    private Task<String> addNewFlight(String name) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("rocket", rocket.getId());
        data.put("launch_timestamp", toISO8601UTC(new Date()));
        data.put("telemetry", telemetry);

        return functions
                .getHttpsCallable("addNewFlight")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        String result = (String) task.getResult().getData();
                        return result;
                    }
                });
    }

    public void showLoading() {
        ConstraintLayout loading = findViewById(R.id.loading_view);
        ConstraintLayout sync_success = findViewById(R.id.sync_success_view);
        ConstraintLayout sync_fail = findViewById(R.id.sync_fail_view);

        loading.setVisibility(View.VISIBLE);
        sync_success.setVisibility(View.GONE);
        sync_fail.setVisibility(View.GONE);
    }

    public void showSuccess() {
        ConstraintLayout loading = findViewById(R.id.loading_view);
        ConstraintLayout sync_success = findViewById(R.id.sync_success_view);
        ConstraintLayout sync_fail = findViewById(R.id.sync_fail_view);

        loading.setVisibility(View.GONE);
        sync_success.setVisibility(View.VISIBLE);
        sync_fail.setVisibility(View.GONE);
    }

    public void showFail() {
        TextView fail_info = findViewById(R.id.fail_info_tv);
        fail_info.setText("Error! Could not connect to the Rocket");
        showEmpty();
    }

    public void showEmpty() {
        ConstraintLayout loading = findViewById(R.id.loading_view);
        ConstraintLayout sync_success = findViewById(R.id.sync_success_view);
        ConstraintLayout sync_fail = findViewById(R.id.sync_fail_view);

        loading.setVisibility(View.GONE);
        sync_success.setVisibility(View.GONE);
        sync_fail.setVisibility(View.VISIBLE);
    }

    public void restart() {
        startLogic();
    }

    public void goBack(View v) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // https://gist.github.com/nickrussler/7527851
    public static String toISO8601UTC(Date date) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        return df.format(date);
    }
}

