package com.example.transport1;


import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;

public class AdminActivity extends AppCompatActivity {

	ListView listView;
	ArrayList<String> userList;
	DatabaseReference databaseReference;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_admin);

		// 🔐 Check Login
		if (FirebaseAuth.getInstance().getCurrentUser() == null) {
			finish();
			return;
		}

		String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

		// 🔐 Check Admin Permission
		DatabaseReference checkAdmin = FirebaseDatabase.getInstance().getReference("admins").child(uid);

		checkAdmin.get().addOnSuccessListener(snapshot -> {

			if (snapshot.exists()) {
				// ✅ User is Admin → Load Users
				loadUsers();
			} else {
				Toast.makeText(this, "Access Denied!", Toast.LENGTH_LONG).show();
				finish();
			}
		});
	}

	private void loadUsers() {

		listView = findViewById(R.id.listViewUsers);
		userList = new ArrayList<>();

		databaseReference = FirebaseDatabase.getInstance().getReference("users");

		databaseReference.addValueEventListener(new ValueEventListener() {

			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {

				userList.clear();

				for (DataSnapshot ds : snapshot.getChildren()) {

					String name = ds.child("name").getValue(String.class);
					String email = ds.child("email").getValue(String.class);

					if (name != null && email != null) {
						userList.add(name + " (" + email + ")");
					}
				}

				listView.setAdapter(
						new ArrayAdapter<>(AdminActivity.this, android.R.layout.simple_list_item_1, userList));
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Toast.makeText(AdminActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
			}
		});
	}
}