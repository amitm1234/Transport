package com.example.transport1;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class PaymentHistoryActivity extends BaseActivity {

    ListView listPayments;
    ArrayList<String> paymentList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    String personName;
    String factoryName;

    DatabaseReference paymentRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        listPayments = findViewById(R.id.listPayments);

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                paymentList);
        listPayments.setAdapter(adapter);

        // Check if Person or Factory
        personName = getIntent().getStringExtra("personName");
        factoryName = getIntent().getStringExtra("factoryName");

        String uid = getUid();

        if (personName != null) {
            // Person payments
            paymentRef = FirebaseDatabase.getInstance()
                    .getReference("transport_data")
                    .child(uid)
                    .child("payments")
                    .child(personName);
        } else if (factoryName != null) {
            // Factory payments
            paymentRef = FirebaseDatabase.getInstance()
                    .getReference("transport_data")
                    .child(uid)
                    .child("factory_payments")
                    .child(factoryName);
        }

        fetchPayments();
    }

    private void fetchPayments() {
        if (paymentRef == null) return;

        paymentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                paymentList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    String amount = data.child("amount").getValue(String.class);
                    String date = data.child("date").getValue(String.class);

                    paymentList.add("Amount: ₹" + amount + "\nDate: " + date + "\n-----------------------------");
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}