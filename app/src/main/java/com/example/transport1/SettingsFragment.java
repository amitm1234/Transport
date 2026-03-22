package com.example.transport1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    private Button btnLogout, btnSaveSettings;

    private EditText etDefaultGST, etDefaultBuyPrice, etDefaultSellPrice;
    private Spinner spDefaultUnit;

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Buttons
        btnLogout = view.findViewById(R.id.btnLogout);
        btnSaveSettings = view.findViewById(R.id.btnSaveSettings);

        // EditTexts
        etDefaultGST = view.findViewById(R.id.etDefaultGST);
        etDefaultBuyPrice = view.findViewById(R.id.etDefaultBuyPrice);
        etDefaultSellPrice = view.findViewById(R.id.etDefaultSellPrice);

        // Spinner
        spDefaultUnit = view.findViewById(R.id.spDefaultUnit);

        // SharedPreferences
        prefs = requireActivity().getSharedPreferences("AppSettings", getContext().MODE_PRIVATE);

        // Spinner data
        String[] units = {"Select Unit","Ton","Quintal"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                units
        );
        spDefaultUnit.setAdapter(adapter);

        // Load saved values
        String savedGST = prefs.getString("default_gst", "18");
        String savedBuyPrice = prefs.getString("default_buy_price", "");
        String savedSellPrice = prefs.getString("default_sell_price", "");
        String savedUnit = prefs.getString("default_unit", "Select Unit");

        etDefaultGST.setText(savedGST);
        etDefaultBuyPrice.setText(savedBuyPrice);
        etDefaultSellPrice.setText(savedSellPrice);

        // Set spinner selection
        for (int i = 0; i < units.length; i++) {
            if (units[i].equals(savedUnit)) {
                spDefaultUnit.setSelection(i);
                break;
            }
        }

        // Save Settings
        btnSaveSettings.setOnClickListener(v -> {

            String gst = etDefaultGST.getText().toString().trim();
            String buyPrice = etDefaultBuyPrice.getText().toString().trim();
            String sellPrice = etDefaultSellPrice.getText().toString().trim();
            String unit = spDefaultUnit.getSelectedItem().toString();

            SharedPreferences.Editor editor = prefs.edit();

            editor.putString("default_gst", gst);
            editor.putString("default_buy_price", buyPrice);
            editor.putString("default_sell_price", sellPrice);
            editor.putString("default_unit", unit);

            editor.apply();

            Toast.makeText(getContext(),"Settings Saved",Toast.LENGTH_SHORT).show();
        });

        // Logout
        btnLogout.setOnClickListener(v -> {

            FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }
}