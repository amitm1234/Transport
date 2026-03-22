package com.example.transport1;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class FactoryReportActivity extends BaseActivity {

    TextView txtFactoryName, txtAdvanceRemaining, txtTotalAdvanceGiven, txtTotalUsed, txtTotalWeight,txtTransactionCount;;
    ListView listFactoryRecords;
    Button btnAddAdvance, btnViewPaymentHistory;
    ImageView btnBack;

    ArrayList<FactoryTransactionModel> recordList = new ArrayList<>();
    FactoryTransactionAdapter adapter;

    DatabaseReference databaseReference;
    DatabaseReference advanceRef;

    double totalAdvance = 0;
    double totalUsedAmount = 0;
    double totalWeightSum = 0;
    String factoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_factory_report);

        // Initialize views
        txtFactoryName = findViewById(R.id.txtFactoryName);
        txtAdvanceRemaining = findViewById(R.id.txtAdvanceRemaining);
        txtTotalAdvanceGiven = findViewById(R.id.txtTotalAdvanceGiven);
        txtTotalUsed = findViewById(R.id.txtTotalUsed);
        txtTotalWeight = findViewById(R.id.txtTotalWeight);
        
        listFactoryRecords = findViewById(R.id.listFactoryRecords);
        btnAddAdvance = findViewById(R.id.btnAddAdvancePayment);
        btnViewPaymentHistory = findViewById(R.id.btnViewPaymentHistory);
        btnBack = findViewById(R.id.btnBack);
        txtTransactionCount = findViewById(R.id.txtTransactionCount);
        adapter = new FactoryTransactionAdapter(this, recordList);
        listFactoryRecords.setAdapter(adapter);

        factoryName = getIntent().getStringExtra("factoryName");

        if (factoryName != null) {
            txtFactoryName.setText(factoryName);

            String uid = getUid();
            databaseReference = FirebaseDatabase.getInstance().getReference("transport_data").child(uid);
            advanceRef = databaseReference.child("factory_payments").child(factoryName);

            loadAdvance();
        }

        btnBack.setOnClickListener(v -> finish());

        btnAddAdvance.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPaymentActivity.class);
            intent.putExtra("factoryName", factoryName);
            startActivity(intent);
        });

        btnViewPaymentHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentHistoryActivity.class);
            intent.putExtra("factoryName", factoryName);
            startActivity(intent);
        });

        // Factory Transactions वरील क्लिक ईव्हेंट (Click Event)
        listFactoryRecords.setOnItemClickListener((parent, view, position, id) -> {
            // १. क्लिक केलेल्या ओळीतील डेटा मिळवणे
            FactoryTransactionModel item = recordList.get(position);
            
            // २. Intent तयार करणे
            Intent intent = new Intent(this, PersonReportActivity.class);
            
            // ३. तिन्ही महत्त्वाचे डिटेल्स (Name + Vehicle + Date) पाठवणे
            intent.putExtra("personName", item.sellPerson);
            intent.putExtra("vehicle", item.vehicle);
            intent.putExtra("date", item.date);
            
            // ४. PersonReportActivity सुरू करणे
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (factoryName != null) {
            loadAdvance();
        }
    }

    private void loadAdvance() {
        advanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                totalAdvance = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String amtStr = ds.child("amount").getValue(String.class);
                    double amt = 0;
                    try { amt = Double.parseDouble(amtStr); } catch (Exception e) {}
                    totalAdvance += amt;
                }
                txtTotalAdvanceGiven.setText(String.format("₹%,.0f", totalAdvance));
                fetchFactoryData(factoryName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchFactoryData(String factoryName) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recordList.clear();
                totalUsedAmount = 0;
                totalWeightSum = 0;
                double runningAdvance = totalAdvance;

                for (DataSnapshot data : snapshot.getChildren()) {
                    if (data.getKey().equals("payments") || data.getKey().equals("factory_payments")) continue;
                    
                    String factory = data.child("factory").getValue(String.class);
                    if (factoryName.equals(factory)) {
                        String vehicle = data.child("vehicle").getValue(String.class);
                        String weight = data.child("weight").getValue(String.class);
                        String buyPriceStr = data.child("buyPrice").getValue(String.class);
                        String sellPerson = data.child("sellPerson").getValue(String.class);
                        String date = data.child("date").getValue(String.class);

                        double buyPrice = 0;
                        double w = 0;
                        try { buyPrice = Double.parseDouble(buyPriceStr); } catch (Exception e){}
                        try { w = Double.parseDouble(weight); } catch (Exception e){}

                        totalUsedAmount += buyPrice;
                        totalWeightSum += w;
                        runningAdvance -= buyPrice;

                        recordList.add(new FactoryTransactionModel(vehicle, weight, buyPrice, sellPerson, date, runningAdvance));
                    }
                }

                txtTotalUsed.setText(String.format("₹%,.0f", totalUsedAmount));
                txtTotalWeight.setText(String.format("%,.0f kg", totalWeightSum));
                
                double finalRemaining = totalAdvance - totalUsedAmount;
                txtAdvanceRemaining.setText(String.format("₹%,.0f", finalRemaining));
                
                adapter.notifyDataSetChanged();
// 🔥 Transaction Count update
                int count = recordList.size();
                txtTransactionCount.setText("Transactions (" + count + ")");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Model Class
    public static class FactoryTransactionModel {
        String vehicle, weight, sellPerson, date;
        double buyPrice, remainingAdvance;

        public FactoryTransactionModel(String vehicle, String weight, double buyPrice, String sellPerson, String date, double remainingAdvance) {
            this.vehicle = vehicle;
            this.weight = weight;
            this.buyPrice = buyPrice;
            this.sellPerson = sellPerson;
            this.date = date;
            this.remainingAdvance = remainingAdvance;
        }
    }

    // Custom Adapter
    public class FactoryTransactionAdapter extends ArrayAdapter<FactoryTransactionModel> {
        public FactoryTransactionAdapter(Context context, List<FactoryTransactionModel> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_factory_transaction, parent, false);
            }

            FactoryTransactionModel item = getItem(position);

            TextView v = convertView.findViewById(R.id.txtItemVehicle);
            TextView w = convertView.findViewById(R.id.txtItemWeight);
            TextView b = convertView.findViewById(R.id.txtItemBuyPrice);
            TextView s = convertView.findViewById(R.id.txtItemSellPerson);
            TextView d = convertView.findViewById(R.id.txtItemDate);
            TextView rem = convertView.findViewById(R.id.txtItemRemainingAdvance);
            LinearLayout layoutRem = convertView.findViewById(R.id.layoutRemainingAdvance);

            if (item != null) {
                v.setText(item.vehicle);
                w.setText(  item.weight + " kg");
                b.setText(String.format("₹%,.0f", item.buyPrice));
                s.setText( item.sellPerson);
                d.setText(item.date);
                rem.setText(String.format("%,.0f", item.remainingAdvance));

                if (item.remainingAdvance < 0) {
                    layoutRem.setBackgroundColor(Color.parseColor("#FEF2F2")); // Light Red
                    rem.setTextColor(Color.parseColor("#B91C1C")); // Red
                } else {
                    layoutRem.setBackgroundColor(Color.parseColor("#F0FDF4")); // Light Green
                    rem.setTextColor(Color.parseColor("#166534")); // Green
                }
            }

            return convertView;
        }
    }
}