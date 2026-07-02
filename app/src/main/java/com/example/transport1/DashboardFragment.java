package com.example.transport1;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DashboardFragment extends Fragment {

    TextView txtTotalSellWeight, txtTotalSellAmount, txtTotalBuyWeight, txtTotalBuyAmount;
    TextView txtVehiclesSold, txtCustomersServed;

    MaterialButton btnFilter;
    Button btnViewReports;

    DatabaseReference transportRef;
    String uid;

    double totalSellWeight, totalSellAmount, totalBuyWeight, totalBuyAmount;
    int totalVehicleCount;
    Set<String> totalCustomers = new HashSet<>();

    private String startDate = "", endDate = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        txtTotalSellWeight = view.findViewById(R.id.txtTotalSellWeight);
        txtTotalSellAmount = view.findViewById(R.id.txtTotalSellAmount);
        txtTotalBuyWeight = view.findViewById(R.id.txtTotalBuyWeight);
        txtTotalBuyAmount = view.findViewById(R.id.txtTotalBuyAmount);
        txtVehiclesSold = view.findViewById(R.id.txtVehiclesSold);
        txtCustomersServed = view.findViewById(R.id.txtCustomersServed);

        btnFilter = view.findViewById(R.id.btnFilter);
        btnViewReports = view.findViewById(R.id.btnViewReports);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        transportRef = FirebaseDatabase.getInstance().getReference("transport_data").child(uid);

        btnViewReports.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), ReportsActivity.class)));

        // Filter popup
        btnFilter.setOnClickListener(v -> {

            PopupMenu popup = new PopupMenu(getContext(), btnFilter);

            popup.getMenu().add("Today");
            popup.getMenu().add("Yesterday");
            popup.getMenu().add("All-Time");
            popup.getMenu().add("Custom");

            popup.setOnMenuItemClickListener(item -> {

                String selected = item.getTitle().toString();

                if(selected.equals("Today")){
                    btnFilter.setText("Today ▼");
                    fetchDashboardData("today");
                }

                if(selected.equals("Yesterday")){
                    btnFilter.setText("Yesterday ▼");
                    fetchDashboardData("yesterday");
                }

                if(selected.equals("All-Time")){
                    btnFilter.setText("All-Time ▼");
                    fetchDashboardData("all");
                }

                if(selected.equals("Custom")){
                    btnFilter.setText("Custom ▼");
                    showCustomDatePicker();
                }

                return true;
            });

            popup.show();
        });

        // Default load
        fetchDashboardData("today");

        return view;
    }

    private void fetchDashboardData(String filterType) {

        totalSellWeight = totalSellAmount = totalBuyWeight = totalBuyAmount = 0;
        totalVehicleCount = 0;
        totalCustomers.clear();

        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        String todayDate = sdf.format(new Date());
        String yesterdayDate = sdf.format(new Date(System.currentTimeMillis() - 86400000));

        transportRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot data : snapshot.getChildren()) {

                    String sellPerson = data.child("sellPerson").getValue(String.class);
                    String date = data.child("date").getValue(String.class);

                    double sellWeight = parseDoubleSafe(data.child("sellWeight").getValue(String.class));
                    double sellTotal = parseDoubleSafe(data.child("sellTotalAmount").getValue(String.class));
                    double buyWeight = parseDoubleSafe(data.child("buyWeight").getValue(String.class));
                    double buyTotal = parseDoubleSafe(data.child("buyTotalAmount").getValue(String.class));

                    boolean include = false;

                    switch(filterType){
                        case "today":
                            include = date != null && date.equals(todayDate);
                            break;

                        case "yesterday":
                            include = date != null && date.equals(yesterdayDate);
                            break;

                        case "all":
                            include = true;
                            break;

                        case "custom":
                            include = date != null && isDateInRange(date);
                            break;
                    }

                    if(include){
                        totalSellWeight += sellWeight;
                        totalSellAmount += sellTotal;
                        totalBuyWeight += buyWeight;
                        totalBuyAmount += buyTotal;
                        totalVehicleCount++;

                        if(sellPerson != null && !sellPerson.isEmpty())
                            totalCustomers.add(sellPerson);
                    }
                }

                updateDashboardUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showCustomDatePicker(){

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog startPicker = new DatePickerDialog(getContext(),
                (view, year, month, day) -> {

                    startDate = day + "/" + (month+1) + "/" + year;

                    DatePickerDialog endPicker = new DatePickerDialog(getContext(),
                            (v, y, m, d) -> {

                                endDate = d + "/" + (m+1) + "/" + y;

                                btnFilter.setText(startDate + " - " + endDate + " ▼");

                                fetchDashboardData("custom");

                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH));

                    endPicker.show();

                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        startPicker.show();
    }

    private boolean isDateInRange(String dateStr){

        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());

        try{

            Date date = sdf.parse(dateStr);
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);

            return date != null && !date.before(start) && !date.after(end);

        } catch(ParseException e){
            return false;
        }
    }

    private void updateDashboardUI(){

        txtTotalSellWeight.setText("Total Sell Weight: " + totalSellWeight + " Ton");
        txtTotalSellAmount.setText("Total Sell Amount: ₹" + totalSellAmount);
        txtTotalBuyWeight.setText("Total Buy Weight: " + totalBuyWeight + " Ton");
        txtTotalBuyAmount.setText("Total Buy Amount: ₹" + totalBuyAmount);

        txtVehiclesSold.setText("Vehicles Sold: " + totalVehicleCount);
        txtCustomersServed.setText("Customers Served: " + totalCustomers.size());
    }

    private double parseDoubleSafe(String val){

        try{
            return val != null ? Double.parseDouble(val) : 0;
        }
        catch(Exception e){
            return 0;
        }
    }
}