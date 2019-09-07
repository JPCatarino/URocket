package com.jaagora.urocket.ui.home;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.jaagora.urocket.MainActivity;
import com.jaagora.urocket.R;
import com.jaagora.urocket.lib.Flight;
import com.jaagora.urocket.lib.FlightHolder;

import java.util.ArrayList;

public class HomeFragment extends Fragment {
    private HomeViewModel homeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user.isAnonymous()) {
            return onCreateAnon(inflater, container);
        } else {
            return onCreateNormal(user, inflater, container);
        }
    }

    private View onCreateNormal(FirebaseUser user, @NonNull LayoutInflater inflater,
                                ViewGroup container) {
        final Context c = getActivity();
        final View root = inflater.inflate(R.layout.fragment_home, container, false);

        final Spinner rocket_spinner = root.findViewById(R.id.rocket_spinner);
        final ArrayList<String> spinner_elements = new ArrayList<>();
        final ArrayList<DocumentReference> spinner_refs = new ArrayList<>();

        final RecyclerView flights_view = root.findViewById(R.id.flights_view_rv);
        LinearLayoutManager flights_layoutManager = new LinearLayoutManager(c);
        FirestoreRecyclerAdapter adapter;
        flights_layoutManager.setReverseLayout(true);
        flights_layoutManager.setStackFromEnd(true);
        flights_view.setLayoutManager(flights_layoutManager);

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(user.getUid());
        CollectionReference rocketsRef = db.collection("rockets");
        rocketsRef.whereEqualTo("owner", userRef)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                spinner_elements.add((String) document.getData().get("name"));
                                spinner_refs.add(document.getReference());
                            }

                            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(c, android.R.layout.simple_spinner_item, spinner_elements);
                            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            rocket_spinner.setAdapter(arrayAdapter);
                            rocket_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                                    Log.d("DevDebug", "at onItemSelected. " + spinner_refs.get(position));
                                    CollectionReference flightsRef = db.collection("flights");
                                    Query baseQuery = flightsRef.whereEqualTo("rocket", spinner_refs.get(position));

                                    FirestoreRecyclerOptions<Flight> options = new FirestoreRecyclerOptions.Builder<Flight>()
                                            .setQuery(baseQuery, Flight.class)
                                            .build();

                                    FirestoreRecyclerAdapter adapter = new FirestoreRecyclerAdapter<Flight, FlightHolder>(options) {
                                        @Override
                                        public void onBindViewHolder(FlightHolder holder, int position, Flight flight) {
                                            holder.launch_timestamp.setText(flight.getPrettyLaunch_timestamp());
                                        }

                                        @Override
                                        public FlightHolder onCreateViewHolder(ViewGroup group, int i) {

                                            View view = LayoutInflater.from(group.getContext())
                                                    .inflate(R.layout.flight_list_item, group, false);

                                            return new FlightHolder(view);
                                        }
                                    };

                                    adapter.notifyDataSetChanged();
                                    flights_view.setAdapter(adapter);
                                    if (adapter != null) adapter.startListening();
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {
                                }
                            });
                        } else {
                            Log.d("DevDebug", "Error getting documents: ", task.getException());
                        }
                    }
                });

        return root;
    }

    private View onCreateAnon(@NonNull LayoutInflater inflater,
                              ViewGroup container) {
        View root = inflater.inflate(R.layout.fragment_home_anon, container, false);
        ((Button) root.findViewById(R.id.sign_in_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Context c = getActivity();
                AuthUI.getInstance()
                        .signOut(c)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                getActivity().finish();
                                startActivity(new Intent(c, MainActivity.class));
                            }
                        });
            }
        });
        return root;
    }


}