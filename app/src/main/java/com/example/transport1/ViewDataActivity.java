package com.example.transport1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ViewDataActivity extends BaseActivity {

    private LinearLayout layoutData;
    private DatabaseReference databaseReference;
    private Button btnDownload, btnOpenCalculation;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data);

        layoutData = findViewById(R.id.layoutData);
        btnDownload = findViewById(R.id.btnDownload);
        btnOpenCalculation = findViewById(R.id.btnOpenCalculation);

        String uid = getUid();

        if (uid == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseReference = FirebaseDatabase.getInstance()
                .getReference("transport_data")
                .child(uid);

        fetchAndDisplayData();

        btnDownload.setOnClickListener(v -> {
            // PDF generate functionality will be handled by separate PdfGenerator class
            PdfGenerator.generatePdf(ViewDataActivity.this, PdfDataHolder.getDataList());
        });

        btnOpenCalculation.setOnClickListener(v -> {
            Intent intent = new Intent(ViewDataActivity.this, CalculationActivity.class);
            startActivity(intent);
        });
    }

    private void fetchAndDisplayData() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                layoutData.removeAllViews();
                PdfDataHolder.clearDataList(); // Clear old data

                LayoutInflater inflater = LayoutInflater.from(ViewDataActivity.this);

                for (DataSnapshot ds : snapshot.getChildren()) {

                    TransportData data = ds.getValue(TransportData.class);
                    String key = ds.getKey();

                    if (data != null) {
                        addCard(data, key, inflater);
                        PdfDataHolder.addData(data); // Add data for PDF generation
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ViewDataActivity.this,
                        error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addCard(TransportData data, String key, LayoutInflater inflater) {

        View card = inflater.inflate(R.layout.card_transport_data, layoutData, false);

        TextView tvVehicle = card.findViewById(R.id.tvVehicle);
        TextView tvFactory = card.findViewById(R.id.tvFactory);
        TextView tvDate = card.findViewById(R.id.tvDate);
        TextView tvWeight = card.findViewById(R.id.tvWeight);

        TextView tvBuyWeight = card.findViewById(R.id.tvBuyWeight);
        TextView tvBuyPrice = card.findViewById(R.id.tvBuyPrice);
        TextView tvBuyGST = card.findViewById(R.id.tvBuyGST);
        TextView tvBuyTotal = card.findViewById(R.id.tvBuyTotal);

        TextView tvSellPerson = card.findViewById(R.id.tvSellPerson);
        TextView tvSellWeight = card.findViewById(R.id.tvSellWeight);
        TextView tvSellPrice = card.findViewById(R.id.tvSellPrice);
        TextView tvSellGST = card.findViewById(R.id.tvSellGST);
        TextView tvSellTotal = card.findViewById(R.id.tvSellTotal);

        ImageButton btnMenu = card.findViewById(R.id.btnMenu);

        tvVehicle.setText("Vehicle: " + data.vehicle);
        tvFactory.setText("Factory: " + data.factory);
        tvDate.setText("Date: " + data.date);
        tvWeight.setText("Weight: " + data.weight + " " + data.measurement);

        tvBuyWeight.setText("Buy Weight: " + data.buyWeight);
        tvBuyPrice.setText("Buy Price: " + data.buyPrice);
        tvBuyGST.setText("Buy GST: " + data.buyGST);
        tvBuyTotal.setText("Buy Total: " + data.buyTotalAmount);

        tvSellPerson.setText("Sell Person: " + data.sellPerson);
        tvSellWeight.setText("Sell Weight: " + data.sellWeight);
        tvSellPrice.setText("Sell Price: " + data.sellPrice);
        tvSellGST.setText("Sell GST: " + data.sellGST);
        tvSellTotal.setText("Sell Total: " + data.sellTotalAmount);

        btnMenu.setOnClickListener(v -> {

            PopupMenu popupMenu = new PopupMenu(ViewDataActivity.this, btnMenu);
            popupMenu.getMenu().add("Edit");
            popupMenu.getMenu().add("Delete");

            popupMenu.setOnMenuItemClickListener(item -> {

                if (item.getTitle().equals("Edit")) {
                    Intent intent = new Intent(ViewDataActivity.this, EditDataActivity.class);

                    intent.putExtra("id", key);
                    intent.putExtra("vehicle", data.vehicle);
                    intent.putExtra("factory", data.factory);
                    intent.putExtra("date", data.date);
                    intent.putExtra("weight", data.weight);
                    intent.putExtra("measurement", data.measurement);
                    intent.putExtra("buyWeight", data.buyWeight);
                    intent.putExtra("buyPrice", data.buyPrice);
                    intent.putExtra("buyGST", data.buyGST);
                    intent.putExtra("sellPerson", data.sellPerson);
                    intent.putExtra("sellWeight", data.sellWeight);
                    intent.putExtra("sellPrice", data.sellPrice);
                    intent.putExtra("sellGST", data.sellGST);

                    startActivity(intent);
                }

                if (item.getTitle().equals("Delete")) {
                    databaseReference.child(key).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(ViewDataActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                                fetchAndDisplayData();
                            });
                }

                return true;
            });

            popupMenu.show();
        });

        layoutData.addView(card);
    }
}