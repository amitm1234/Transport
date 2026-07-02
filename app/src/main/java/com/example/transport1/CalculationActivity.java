package com.example.transport1;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.app.DatePickerDialog;
import java.util.Calendar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CalculationActivity extends AppCompatActivity {
	
	private EditText etFromDate, etToDate;
	private CheckBox checkWeight;

	private TextView tvTotalVehicles, tvTotalWeight;
	private Button btnCalculate;
	
	private DatabaseReference entryRef;
	private FirebaseAuth mAuth;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_calculation);
		
		etFromDate = findViewById(R.id.etFromDate);
		etToDate = findViewById(R.id.etToDate);
		checkWeight = findViewById(R.id.checkWeight);
		tvTotalVehicles = findViewById(R.id.tvTotalVehicles);
		tvTotalWeight = findViewById(R.id.tvTotalWeight);
		btnCalculate = findViewById(R.id.btnCalculate);
		
		mAuth = FirebaseAuth.getInstance();
		
		if (mAuth.getCurrentUser() == null) {
			Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		String userId = mAuth.getCurrentUser().getUid();
		
		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		
		if (currentUser != null) {
			entryRef = FirebaseDatabase.getInstance()
			.getReference("transport_data")
			.child(currentUser.getUid());
		}
		
		etFromDate.setOnClickListener(v -> showDatePicker(etFromDate));
etToDate.setOnClickListener(v -> showDatePicker(etToDate));
		
		btnCalculate.setOnClickListener(v -> calculateData());
	}
	
	private void showDatePicker(EditText editText) {

    final Calendar calendar = Calendar.getInstance();

    int year = calendar.get(Calendar.YEAR);
    int month = calendar.get(Calendar.MONTH);
    int day = calendar.get(Calendar.DAY_OF_MONTH);

    DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, selectedYear, selectedMonth, selectedDay) -> {

                // Month +1 कारण month 0 पासून सुरू होतो
                selectedMonth = selectedMonth + 1;

                String date = selectedDay + "/" + selectedMonth + "/" + selectedYear;
				editText.setText(date);

            },
            year, month, day
    );

    datePickerDialog.show();
}
	
	private void calculateData() {
		
		String fromDateStr = etFromDate.getText().toString().trim();
		String toDateStr = etToDate.getText().toString().trim();
		
		if (fromDateStr.isEmpty() || toDateStr.isEmpty()) {
			Toast.makeText(this, "Enter both dates", Toast.LENGTH_SHORT).show();
			return;
		}
		
		try {
			
			// Date format: 1.2.2026
			SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
			Date fromDate = sdf.parse(fromDateStr);
			Date toDate = sdf.parse(toDateStr);
			
			if (fromDate == null || toDate == null) {
				Toast.makeText(this, "Invalid Date Format", Toast.LENGTH_SHORT).show();
				return;
			}
			
			entryRef.addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					
					double totalWeight = 0;
					int totalVehicles = 0;
					
					for (DataSnapshot data : snapshot.getChildren()) {
						
						String dateStr = data.child("date").getValue(String.class);
						String weightStr = data.child("weight").getValue(String.class);
						String measurement = data.child("measurement").getValue(String.class);
						
						if (dateStr != null) {
							
							try {
								Date entryDate = sdf.parse(dateStr);
								
								if (entryDate != null &&
								!entryDate.before(fromDate) &&
								!entryDate.after(toDate)) {
									
									totalVehicles++;
									
									if (weightStr != null) {
										
										double weight = Double.parseDouble(weightStr);
										
										// Convert to Quintal (example base unit)
										if (measurement != null) {
											
											if (measurement.equalsIgnoreCase("Ton")) {
												weight = weight * 10;   // 1 Ton = 10 Quintal
											}
											
											// If already Quintal, no change
										}
										
										totalWeight += weight;
									}
								}
								
								} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					
					tvTotalVehicles.setText("Total Vehicles: " + totalVehicles);
					
					if (checkWeight.isChecked()) {
						tvTotalWeight.setVisibility(View.VISIBLE);
						tvTotalWeight.setText("Total Weight: " + totalWeight);
						} else {
						tvTotalWeight.setVisibility(View.GONE);
					}
				}
				
				@Override
				public void onCancelled(@NonNull DatabaseError error) {
					Toast.makeText(CalculationActivity.this,
					"Error: " + error.getMessage(),
					Toast.LENGTH_SHORT).show();
				}
			});
			
			} catch (Exception e) {
			Toast.makeText(this, "Date Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
}