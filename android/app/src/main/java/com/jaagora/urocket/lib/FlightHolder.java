package com.jaagora.urocket.lib;

import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.jaagora.urocket.R;

public class FlightHolder extends RecyclerView.ViewHolder {
    public TextView launch_timestamp;

    public FlightHolder(View view) {
        super(view);
        launch_timestamp = view.findViewById(R.id.launch_timestamp);
    }
}
