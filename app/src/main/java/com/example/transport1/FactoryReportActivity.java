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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FactoryReportActivity extends AppCompatActivity {

    TextView txtFactoryName,
            txtAdvanceRemaining,
            txtTotalAdvanceGiven,
            txtTotalUsed,
            txtTotalWeight,
            txtTransactionCount;

    ListView listFactoryRecords;

    Button btnAddAdvance,
            btnViewPaymentHistory;

    ImageView btnBack;

    ArrayList<FactoryTransactionModel> recordList =
            new ArrayList<>();

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


        // ------------------------------------------------
        // Initialize Views
        // ------------------------------------------------

        txtFactoryName =
                findViewById(R.id.txtFactoryName);

        txtAdvanceRemaining =
                findViewById(R.id.txtAdvanceRemaining);

        txtTotalAdvanceGiven =
                findViewById(R.id.txtTotalAdvanceGiven);

        txtTotalUsed =
                findViewById(R.id.txtTotalUsed);

        txtTotalWeight =
                findViewById(R.id.txtTotalWeight);

        txtTransactionCount =
                findViewById(R.id.txtTransactionCount);

        listFactoryRecords =
                findViewById(R.id.listFactoryRecords);

        btnAddAdvance =
                findViewById(R.id.btnAddAdvancePayment);

        btnViewPaymentHistory =
                findViewById(R.id.btnViewPaymentHistory);

        btnBack =
                findViewById(R.id.btnBack);


        // ------------------------------------------------
        // Adapter
        // ------------------------------------------------

        adapter =
                new FactoryTransactionAdapter(
                        this,
                        recordList
                );

        listFactoryRecords.setAdapter(adapter);


        // ------------------------------------------------
        // Get Factory Name
        // ------------------------------------------------

        factoryName =
                getIntent().getStringExtra("factoryName");


        if (factoryName != null &&
                !factoryName.trim().isEmpty()) {

            txtFactoryName.setText(factoryName);


            FirebaseAuth auth =
                    FirebaseAuth.getInstance();

            if (auth.getCurrentUser() != null) {

                String uid =
                        auth.getCurrentUser().getUid();


                databaseReference =
                        FirebaseDatabase
                                .getInstance()
                                .getReference("transport_data")
                                .child(uid);


                advanceRef =
                        databaseReference
                                .child("factory_payments")
                                .child(factoryName);


                loadAdvance();
            }
        }


        // ------------------------------------------------
        // Back Button
        // ------------------------------------------------

        btnBack.setOnClickListener(v ->
                finish()
        );


        // ------------------------------------------------
        // Add Advance
        // ------------------------------------------------

        btnAddAdvance.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            this,
                            AddPaymentActivity.class
                    );

            intent.putExtra(
                    "factoryName",
                    factoryName
            );

            startActivity(intent);
        });


        // ------------------------------------------------
        // Payment History
        // ------------------------------------------------

        btnViewPaymentHistory.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            this,
                            PaymentHistoryActivity.class
                    );

            intent.putExtra(
                    "factoryName",
                    factoryName
            );

            startActivity(intent);
        });


        // ------------------------------------------------
        // Transaction Click
        // ------------------------------------------------

        listFactoryRecords.setOnItemClickListener(
                (parent, view, position, id) -> {

                    FactoryTransactionModel item =
                            recordList.get(position);


                    Intent intent =
                            new Intent(
                                    this,
                                    PersonReportActivity.class
                            );


                    intent.putExtra(
                            "personName",
                            item.sellPerson
                    );

                    intent.putExtra(
                            "vehicle",
                            item.vehicle
                    );

                    intent.putExtra(
                            "date",
                            item.date
                    );


                    startActivity(intent);
                }
        );
    }


    // ====================================================
    // ON RESUME
    // ====================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (factoryName != null &&
                advanceRef != null) {

            loadAdvance();
        }
    }


    // ====================================================
    // LOAD ADVANCE
    // ====================================================

    private void loadAdvance() {

        advanceRef.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        totalAdvance = 0;


                        for (DataSnapshot ds :
                                snapshot.getChildren()) {

                            String amountString =
                                    ds.child("amount")
                                            .getValue(String.class);


                            double amount = 0;


                            try {

                                if (amountString != null) {

                                    amount =
                                            Double.parseDouble(
                                                    amountString
                                            );
                                }

                            } catch (Exception e) {

                                amount = 0;
                            }


                            totalAdvance += amount;
                        }


                        // Total Advance
                        txtTotalAdvanceGiven.setText(
                                String.format(
                                        "₹%,.0f",
                                        totalAdvance
                                )
                        );


                        // Load transactions
                        fetchFactoryData(factoryName);
                    }


                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {
                    }
                }
        );
    }


    // ====================================================
    // FETCH FACTORY DATA
    // ====================================================

    private void fetchFactoryData(
            String factoryName) {


        databaseReference.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {


                        recordList.clear();

                        totalUsedAmount = 0;

                        totalWeightSum = 0;


                        // IMPORTANT
                        // Remaining advance starts
                        // from total advance

                        double runningAdvance =
                                totalAdvance;


                        // --------------------------------------------
                        // Loop Transactions
                        // --------------------------------------------

                        for (DataSnapshot data :
                                snapshot.getChildren()) {


                            // Skip payment nodes
                            if ("payments".equals(data.getKey()) ||
                                    "factory_payments".equals(data.getKey())) {

                                continue;
                            }


                            String factory =
                                    data.child("factory")
                                            .getValue(String.class);


                            boolean validFactory =

                                    factoryName != null
                                            && !factoryName
                                            .trim()
                                            .isEmpty()

                                            && factory != null
                                            && !factory
                                            .trim()
                                            .isEmpty()

                                            && factoryName.equals(factory);


                            if (!validFactory) {
                                continue;
                            }


                            // --------------------------------------------
                            // Get Data
                            // --------------------------------------------

                            String vehicle =
                                    data.child("vehicle")
                                            .getValue(String.class);


                            String weight =
                                    data.child("weight")
                                            .getValue(String.class);


                            String buyPriceString =
                                    data.child("buyPrice")
                                            .getValue(String.class);


                            String buyGSTString =
                                    data.child("buyGST")
                                            .getValue(String.class);


                            String buyTotalString =
                                    data.child("buyTotal")
                                            .getValue(String.class);


                            String sellPerson =
                                    data.child("sellPerson")
                                            .getValue(String.class);


                            String date =
                                    data.child("date")
                                            .getValue(String.class);


                            // --------------------------------------------
                            // Convert Values
                            // --------------------------------------------

                            double buyPrice = 0;

                            double buyGST = 0;

                            double buyTotal = 0;

                            double weightValue = 0;


                            try {

                                if (buyPriceString != null) {

                                    buyPrice =
                                            Double.parseDouble(
                                                    buyPriceString
                                            );
                                }

                            } catch (Exception e) {

                                buyPrice = 0;
                            }


                            try {

                                if (buyGSTString != null) {

                                    buyGST =
                                            Double.parseDouble(
                                                    buyGSTString
                                            );
                                }

                            } catch (Exception e) {

                                buyGST = 0;
                            }


                            try {

                                if (buyTotalString != null) {

                                    buyTotal =
                                            Double.parseDouble(
                                                    buyTotalString
                                            );
                                }

                            } catch (Exception e) {

                                buyTotal = 0;
                            }


                            try {

                                if (weight != null) {

                                    weightValue =
                                            Double.parseDouble(
                                                    weight
                                            );
                                }

                            } catch (Exception e) {

                                weightValue = 0;
                            }


                            // --------------------------------------------
                            // SAFETY:
                            // जर Firebase मध्ये buyTotal save नसेल
                            // तर Buy Price + GST calculate करेल
                            // --------------------------------------------

                            if (buyTotal == 0) {

                                buyTotal =
                                        buyPrice + buyGST;
                            }


                            // --------------------------------------------
                            // Total Used
                            //
                            // येथे Total Amount वापरत आहोत
                            // म्हणजे Buy Price + GST
                            // --------------------------------------------

                            totalUsedAmount += buyTotal;


                            // --------------------------------------------
                            // Weight
                            // --------------------------------------------

                            totalWeightSum += weightValue;


                            // --------------------------------------------
                            // IMPORTANT
                            //
                            // Remaining Advance मधून
                            // BUY TOTAL कमी करा
                            //
                            // आधी:
                            // runningAdvance -= buyPrice;
                            //
                            // आता:
                            // runningAdvance -= buyTotal;
                            // --------------------------------------------

                            runningAdvance -= buyTotal;


                            // --------------------------------------------
                            // Add Record
                            // --------------------------------------------

                            recordList.add(
                                    new FactoryTransactionModel(

                                            vehicle,

                                            weight,

                                            buyPrice,

                                            buyGST,

                                            buyTotal,

                                            sellPerson,

                                            date,

                                            runningAdvance
                                    )
                            );
                        }


                        // ====================================================
                        // HEADER TOTALS
                        // ====================================================


                        // Total Used = Buy Price + GST
                        txtTotalUsed.setText(
                                String.format(
                                        "₹%,.0f",
                                        totalUsedAmount
                                )
                        );


                        txtTotalWeight.setText(
                                String.format(
                                        "%,.0f ton",
                                        totalWeightSum
                                )
                        );


                        // --------------------------------------------
                        // Final Remaining
                        //
                        // Total Advance - Total Amount
                        // --------------------------------------------

                        double finalRemaining =
                                totalAdvance - totalUsedAmount;


                        txtAdvanceRemaining.setText(
                                String.format(
                                        "₹%,.0f",
                                        finalRemaining
                                )
                        );


                        // --------------------------------------------
                        // Transaction Count
                        // --------------------------------------------

                        int count =
                                recordList.size();


                        txtTransactionCount.setText(
                                "Transactions (" +
                                        count +
                                        ")"
                        );


                        // Refresh List
                        adapter.notifyDataSetChanged();
                    }


                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {
                    }
                }
        );
    }


    // ====================================================
    // MODEL CLASS
    // ====================================================

    public static class FactoryTransactionModel {

        String vehicle;
        String weight;
        String sellPerson;
        String date;

        double buyPrice;
        double buyGST;
        double buyTotal;
        double remainingAdvance;


        public FactoryTransactionModel(

                String vehicle,

                String weight,

                double buyPrice,

                double buyGST,

                double buyTotal,

                String sellPerson,

                String date,

                double remainingAdvance
        ) {

            this.vehicle =
                    vehicle;

            this.weight =
                    weight;

            this.buyPrice =
                    buyPrice;

            this.buyGST =
                    buyGST;

            this.buyTotal =
                    buyTotal;

            this.sellPerson =
                    sellPerson;

            this.date =
                    date;

            this.remainingAdvance =
                    remainingAdvance;
        }
    }


    // ====================================================
    // CUSTOM ADAPTER
    // ====================================================

    public class FactoryTransactionAdapter
            extends ArrayAdapter<FactoryTransactionModel> {


        public FactoryTransactionAdapter(
                Context context,
                List<FactoryTransactionModel> objects) {

            super(
                    context,
                    0,
                    objects
            );
        }


        @NonNull
        @Override
        public View getView(

                int position,

                @Nullable View convertView,

                @NonNull ViewGroup parent) {


            if (convertView == null) {

                convertView =
                        LayoutInflater
                                .from(getContext())
                                .inflate(
                                        R.layout.item_factory_transaction,
                                        parent,
                                        false
                                );
            }


            FactoryTransactionModel item =
                    getItem(position);


            // --------------------------------------------
            // Find Views
            // --------------------------------------------

            TextView v =
                    convertView.findViewById(
                            R.id.txtItemVehicle
                    );


            TextView w =
                    convertView.findViewById(
                            R.id.txtItemWeight
                    );


            TextView b =
                    convertView.findViewById(
                            R.id.txtItemBuyPrice
                    );


            TextView gst =
                    convertView.findViewById(
                            R.id.txtItemGST
                    );


            TextView total =
                    convertView.findViewById(
                            R.id.txtItemTotalAmount
                    );


            TextView s =
                    convertView.findViewById(
                            R.id.txtItemSellPerson
                    );


            TextView d =
                    convertView.findViewById(
                            R.id.txtItemDate
                    );


            TextView rem =
                    convertView.findViewById(
                            R.id.txtItemRemainingAdvance
                    );


            LinearLayout layoutRem =
                    convertView.findViewById(
                            R.id.layoutRemainingAdvance
                    );


            // --------------------------------------------
            // Set Data
            // --------------------------------------------

            if (item != null) {


                v.setText(
                        item.vehicle
                );


                w.setText(
                        item.weight + " ton"
                );


                // Buy Price
                b.setText(
                        String.format(
                                "₹%,.0f",
                                item.buyPrice
                        )
                );


                // GST
                gst.setText(
                        String.format(
                                "₹%,.0f",
                                item.buyGST
                        )
                );


                // Total Amount
                total.setText(
                        String.format(
                                "₹%,.0f",
                                item.buyTotal
                        )
                );


                s.setText(
                        item.sellPerson
                );


                d.setText(
                        item.date
                );


                // Remaining
                rem.setText(
                        String.format(
                                "₹%,.0f",
                                item.remainingAdvance
                        )
                );


                // --------------------------------------------
                // Remaining Color
                // --------------------------------------------

                if (item.remainingAdvance < 0) {

                    layoutRem.setBackgroundColor(
                            Color.parseColor(
                                    "#FEF2F2"
                            )
                    );


                    rem.setTextColor(
                            Color.parseColor(
                                    "#B91C1C"
                            )
                    );

                } else {

                    layoutRem.setBackgroundColor(
                            Color.parseColor(
                                    "#F0FDF4"
                            )
                    );


                    rem.setTextColor(
                            Color.parseColor(
                                    "#166534"
                            )
                    );
                }
            }


            return convertView;
        }
    }
}







