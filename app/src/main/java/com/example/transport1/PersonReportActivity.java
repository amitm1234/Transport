package com.example.transport1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class PersonReportActivity extends AppCompatActivity {

    TextView txtTitle, txtTotalWeight, txtTotalSell, txtTotalPaid, txtRemaining, txtTransactionCount;
    ListView listTransactions;
    Button btnAddPayment, btnPaymentHistory;


    ArrayList<TransactionModel> transactionList = new ArrayList<>();
    TransactionAdapter adapter;

    DatabaseReference databaseReference, paymentReference;

    double totalWeight = 0;
    double totalSell = 0;
    double totalPaid = 0;

    String personName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_report);

        // Initialize views
        txtTitle = findViewById(R.id.txtTitle);
        txtTotalWeight = findViewById(R.id.txtTotalWeight);
        txtTotalSell = findViewById(R.id.txtTotalSell);
        txtTotalPaid = findViewById(R.id.txtTotalPaid);
        txtRemaining = findViewById(R.id.txtRemaining);
        listTransactions = findViewById(R.id.listTransactions);
        txtTransactionCount = findViewById(R.id.txtTransactionCount);

        btnAddPayment = findViewById(R.id.btnAddPayment);
        btnPaymentHistory = findViewById(R.id.btnPaymentHistory);

        // Using Custom Adapter
        adapter = new TransactionAdapter(this, transactionList);
        listTransactions.setAdapter(adapter);

        // Get intent data
        personName = getIntent().getStringExtra("personName");
        String vehicle = getIntent().getStringExtra("vehicle");
        String date = getIntent().getStringExtra("date");

        if (personName != null) {
            txtTitle.setText(personName);
            TextView subtitle = findViewById(R.id.txtPersonNameSubtitle);
            if (subtitle != null) subtitle.setText(personName);
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("transport_data")
                .child(uid);
        paymentReference = FirebaseDatabase.getInstance()
                .getReference("transport_data")
                .child(uid)
                .child("payments")
                .child(personName != null ? personName : "");

        // Button click listeners
        btnAddPayment.setOnClickListener(v -> {
            Intent intent = new Intent(PersonReportActivity.this, AddPaymentActivity.class);
            intent.putExtra("personName", personName);
            startActivity(intent);
        });

        btnPaymentHistory.setOnClickListener(v -> {
            Intent intent = new Intent(PersonReportActivity.this, PaymentHistoryActivity.class);
            intent.putExtra("personName", personName);
            startActivity(intent);
        });

        // Fetch data
        fetchTransactions(vehicle, date);
        fetchPayments();
    }

    private void fetchTransactions(String vehicle, String date) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                transactionList.clear();
                totalWeight = 0;
                totalSell = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    if (data.getKey().equals("payments") || data.getKey().equals("factory_payments")) continue;

                    String sellPerson = data.child("sellPerson").getValue(String.class);
                    String v = data.child("vehicle").getValue(String.class);
                    String d = data.child("date").getValue(String.class);
                    String weight = data.child("weight").getValue(String.class);
                    String sellPrice = data.child("sellPrice").getValue(String.class);
                    String sellGST = data.child("sellGST").getValue(String.class);
                    String sellGSTPercent = data.child("sellGSTPercent").getValue(String.class);
                    String total = data.child("sellTotalAmount").getValue(String.class);
                    String factory = data.child("factory").getValue(String.class);

                    boolean match = false;
                    if(vehicle != null && date != null){
                        if(personName != null && personName.equals(sellPerson) && vehicle.equals(v) && date.equals(d)){
                            match = true;
                        }
                    } else {
                        if(personName != null && personName.equals(sellPerson)){
                            match = true;
                        }
                    }

                    if(match){
                        double w = 0, t = 0;
                        try {
                            if (weight != null) w = Double.parseDouble(weight);
                            if (total != null) t = Double.parseDouble(total);
                        } catch(Exception e) {}

                        totalWeight += w;
                        totalSell += t;

                        transactionList.add(new TransactionModel(v, weight, sellPrice, sellGST, total, d, sellGSTPercent, factory));
                    }
                }

                adapter.notifyDataSetChanged();
                updateLedger();

                // 🔥 Transaction Count update
                int count = transactionList.size();
                txtTransactionCount.setText("Transactions (" + count + ")");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchPayments() {
        paymentReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                totalPaid = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    String amountStr = data.child("amount").getValue(String.class);
                    if(amountStr != null){
                        try {
                            totalPaid += Double.parseDouble(amountStr);
                        } catch(Exception e) {}
                    }
                }

                updateLedger();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    private void updateLedger() {
        txtTotalWeight.setText(String.format("%,.0f kg", totalWeight));
        txtTotalSell.setText(String.format("₹ %,.2f", totalSell));
        txtTotalPaid.setText(String.format("₹ %,.2f", totalPaid));
        txtRemaining.setText(String.format("₹ %,.2f", (totalSell - totalPaid)));
    }


    // Model Class updated to include Factory
    public static class TransactionModel {
        String vehicle, weight, price, gst, total, date, gstPercent, factory;
        public TransactionModel(String vehicle, String weight, String price, String gst, String total, String date, String gstPercent, String factory) {
            this.vehicle = vehicle;
            this.weight = weight;
            this.price = price;
            this.gst = gst;
            this.total = total;
            this.date = date;
            this.gstPercent = gstPercent;
            this.factory = factory;
        }
    }

    // Custom Adapter Class
    public class TransactionAdapter extends ArrayAdapter<TransactionModel> {
        public TransactionAdapter(Context context, List<TransactionModel> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_transaction, parent, false);
            }

            TransactionModel item = getItem(position);

            View factoryLayout = convertView.findViewById(R.id.layoutItemFactory);
            TextView f = convertView.findViewById(R.id.txtItemFactory);
            TextView v = convertView.findViewById(R.id.txtItemVehicle);
            TextView w = convertView.findViewById(R.id.txtItemWeight);
            TextView p = convertView.findViewById(R.id.txtItemPrice);
            TextView gLabel = convertView.findViewById(R.id.txtItemGSTLabel); // GST Label
            TextView gValue = convertView.findViewById(R.id.txtItemGST);     // GST Value
            TextView t = convertView.findViewById(R.id.txtItemTotal);
            TextView d = convertView.findViewById(R.id.txtItemDate);

            if (item != null) {
                f.setText(item.factory != null ? item.factory : "N/A");
                v.setText(item.vehicle);
                w.setText(item.weight + " kg");
                p.setText("₹ " + item.price);
                
                // GST टक्केवारीनुसार लेबल सेट करा (उदा. GST (5%))
                if (item.gstPercent != null && !item.gstPercent.isEmpty()) {
                    gLabel.setText("GST (" + item.gstPercent + "%)");
                } else {
                    gLabel.setText("GST");
                }
                
                // GST ची रक्कम सेट करा (उदा. ₹ 11.88)
                if (item.gst != null && !item.gst.isEmpty()) {
                    gValue.setText("₹ " + item.gst);
                } else {
                    gValue.setText("₹ 0");
                }
                
                t.setText("₹ " + item.total);
                d.setText(item.date);

                // Factory Box Click Listener
                factoryLayout.setOnClickListener(view -> {
                    Intent intent = new Intent(getContext(), FactoryReportActivity.class);
                    intent.putExtra("factoryName", item.factory);
                    intent.putExtra("personName", personName);
                    intent.putExtra("vehicle", item.vehicle);
                    intent.putExtra("date", item.date);
                    getContext().startActivity(intent);
                });
            }

            return convertView;
        }
    }
}