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
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.jaagora.urocket.MainActivity;
import com.jaagora.urocket.R;

import java.util.HashMap;
import java.util.Map;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

import static app.akexorcist.bluetotohspp.library.BluetoothState.REQUEST_ENABLE_BT;

public class BluetoothPairActivity extends AppCompatActivity {

    BluetoothSPP bt;
    FirebaseFunctions functions;
    String rocket_address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_pair);

        functions = FirebaseFunctions.getInstance();

        bt = new BluetoothSPP(this);
        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                showSuccess();
                Log.d("DEVDEBUG", "onConnect");
                bt.send("[" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "]", true);
                rocket_address = address;

            }

            @Override
            public void onDeviceDisconnected() {
                Log.d("DEVDEBUG", "Disconnected");
            }

            @Override
            public void onDeviceConnectionFailed() {
                Log.d("DEVDEBUG", "Failed");
            }
        });

        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                Log.d("DEVDEBUG", "BT Received: " + message);
            }
        });

        if (!bt.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            bt.setupService();
            bt.startService(BluetoothState.DEVICE_OTHER);
            Intent intent = new Intent(getApplicationContext(), DeviceList.class);
            startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("DEVDEBUG", bt.toString());
                bt.connect(data);
            }
            else{
                finish();
            }
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            startActivity(new Intent(this, BluetoothPairActivity.class));
            finish();
        }
    }

    public void submit(View v) {
        EditText name = findViewById(R.id.rocket_name_edit);
        if (name.getText().length() == 0) {
            Snackbar.make(v, "Name field cannot be empty!",
                    Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }

        TextView attempting = findViewById(R.id.attempting_tv);
        attempting.setText("Saving information");
        showLoading();

        Switch is_public = findViewById(R.id.is_public_switch);

        final Context c = this;
        addNewRocket(name.getText().toString(), is_public.isChecked())
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        startActivity(new Intent(c, MainActivity.class));
                        finish();
                    }
                });
    }

    private Task<String> addNewRocket(String name, boolean is_public) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("is_public", is_public);
        data.put("owner", FirebaseAuth.getInstance().getCurrentUser().getUid());
        data.put("address", rocket_address);

        return functions
                .getHttpsCallable("addNewRocket")
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

    public void showLoading(){
        ConstraintLayout loading = findViewById(R.id.loading_view);
        ConstraintLayout pair_success = findViewById(R.id.pair_success_view);

        loading.setVisibility(View.VISIBLE);
        pair_success.setVisibility(View.GONE);
    }

    public void showSuccess(){
        ConstraintLayout loading = findViewById(R.id.loading_view);
        ConstraintLayout pair_success = findViewById(R.id.pair_success_view);

        loading.setVisibility(View.GONE);
        pair_success.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            Log.d("DEVDEBUG", "Back press detected on BluetoothPairActivity");
            //bt.stopService();
            finish();
            //onDestroy();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        Log.d("DEVDEBUG", "DESTROYING BluetoothPairActivity");
        bt.setBluetoothConnectionListener(null);
        bt.setOnDataReceivedListener(null);
        bt.stopService();
        super.onDestroy();
    }
}

