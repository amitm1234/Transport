package com.example.transport1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class ReportsActivity extends AppCompatActivity {

    ListView listSellPerson, listFactory;

    ArrayList<String> sellPersonList = new ArrayList<>();
    ArrayList<String> factoryList = new ArrayList<>();

    // Person Calculations
    HashMap<String, Double> sellTotals = new HashMap<>();
    HashMap<String, Double> paidTotals = new HashMap<>();

    // Factory Calculations
    HashMap<String, Double> factoryTotals = new HashMap<>();
    HashMap<String, Double> factoryPaidTotals = new HashMap<>();

    DatabaseReference databaseReference;
    TextView txtGrandTotalSellRemaining, txtGrandTotalFactoryRemaining;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        listSellPerson = findViewById(R.id.listSellPerson);
        listFactory = findViewById(R.id.listFactory);
        txtGrandTotalSellRemaining = findViewById(R.id.txtGrandTotalSellRemaining);
        txtGrandTotalFactoryRemaining = findViewById(R.id.txtGrandTotalFactoryRemaining);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("transport_data")
                .child(uid);

        loadData();

        listSellPerson.setOnItemClickListener((parent, view, position, id) -> {
            String person = sellPersonList.get(position);
            Intent intent = new Intent(ReportsActivity.this, PersonReportActivity.class);
            intent.putExtra("personName", person);
            startActivity(intent);
        });

        listFactory.setOnItemClickListener((parent, view, position, id) -> {
            String factory = factoryList.get(position);
            Intent intent = new Intent(ReportsActivity.this, FactoryReportActivity.class);
            intent.putExtra("factoryName", factory);
            startActivity(intent);
        });
    }

    private void loadData() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                sellPersonList.clear();
                factoryList.clear();
                sellTotals.clear();
                paidTotals.clear();
                factoryTotals.clear();
                factoryPaidTotals.clear();

                // 1. Calculate Sell Totals and Factory Buy Totals from main records
                for (DataSnapshot data : snapshot.getChildren()) {
                    String key = data.getKey();
                    if (key != null && (key.equals("payments") || key.equals("factory_payments"))) continue;

                    String person = data.child("sellPerson").getValue(String.class);
                    String factory = data.child("factory").getValue(String.class);

                    // Safe parsing for sellTotalAmount
                    String sellStr = data.child("sellTotalAmount").getValue(String.class);
                    double sellAmount = 0;
                    if (sellStr != null && !sellStr.isEmpty()) {
                        try { sellAmount = Double.parseDouble(sellStr); } catch (Exception e) {}
                    }

                    // Safe parsing for buyPrice
                    String buyStr = data.child("buyPrice").getValue(String.class);
                    double buyPrice = 0;
                    if (buyStr != null && !buyStr.isEmpty()) {
                        try { buyPrice = Double.parseDouble(buyStr); } catch (Exception e) {}
                    }

                    if (person != null) {
                        if (!sellPersonList.contains(person)) sellPersonList.add(person);
                        double current = sellTotals.containsKey(person) ? sellTotals.get(person) : 0;
                        sellTotals.put(person, current + sellAmount);
                    }

                    if (factory != null) {
                        if (!factoryList.contains(factory)) factoryList.add(factory);
                        double current = factoryTotals.containsKey(factory) ? factoryTotals.get(factory) : 0;
                        factoryTotals.put(factory, current + buyPrice);
                    }
                }

                // 2. Calculate Paid Totals (Person)
                DataSnapshot paymentsSnapshot = snapshot.child("payments");
                for (DataSnapshot personPayments : paymentsSnapshot.getChildren()) {
                    String personName = personPayments.getKey();
                    double totalPaid = 0;
                    for (DataSnapshot payment : personPayments.getChildren()) {
                        String pAmountStr = payment.child("amount").getValue(String.class);
                        if (pAmountStr != null && !pAmountStr.isEmpty()) {
                            try { totalPaid += Double.parseDouble(pAmountStr); } catch (Exception e) {}
                        }
                    }
                    paidTotals.put(personName, totalPaid);
                }

                // 3. Calculate Factory Advance (factory_payments)
                DataSnapshot factoryPaySnapshot = snapshot.child("factory_payments");
                for (DataSnapshot fNode : factoryPaySnapshot.getChildren()) {
                    String fName = fNode.getKey();
                    double totalAdvance = 0;
                    for (DataSnapshot payment : fNode.getChildren()) {
                        String fAmountStr = payment.child("amount").getValue(String.class);
                        if (fAmountStr != null && !fAmountStr.isEmpty()) {
                            try { totalAdvance += Double.parseDouble(fAmountStr); } catch (Exception e) {}
                        }
                    }
                    factoryPaidTotals.put(fName, totalAdvance);
                }

                // Custom Adapter for Sell Person
                ArrayAdapter<String> sellAdapter = new ArrayAdapter<String>(ReportsActivity.this,
                        R.layout.item_sell_person, android.R.id.text1, sellPersonList) {
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        String personName = getItem(position);
                        TextView txtAmount = view.findViewById(R.id.txtAmount);

                        double sell = sellTotals.containsKey(personName) ? sellTotals.get(personName) : 0;
                        double paid = paidTotals.containsKey(personName) ? paidTotals.get(personName) : 0;
                        double remaining = sell - paid;

                        txtAmount.setText("₹ " + String.format("%.2f", remaining));
                        return view;
                    }
                };

                // Custom Adapter for Factory
                ArrayAdapter<String> factoryAdapter = new ArrayAdapter<String>(ReportsActivity.this,
                        R.layout.item_factory, android.R.id.text1, factoryList) {
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        String fName = getItem(position);
                        TextView txtAmount = view.findViewById(R.id.txtAmount);

                        double totalBuy = factoryTotals.containsKey(fName) ? factoryTotals.get(fName) : 0;
                        double advance = factoryPaidTotals.containsKey(fName) ? factoryPaidTotals.get(fName) : 0;
                        double remaining = advance - totalBuy;

                        txtAmount.setText("₹ " + String.format("%.2f", remaining));
                        return view;
                    }
                };

                listSellPerson.setAdapter(sellAdapter);
                listFactory.setAdapter(factoryAdapter);
// 🔹 Grand Total Sell Remaining
                double grandTotalSellRemaining = 0;
                for (String person : sellPersonList) {
                    double sell = sellTotals.containsKey(person) ? sellTotals.get(person) : 0;
                    double paid = paidTotals.containsKey(person) ? paidTotals.get(person) : 0;
                    grandTotalSellRemaining += (sell - paid);
                }

// 🔹 Grand Total Factory Remaining
                double grandTotalFactoryRemaining = 0;
                for (String factory : factoryList) {
                    double buy = factoryTotals.containsKey(factory) ? factoryTotals.get(factory) : 0;
                    double adv = factoryPaidTotals.containsKey(factory) ? factoryPaidTotals.get(factory) : 0;
                    grandTotalFactoryRemaining += (adv - buy);
                }

// 🔹 UI update
                txtGrandTotalSellRemaining.setText("Total: ₹ " + String.format("%.2f", grandTotalSellRemaining));
                txtGrandTotalFactoryRemaining.setText("Total: ₹ " + String.format("%.2f", grandTotalFactoryRemaining));

            }



            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}
