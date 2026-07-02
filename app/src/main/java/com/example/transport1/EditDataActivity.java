package com.example.transport1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EditDataActivity extends AppCompatActivity {

    EditText etVehicle, etFactory, etDate, etWeight, etMeasurement;
    EditText etBuyWeight, etBuyPrice, etBuyGST;
    EditText etSellPerson, etSellWeight, etSellPrice, etSellGST;

    Button btnUpdate;

    DatabaseReference databaseReference;
    String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_data);

        // Find Views
        etVehicle = findViewById(R.id.etVehicle);
        etFactory = findViewById(R.id.etFactory);
        etDate = findViewById(R.id.etDate);
        etWeight = findViewById(R.id.etWeight);
        etMeasurement = findViewById(R.id.etMeasurement);

        etBuyWeight = findViewById(R.id.etBuyWeight);
        etBuyPrice = findViewById(R.id.etBuyPrice);
        etBuyGST = findViewById(R.id.etBuyGST);

        etSellPerson = findViewById(R.id.etSellPerson);
        etSellWeight = findViewById(R.id.etSellWeight);
        etSellPrice = findViewById(R.id.etSellPrice);
        etSellGST = findViewById(R.id.etSellGST);

        btnUpdate = findViewById(R.id.btnUpdate);

        // Receive ID
        id = getIntent().getStringExtra("id");

        if(id == null){
            Toast.makeText(this,"Error loading data",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Receive old data
        String vehicle = getIntent().getStringExtra("vehicle");
        String factory = getIntent().getStringExtra("factory");
        String date = getIntent().getStringExtra("date");
        String weight = getIntent().getStringExtra("weight");
        String measurement = getIntent().getStringExtra("measurement");

        String buyWeight = getIntent().getStringExtra("buyWeight");
        String buyPrice = getIntent().getStringExtra("buyPrice");
        String buyGST = getIntent().getStringExtra("buyGST");

        String sellPerson = getIntent().getStringExtra("sellPerson");
        String sellWeight = getIntent().getStringExtra("sellWeight");
        String sellPrice = getIntent().getStringExtra("sellPrice");
        String sellGST = getIntent().getStringExtra("sellGST");

        // Auto fill fields
        etVehicle.setText(vehicle);
        etFactory.setText(factory);
        etDate.setText(date);
        etWeight.setText(weight);
        etMeasurement.setText(measurement);

        etBuyWeight.setText(buyWeight);
        etBuyPrice.setText(buyPrice);
        etBuyGST.setText(buyGST);

        etSellPerson.setText(sellPerson);
        etSellWeight.setText(sellWeight);
        etSellPrice.setText(sellPrice);
        etSellGST.setText(sellGST);

        // Firebase User
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(user == null){
            Toast.makeText(this,"User not logged in",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseReference = FirebaseDatabase.getInstance()
                .getReference("transport_data")
                .child(user.getUid());

        // Update Button
        btnUpdate.setOnClickListener(v -> {

            databaseReference.child(id).child("vehicle")
                    .setValue(etVehicle.getText().toString());

            databaseReference.child(id).child("factory")
                    .setValue(etFactory.getText().toString());

            databaseReference.child(id).child("date")
                    .setValue(etDate.getText().toString());

            databaseReference.child(id).child("weight")
                    .setValue(etWeight.getText().toString());

            databaseReference.child(id).child("measurement")
                    .setValue(etMeasurement.getText().toString());

            databaseReference.child(id).child("buyWeight")
                    .setValue(etBuyWeight.getText().toString());

            databaseReference.child(id).child("buyPrice")
                    .setValue(etBuyPrice.getText().toString());

            databaseReference.child(id).child("buyGST")
                    .setValue(etBuyGST.getText().toString());

            databaseReference.child(id).child("sellPerson")
                    .setValue(etSellPerson.getText().toString());

            databaseReference.child(id).child("sellWeight")
                    .setValue(etSellWeight.getText().toString());

            databaseReference.child(id).child("sellPrice")
                    .setValue(etSellPrice.getText().toString());

            databaseReference.child(id).child("sellGST")
                    .setValue(etSellGST.getText().toString());

            Toast.makeText(this,"Data Updated Successfully",Toast.LENGTH_SHORT).show();

            finish();
        });

    }
}