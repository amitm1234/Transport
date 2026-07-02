package com.example.transport1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HomeFragment extends Fragment {

    FloatingActionButton fabAddEntry;
    MaterialCardView cardReports, cardViewData;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        fabAddEntry = view.findViewById(R.id.fabAddEntry);
        cardReports = view.findViewById(R.id.cardReports);
        cardViewData = view.findViewById(R.id.cardViewData);

        fabAddEntry.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEntryActivity.class);
            startActivity(intent);
        });

        cardReports.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ReportsActivity.class);
            startActivity(intent);
        });

        cardViewData.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ViewDataActivity.class);
            startActivity(intent);
        });

        return view;
    }
}
