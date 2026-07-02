package com.example.transport1;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class AddPaymentActivity extends AppCompatActivity {

    EditText edtAmount, edtDate;
    Button btnSave;

    String personName, factoryName;
    DatabaseReference paymentRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_payment);

        edtAmount = findViewById(R.id.edtAmount);
        edtDate = findViewById(R.id.edtDate);
        btnSave = findViewById(R.id.btnSave);

        // Get intent extras
        personName = getIntent().getStringExtra("personName");
        factoryName = getIntent().getStringExtra("factoryName");

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Decide Firebase path based on what is passed
        if(factoryName != null){
            // Factory advance payments
            paymentRef = FirebaseDatabase.getInstance()
                    .getReference("transport_data")
                    .child(uid)
                    .child("factory_payments")
                    .child(factoryName);
        } else if(personName != null){
            // Sell person payments
            paymentRef = FirebaseDatabase.getInstance()
                    .getReference("transport_data")
                    .child(uid)
                    .child("payments")
                    .child(personName);
        } else {
            Toast.makeText(this, "No target for payment!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Date picker
        edtDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int mYear = c.get(Calendar.YEAR);
            int mMonth = c.get(Calendar.MONTH);
            int mDay = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, monthOfYear, dayOfMonth) -> {
                        String dateStr = dayOfMonth + "/" + (monthOfYear+1) + "/" + year;
                        edtDate.setText(dateStr);
                    }, mYear, mMonth, mDay);
            datePickerDialog.show();
        });

        btnSave.setOnClickListener(v -> savePayment());
    }

    private void savePayment() {
        String amount = edtAmount.getText().toString().trim();
        String date = edtDate.getText().toString().trim();

        if(amount.isEmpty() || date.isEmpty()){
            Toast.makeText(this, "Enter amount and date", Toast.LENGTH_SHORT).show();
            return;
        }

        String paymentId = paymentRef.push().getKey();
        if(paymentId != null){
            paymentRef.child(paymentId).child("amount").setValue(amount);
            paymentRef.child(paymentId).child("date").setValue(date);

            Toast.makeText(this, "Payment saved", Toast.LENGTH_SHORT).show();
            finish(); // go back to report
        }
    }
}