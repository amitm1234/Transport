package com.example.transport1;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CashRecordsActivity extends AppCompatActivity {

    ImageButton btnBack;
    TextView txtTotalCashGiven, txtEmptyState;
    Spinner spinnerPersonFilter;
    MaterialButton btnGenerateCashPdf;
    ListView listCashRecords;

    DatabaseReference cashRef;

    List<CashRecord> allCashRecords = new ArrayList<>();
    List<CashRecord> filteredCashRecords = new ArrayList<>();
    List<String> personFilterList = new ArrayList<>();

    List<DisplayItem> displayItems = new ArrayList<>();
    CashRecordAdapter adapter;

    String selectedPersonFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cash_records);

        btnBack = findViewById(R.id.btnBack);
        txtTotalCashGiven = findViewById(R.id.txtTotalCashGiven);
        txtEmptyState = findViewById(R.id.txtEmptyState);
        spinnerPersonFilter = findViewById(R.id.spinnerPersonFilter);
        btnGenerateCashPdf = findViewById(R.id.btnGenerateCashPdf);
        listCashRecords = findViewById(R.id.listCashRecords);

        adapter = new CashRecordAdapter(this, displayItems);
        listCashRecords.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String uid = user.getUid();
        cashRef = FirebaseDatabase.getInstance()
                .getReference("transport_data")
                .child(uid)
                .child("cash_records");

        btnGenerateCashPdf.setOnClickListener(v -> {
            if (filteredCashRecords == null || filteredCashRecords.isEmpty()) {
                Toast.makeText(CashRecordsActivity.this, "No transaction data", Toast.LENGTH_SHORT).show();
                return;
            }
            PdfGenerator.generateCashPdf(CashRecordsActivity.this, filteredCashRecords, selectedPersonFilter);
        });

        spinnerPersonFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < personFilterList.size()) {
                    selectedPersonFilter = personFilterList.get(position);
                    applyFilterAndRender();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        fetchCashRecords();
    }

    private void fetchCashRecords() {
        cashRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allCashRecords.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    CashRecord record = ds.getValue(CashRecord.class);
                    if (record != null) {
                        record.id = ds.getKey();
                        allCashRecords.add(record);
                    }
                }

                // Update Person Filter Spinner items
                personFilterList.clear();
                personFilterList.add("All");

                for (CashRecord record : allCashRecords) {
                    if (record != null && record.personName != null && !record.personName.trim().isEmpty()) {
                        String name = record.personName.trim();
                        if (!personFilterList.contains(name)) {
                            personFilterList.add(name);
                        }
                    }
                }

                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                        CashRecordsActivity.this,
                        android.R.layout.simple_spinner_item,
                        personFilterList
                );
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerPersonFilter.setAdapter(spinnerAdapter);

                // Preserve selection if possible
                int selectionIndex = personFilterList.indexOf(selectedPersonFilter);
                if (selectionIndex >= 0) {
                    spinnerPersonFilter.setSelection(selectionIndex);
                } else {
                    spinnerPersonFilter.setSelection(0);
                    selectedPersonFilter = "All";
                }

                applyFilterAndRender();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CashRecordsActivity.this, "Error loading cash records: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilterAndRender() {
        filteredCashRecords.clear();
        displayItems.clear();

        double totalCash = 0;

        for (CashRecord record : allCashRecords) {
            if (record == null) continue;

            boolean match = false;
            if ("All".equalsIgnoreCase(selectedPersonFilter) || selectedPersonFilter == null || selectedPersonFilter.trim().isEmpty()) {
                match = true;
            } else if (selectedPersonFilter.trim().equalsIgnoreCase(record.personName != null ? record.personName.trim() : "")) {
                match = true;
            }

            if (match) {
                filteredCashRecords.add(record);
                totalCash += parseDoubleSafely(record.amount);
            }
        }

        txtTotalCashGiven.setText(String.format("₹ %,.2f", totalCash));

        if (filteredCashRecords.isEmpty()) {
            txtEmptyState.setVisibility(View.VISIBLE);
            listCashRecords.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
            return;
        }

        txtEmptyState.setVisibility(View.GONE);
        listCashRecords.setVisibility(View.VISIBLE);

        // Group filtered records by personName
        Map<String, List<CashRecord>> groupedMap = new LinkedHashMap<>();
        for (CashRecord r : filteredCashRecords) {
            String pName = (r.personName != null && !r.personName.trim().isEmpty()) ? r.personName.trim() : "Other";
            List<CashRecord> list = groupedMap.get(pName);
            if (list == null) {
                list = new ArrayList<>();
                groupedMap.put(pName, list);
            }
            list.add(r);
        }

        for (Map.Entry<String, List<CashRecord>> entry : groupedMap.entrySet()) {
            String personName = entry.getKey();
            List<CashRecord> records = entry.getValue();

            Collections.sort(records, (r1, r2) -> PersonReportActivity.compareDates(r1.date, r2.date));

            double personTotal = 0;
            for (CashRecord r : records) {
                personTotal += parseDoubleSafely(r.amount);
            }

            displayItems.add(new DisplayItem(personName, personTotal));
            for (CashRecord r : records) {
                displayItems.add(new DisplayItem(r));
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void confirmDeleteRecord(CashRecord record) {
        if (record == null || record.id == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete Cash Record")
                .setMessage("Are you sure you want to delete this cash record?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    cashRef.child(record.id).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(CashRecordsActivity.this, "Cash record deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(CashRecordsActivity.this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
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

    // Helper item for grouped list view
    static class DisplayItem {
        boolean isHeader;
        String personName;
        double personTotal;
        CashRecord record;

        DisplayItem(String personName, double personTotal) {
            this.isHeader = true;
            this.personName = personName;
            this.personTotal = personTotal;
        }

        DisplayItem(CashRecord record) {
            this.isHeader = false;
            this.record = record;
        }
    }

    // Multi-type Adapter
    class CashRecordAdapter extends BaseAdapter {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_RECORD = 1;

        private final Context context;
        private final List<DisplayItem> items;

        CashRecordAdapter(Context context, List<DisplayItem> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).isHeader ? TYPE_HEADER : TYPE_RECORD;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DisplayItem item = items.get(position);
            int type = getItemViewType(position);

            if (type == TYPE_HEADER) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.item_cash_person_header, parent, false);
                }
                TextView txtHeaderPersonName = convertView.findViewById(R.id.txtHeaderPersonName);
                TextView txtHeaderPersonTotal = convertView.findViewById(R.id.txtHeaderPersonTotal);

                txtHeaderPersonName.setText(item.personName);
                txtHeaderPersonTotal.setText(String.format("₹ %,.2f", item.personTotal));

            } else {
                if (convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.item_cash_record, parent, false);
                }

                CashRecord record = item.record;

                TextView txtCashRefNo = convertView.findViewById(R.id.txtCashRefNo);
                TextView txtCashDate = convertView.findViewById(R.id.txtCashDate);
                TextView txtCashVehicle = convertView.findViewById(R.id.txtCashVehicle);
                TextView txtCashFactory = convertView.findViewById(R.id.txtCashFactory);
                TextView txtCashWeight = convertView.findViewById(R.id.txtCashWeight);
                TextView txtCashAmount = convertView.findViewById(R.id.txtCashAmount);

                if (record != null) {
                    txtCashRefNo.setText("Ref #" + (record.refNo != null ? record.refNo : "-"));
                    txtCashDate.setText(record.date != null ? record.date : "-");
                    txtCashVehicle.setText(record.vehicle != null ? record.vehicle : "-");
                    txtCashFactory.setText(record.factory != null ? record.factory : "-");

                    String w = (record.weight != null && !record.weight.trim().isEmpty()) ? record.weight.trim() : "-";
                    String m = (record.measurement != null && !record.measurement.trim().isEmpty()) ? record.measurement.trim() : "";
                    txtCashWeight.setText(m.isEmpty() ? w : w + " " + m);

                    double amt = parseDoubleSafely(record.amount);
                    txtCashAmount.setText(String.format("₹ %,.2f", amt));

                    convertView.setOnLongClickListener(v -> {
                        confirmDeleteRecord(record);
                        return true;
                    });
                }
            }

            return convertView;
        }
    }
}
