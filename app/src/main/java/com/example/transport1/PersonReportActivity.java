package com.example.transport1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PersonReportActivity extends AppCompatActivity {

    TextView txtTitle, txtTotalWeight, txtTotalSell, txtTotalPaid, txtRemaining, txtTransactionCount;
    ListView listTransactions;
    Button btnAddPayment, btnPaymentHistory, btnGeneratePdf;
    ImageView btnGeneratePdfHeader;

    ArrayList<TransactionModel> transactionList = new ArrayList<>();
    List<TransportData> personTransportList = new ArrayList<>();
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
        btnGeneratePdf = findViewById(R.id.btnGeneratePdf);
        btnGeneratePdfHeader = findViewById(R.id.btnGeneratePdfHeader);

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

        // Generate PDF Click Listener
        View.OnClickListener pdfListener = v -> {
            if (personTransportList == null || personTransportList.isEmpty()) {
                Toast.makeText(PersonReportActivity.this, "No transaction data", Toast.LENGTH_SHORT).show();
                return;
            }

            double pSum = 0, gSum = 0, aSum = 0;
            for (TransportData t : personTransportList) {
                if (t == null) continue;
                pSum += parseDoubleSafely(t.sellPrice);
                gSum += parseDoubleSafely(t.sellGST);
                aSum += parseDoubleSafely(t.sellTotalAmount);
            }

            PdfGenerator.generateSellPersonPdf(
                    PersonReportActivity.this,
                    personName,
                    personTransportList,
                    pSum,
                    gSum,
                    aSum
            );
        };

        if (btnGeneratePdf != null) {
            btnGeneratePdf.setOnClickListener(pdfListener);
        }
        if (btnGeneratePdfHeader != null) {
            btnGeneratePdfHeader.setOnClickListener(pdfListener);
        }

        // Fetch data
        fetchTransactions(vehicle, date);
        fetchPayments();
    }

    private void fetchTransactions(String vehicle, String date) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                transactionList.clear();
                personTransportList.clear();
                totalWeight = 0;
                totalSell = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    String key = data.getKey();
                    if (key != null && (key.equals("payments") || key.equals("factory_payments") || key.equals("cash_records"))) continue;

                    String sellPerson = data.child("sellPerson").getValue(String.class);
                    String v = data.child("vehicle").getValue(String.class);
                    String d = data.child("date").getValue(String.class);
                    String weight = data.child("weight").getValue(String.class);
                    String measurement = data.child("measurement").getValue(String.class);
                    String sellPrice = data.child("sellPrice").getValue(String.class);
                    String sellGST = data.child("sellGST").getValue(String.class);
                    String sellGSTPercent = data.child("sellGSTPercent").getValue(String.class);
                    String total = data.child("sellTotalAmount").getValue(String.class);
                    String factory = data.child("factory").getValue(String.class);

                    boolean match = false;
                    boolean validPerson = personName != null && !personName.trim().isEmpty()
                            && sellPerson != null && !sellPerson.trim().isEmpty();

                    if(validPerson){
                        if(vehicle != null && date != null){
                            if(personName.equals(sellPerson) && vehicle.equals(v) && date.equals(d)){
                                match = true;
                            }
                        } else {
                            if(personName.equals(sellPerson)){
                                match = true;
                            }
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

                        transactionList.add(new TransactionModel(key, v, weight, sellPrice, sellGST, total, d, sellGSTPercent, factory, measurement));

                        TransportData tData = data.getValue(TransportData.class);
                        if (tData != null) {
                            personTransportList.add(tData);
                        }
                    }
                }

                // Sort transactions chronologically by date (oldest to newest)
                Collections.sort(transactionList, (t1, t2) -> compareDates(t1.date, t2.date));
                Collections.sort(personTransportList, (p1, p2) -> compareDates(p1.date, p2.date));

                adapter.notifyDataSetChanged();
                updateLedger();

                // Transaction Count update
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

    private double parseDoubleSafely(String val) {
        if (val == null) return 0.0;
        try {
            String clean = val.replaceAll("[^0-9.-]", "");
            if (clean.isEmpty() || clean.equals("-") || clean.equals(".")) return 0.0;
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static int compareDates(String d1, String d2) {
        if (d1 == null && d2 == null) return 0;
        if (d1 == null) return -1;
        if (d2 == null) return 1;
        SimpleDateFormat sdf1 = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        SimpleDateFormat sdf2 = new SimpleDateFormat("d/M/yyyy", Locale.US);
        Date date1 = parseDate(d1, sdf1, sdf2);
        Date date2 = parseDate(d2, sdf1, sdf2);
        if (date1 == null && date2 == null) return d1.compareTo(d2);
        if (date1 == null) return -1;
        if (date2 == null) return 1;
        return date1.compareTo(date2);
    }

    private static Date parseDate(String str, SimpleDateFormat... sdfs) {
        if (str == null || str.trim().isEmpty()) return null;
        for (SimpleDateFormat sdf : sdfs) {
            try {
                return sdf.parse(str.trim());
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void copyToCashList(TransactionModel item, int position) {
        if (item == null || item.recordKey == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference cashRef = FirebaseDatabase.getInstance()
                .getReference("transport_data")
                .child(uid)
                .child("cash_records");

        cashRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean alreadyExists = false;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String srcId = ds.child("sourceRecordId").getValue(String.class);
                    if (item.recordKey.equals(srcId)) {
                        alreadyExists = true;
                        break;
                    }
                }

                if (alreadyExists) {
                    Toast.makeText(PersonReportActivity.this, "Already in Cash List", Toast.LENGTH_SHORT).show();
                } else {
                    String newCashKey = cashRef.push().getKey();
                    if (newCashKey != null) {
                        String refNo = String.valueOf(position + 1);

                        CashRecord cashRecord = new CashRecord(
                                newCashKey,
                                item.recordKey,
                                personName,
                                refNo,
                                item.date,
                                item.vehicle,
                                item.factory,
                                item.measurement != null ? item.measurement : "",
                                item.weight,
                                item.total
                        );

                        cashRef.child(newCashKey).setValue(cashRecord)
                                .addOnSuccessListener(aVoid -> Toast.makeText(PersonReportActivity.this, "Copied to Cash List", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(PersonReportActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PersonReportActivity.this, "Error checking cash list: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Model Class updated to include recordKey and measurement
    public static class TransactionModel {
        String recordKey, vehicle, weight, price, gst, total, date, gstPercent, factory, measurement;
        public TransactionModel(String recordKey, String vehicle, String weight, String price, String gst, String total, String date, String gstPercent, String factory, String measurement) {
            this.recordKey = recordKey;
            this.vehicle = vehicle;
            this.weight = weight;
            this.price = price;
            this.gst = gst;
            this.total = total;
            this.date = date;
            this.gstPercent = gstPercent;
            this.factory = factory;
            this.measurement = measurement;
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
            TextView gLabel = convertView.findViewById(R.id.txtItemGSTLabel);
            TextView gValue = convertView.findViewById(R.id.txtItemGST);
            TextView t = convertView.findViewById(R.id.txtItemTotal);
            TextView d = convertView.findViewById(R.id.txtItemDate);

            if (item != null) {
                f.setText(item.factory != null ? item.factory : "N/A");
                v.setText(item.vehicle);
                w.setText(item.weight + " kg");
                p.setText("₹ " + item.price);

                if (item.gstPercent != null && !item.gstPercent.isEmpty()) {
                    gLabel.setText("GST (" + item.gstPercent + "%)");
                } else {
                    gLabel.setText("GST");
                }

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

                // Long press listener on transaction card for "Copy to Cash List"
                convertView.setOnLongClickListener(view -> {
                    PopupMenu popup = new PopupMenu(getContext(), view);
                    popup.getMenu().add("Copy to Cash List");
                    popup.setOnMenuItemClickListener(menuItem -> {
                        if (menuItem.getTitle() != null && "Copy to Cash List".equals(menuItem.getTitle().toString())) {
                            copyToCashList(item, position);
                            return true;
                        }
                        return false;
                    });
                    popup.show();
                    return true;
                });
            }

            return convertView;
        }
    }
}
