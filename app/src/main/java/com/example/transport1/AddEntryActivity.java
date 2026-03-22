package com.example.transport1;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Calendar;

public class AddEntryActivity extends AppCompatActivity {

    AutoCompleteTextView etVehicleNo, etFactory, etSellPerson;
    EditText etDate, etWeight;
    Spinner spMeasurement;

    EditText etBuyPrice, etBuyGSTPercent, etBuyGST, etBuyTotal;
    EditText etSellWeight, etSellPrice, etSellGSTPercent, etSellGST, etSellTotal;

    Button btnSubmit;
    MaterialCardView cardGeneral, cardBuy, cardSell;
    Spinner spinnerEntryType;

    DatabaseReference databaseReference;
    SharedPreferences prefs;

    ArrayList<String> vehicleList = new ArrayList<>();
    ArrayList<String> factoryList = new ArrayList<>();
    ArrayList<String> personList = new ArrayList<>();

    private int currentEntryType = ENTRY_TYPE_BOTH;
    private static final int ENTRY_TYPE_BUY = 0;
    private static final int ENTRY_TYPE_SELL = 1;
    private static final int ENTRY_TYPE_BOTH = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_entry);

        // --- Init Views ---
        etVehicleNo = findViewById(R.id.etVehicleNo);
        etDate = findViewById(R.id.etDate);
        etFactory = findViewById(R.id.etFactory);
        etWeight = findViewById(R.id.etWeight);
        spMeasurement = findViewById(R.id.spMeasurement);

        btnSubmit = findViewById(R.id.btnSubmit);
        spinnerEntryType = findViewById(R.id.spinnerEntryType);

        cardGeneral = findViewById(R.id.cardGeneral);
        cardBuy = findViewById(R.id.cardBuy);
        cardSell = findViewById(R.id.cardSell);

        etBuyPrice = findViewById(R.id.etBuyPrice);
        etBuyGSTPercent = findViewById(R.id.etBuyGSTPercent);
        etBuyGST = findViewById(R.id.etBuyGST);
        etBuyTotal = findViewById(R.id.etBuyTotal);

        etSellPerson = findViewById(R.id.etSellPerson);
        etSellWeight = findViewById(R.id.etSellWeight);
        etSellPrice = findViewById(R.id.etSellPrice);
        etSellGSTPercent = findViewById(R.id.etSellGSTPercent);
        etSellGST = findViewById(R.id.etSellGST);
        etSellTotal = findViewById(R.id.etSellTotal);

        // --- Firebase User ---
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            databaseReference = FirebaseDatabase
                    .getInstance()
                    .getReference("transport_data")
                    .child(user.getUid());

            loadSuggestions();
        }

        prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);

        // --- Default values from settings ---
        String defaultGST = prefs.getString("default_gst", "0");
        String defaultBuyPrice = prefs.getString("default_buy_price", "");
        String defaultSellPrice = prefs.getString("default_sell_price", "");
        String defaultUnit = prefs.getString("default_unit", "Select Unit");

        etBuyGSTPercent.setText(defaultGST);
        etSellGSTPercent.setText(defaultGST);

        if(!defaultBuyPrice.isEmpty()) {
            etBuyPrice.setText(defaultBuyPrice);
            calculateBuyGST();
        }
        if(!defaultSellPrice.isEmpty()) {
            etSellPrice.setText(defaultSellPrice);
            calculateSellGST();
        }

        String[] units = {"Select Unit", "Ton", "Quintal"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, units);
        spMeasurement.setAdapter(adapter);

        // Set default unit
        for (int i = 0; i < units.length; i++) {
            if (units[i].equals(defaultUnit)) {
                spMeasurement.setSelection(i);
                break;
            }
        }

        // --- Default today's date ---
        Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH);
        int year = c.get(Calendar.YEAR);
        etDate.setText(String.format("%02d/%02d/%d", day, month + 1, year));

        // --- Date picker ---
        etDate.setOnClickListener(v -> {
            new DatePickerDialog(
                    this,
                    (view, y, m, d) -> etDate.setText(String.format("%02d/%02d/%d", d, m + 1, y)),
                    year,
                    month,
                    day
            ).show();
        });

        // --- Weight sync ---
        etWeight.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){}
            public void afterTextChanged(Editable s){
                if(etSellWeight.getText().toString().isEmpty()){
                    etSellWeight.setText(s.toString());
                }
            }
        });

        // --- GST Calculations ---
        TextWatcher buyWatcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){}
            public void afterTextChanged(Editable s){ calculateBuyGST(); }
        };
        etBuyPrice.addTextChangedListener(buyWatcher);
        etBuyGSTPercent.addTextChangedListener(buyWatcher);

        TextWatcher sellWatcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){}
            public void afterTextChanged(Editable s){ calculateSellGST(); }
        };
        etSellPrice.addTextChangedListener(sellWatcher);
        etSellGSTPercent.addTextChangedListener(sellWatcher);

        // --- Submit ---
        btnSubmit.setOnClickListener(v -> submitData());

        // --- Entry Type Spinner ---
        String[] entryTypes = {"General + Buy", "General + Sell", "Both"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, entryTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEntryType.setAdapter(spinnerAdapter);
        spinnerEntryType.setSelection(ENTRY_TYPE_BOTH);

        spinnerEntryType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentEntryType = position;
                updateCardVisibility(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateCardVisibility(int entryType){
        cardGeneral.setVisibility(View.VISIBLE);
        cardBuy.setVisibility(entryType == ENTRY_TYPE_BUY || entryType == ENTRY_TYPE_BOTH ? View.VISIBLE : View.GONE);
        cardSell.setVisibility(entryType == ENTRY_TYPE_SELL || entryType == ENTRY_TYPE_BOTH ? View.VISIBLE : View.GONE);
    }

    private void calculateBuyGST(){
        String priceStr = etBuyPrice.getText().toString().trim();
        String gstStr = etBuyGSTPercent.getText().toString().trim();
        if(!priceStr.isEmpty() && !gstStr.isEmpty()){
            double price = Double.parseDouble(priceStr);
            double gstPercent = Double.parseDouble(gstStr);
            double gst = price * gstPercent / 100;
            etBuyGST.setText(String.format("%.2f", gst));
            etBuyTotal.setText(String.format("%.2f", price + gst));
        } else { etBuyGST.setText(""); etBuyTotal.setText(""); }
    }

    private void calculateSellGST(){
        String priceStr = etSellPrice.getText().toString().trim();
        String gstStr = etSellGSTPercent.getText().toString().trim();
        if(!priceStr.isEmpty() && !gstStr.isEmpty()){
            double price = Double.parseDouble(priceStr);
            double gstPercent = Double.parseDouble(gstStr);
            double gst = price * gstPercent / 100;
            etSellGST.setText(String.format("%.2f", gst));
            etSellTotal.setText(String.format("%.2f", price + gst));
        } else { etSellGST.setText(""); etSellTotal.setText(""); }
    }

    private void submitData(){
        String vehicle = etVehicleNo.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String factory = etFactory.getText().toString().trim();
        String measurement = spMeasurement.getSelectedItem().toString();
        String weight = etWeight.getText().toString().trim();

        String buyPrice = etBuyPrice.getText().toString().trim();
        String buyGST = etBuyGST.getText().toString().trim();
        String buyTotal = etBuyTotal.getText().toString().trim();
        String buyGSTPercent = etBuyGSTPercent.getText().toString().trim();

        String sellPerson = etSellPerson.getText().toString().trim();
        String sellWeight = etSellWeight.getText().toString().trim();
        String sellPrice = etSellPrice.getText().toString().trim();
        String sellGST = etSellGST.getText().toString().trim();
        String sellTotal = etSellTotal.getText().toString().trim();
        String sellGSTPercent = etSellGSTPercent.getText().toString().trim();

        if(vehicle.isEmpty() || date.isEmpty() || factory.isEmpty()){
            Toast.makeText(this,"Please fill General section fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if((currentEntryType==ENTRY_TYPE_BUY || currentEntryType==ENTRY_TYPE_BOTH) && buyPrice.isEmpty()){
            Toast.makeText(this,"Please fill Buy Price", Toast.LENGTH_SHORT).show();
            return;
        }
        if((currentEntryType==ENTRY_TYPE_SELL || currentEntryType==ENTRY_TYPE_BOTH) && (sellPerson.isEmpty() || sellPrice.isEmpty())){
            Toast.makeText(this,"Please fill Sell Details", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = databaseReference.push().getKey();
        String buyWeightStr = (!weight.isEmpty()) ? weight : "";
        String sellWeightStr = (!sellWeight.isEmpty()) ? sellWeight : weight;

        TransportData data = new TransportData(vehicle,date,factory,measurement,weight,
                buyWeightStr,buyPrice,buyGST,buyTotal,buyGSTPercent,
                sellPerson,sellWeightStr,sellPrice,sellGST,sellTotal,sellGSTPercent
        );

        if(id!=null){
            databaseReference.child(id).setValue(data).addOnSuccessListener(aVoid -> {
                Toast.makeText(this,"Data Saved", Toast.LENGTH_SHORT).show();
                clearFields();
            });
        }
    }

    private void loadSuggestions(){
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot){
                vehicleList.clear(); factoryList.clear(); personList.clear();
                for(DataSnapshot data:snapshot.getChildren()){
                    TransportData t = data.getValue(TransportData.class);
                    if(t==null) continue;
                    if(t.vehicle!=null && !vehicleList.contains(t.vehicle)) vehicleList.add(t.vehicle);
                    if(t.factory!=null && !factoryList.contains(t.factory)) factoryList.add(t.factory);
                    if(t.sellPerson!=null && !personList.contains(t.sellPerson)) personList.add(t.sellPerson);
                }
                etVehicleNo.setAdapter(new ArrayAdapter<>(AddEntryActivity.this, android.R.layout.simple_dropdown_item_1line, vehicleList));
                etFactory.setAdapter(new ArrayAdapter<>(AddEntryActivity.this, android.R.layout.simple_dropdown_item_1line, factoryList));
                etSellPerson.setAdapter(new ArrayAdapter<>(AddEntryActivity.this, android.R.layout.simple_dropdown_item_1line, personList));
            }
            @Override public void onCancelled(@NonNull DatabaseError error){}
        });
    }

    private void clearFields(){
        String defaultGST = prefs.getString("default_gst","18");
        String defaultBuyPrice = prefs.getString("default_buy_price","");
        String defaultSellPrice = prefs.getString("default_sell_price","");
        String defaultUnit = prefs.getString("default_unit","Select Unit");

        etVehicleNo.setText("");
        Calendar c = Calendar.getInstance();
        etDate.setText(String.format("%02d/%02d/%d", c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.MONTH)+1, c.get(Calendar.YEAR)));
        etFactory.setText("");
        etWeight.setText("");

        // Buy section
        etBuyPrice.setText(defaultBuyPrice);
        etBuyGSTPercent.setText(defaultGST);
        calculateBuyGST(); // <-- Auto GST
        etBuyGST.setText("");
        etBuyTotal.setText("");

        // Sell section
        etSellPerson.setText("");
        etSellWeight.setText("");
        etSellPrice.setText(defaultSellPrice);
        etSellGSTPercent.setText(defaultGST);
        calculateSellGST(); // <-- Auto GST
        etSellGST.setText("");
        etSellTotal.setText("");

        // Unit Spinner
        String[] units = {"Select Unit","Ton","Quintal"};
        for(int i=0;i<units.length;i++){
            if(units[i].equals(defaultUnit)){
                spMeasurement.setSelection(i);
                break;
            }
        }
    }
}