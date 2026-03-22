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
import android.content.Intent; // ✅ ADD

public class AdminActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> userList;
    ArrayList<String> userUidList; // ✅ FIX
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference checkAdmin = FirebaseDatabase.getInstance()
                .getReference("admins").child(uid);

        checkAdmin.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
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
        userUidList = new ArrayList<>();

        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        databaseReference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                userList.clear();
                userUidList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {

                    String uid = ds.getKey();
                    String name = ds.child("name").getValue(String.class);
                    String email = ds.child("email").getValue(String.class);

                    if (name != null && email != null && uid != null) {
                        userList.add(name + " (" + email + ")");
                        userUidList.add(uid);
                    }
                }

                listView.setAdapter(
                        new ArrayAdapter<>(AdminActivity.this,
                                android.R.layout.simple_list_item_1,
                                userList));

                // 🔥 CLICK
                listView.setOnItemClickListener((parent, view, position, id) -> {

                    String selectedUid = userUidList.get(position);

                    BaseActivity.overrideUid = selectedUid; // 🔥 MAIN LOGIC

                    Toast.makeText(AdminActivity.this,
                            "Opening User Data...",
                            Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(AdminActivity.this, MainActivity.class));
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}