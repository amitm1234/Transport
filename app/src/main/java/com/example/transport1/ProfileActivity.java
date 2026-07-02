package com.example.transport1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.transport1.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class ProfileActivity extends AppCompatActivity {

	private ActivityProfileBinding binding;

	private FirebaseAuth mAuth;
	private DatabaseReference userRef;

	private static final int PICK_IMAGE_REQUEST = 1;
	private Uri imageUri;
	private String savedImageUri;
	private String savedRole;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		binding = ActivityProfileBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		mAuth = FirebaseAuth.getInstance();
		FirebaseUser currentUser = mAuth.getCurrentUser();

		if (currentUser == null) {
			Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		binding.etEmail.setText(currentUser.getEmail());
		binding.etEmail.setEnabled(false);

		userRef = FirebaseDatabase.getInstance()
				.getReference("users")
				.child(currentUser.getUid());

		binding.fabEditPhoto.setOnClickListener(v -> openGallery());
		binding.btnSave.setOnClickListener(v -> saveUserInfo());

		loadUserInfo();
	}

	private void openGallery() {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
			imageUri = data.getData();
			Glide.with(this).load(imageUri).into(binding.ivProfile);
		}
	}

	private void saveUserInfo() {
		String name = binding.etName.getText().toString().trim();
		String companyName = binding.etCompanyName.getText().toString().trim();
		String roleInput = binding.etRole.getText().toString().trim();

		if (TextUtils.isEmpty(name)) {
			binding.etName.setError("Name is required");
			binding.etName.requestFocus();
			return;
		}

		String role = !TextUtils.isEmpty(roleInput) ? roleInput : "user";
		binding.progressBar.setVisibility(View.VISIBLE);
		binding.btnSave.setEnabled(false);

		if (imageUri != null) {
			String userId = mAuth.getCurrentUser().getUid();
			StorageReference storageRef = FirebaseStorage.getInstance()
					.getReference()
					.child("profile_photos/" + userId + ".jpg");

			// Use putFile instead of putStream for local Uri
			storageRef.putFile(imageUri).continueWithTask(task -> {
				if (!task.isSuccessful()) {
					throw task.getException();
				}
				return storageRef.getDownloadUrl();
			}).addOnCompleteListener(task -> {
				if (task.isSuccessful()) {
					Uri downloadUri = task.getResult();
					updateDatabase(name, companyName, downloadUri.toString(), role);
				} else {
					binding.progressBar.setVisibility(View.GONE);
					binding.btnSave.setEnabled(true);
					Toast.makeText(ProfileActivity.this, "Upload failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
				}
			});
		} else {
			updateDatabase(name, companyName, savedImageUri, role);
		}
	}

	private void updateDatabase(String name, String companyName, String photoUrl, String role) {
		User user = new User(name, binding.etEmail.getText().toString(), companyName, photoUrl, role);
		userRef.setValue(user).addOnCompleteListener(task -> {
			binding.progressBar.setVisibility(View.GONE);
			binding.btnSave.setEnabled(true);
			if (task.isSuccessful()) {
				Toast.makeText(ProfileActivity.this, "Profile Updated!", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(ProfileActivity.this, "Database error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
			}
		});
	}

	private void loadUserInfo() {
		userRef.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot.exists()) {
					User user = snapshot.getValue(User.class);
					if (user != null) {
						binding.etName.setText(user.name != null ? user.name : "");
						binding.etCompanyName.setText(user.companyName != null ? user.companyName : "");
						savedRole = user.role != null ? user.role : "user";
						binding.etRole.setText(savedRole);

						savedImageUri = user.photoUrl;
						if (savedImageUri != null && !savedImageUri.isEmpty()) {
							Glide.with(ProfileActivity.this)
									.load(savedImageUri)
									.placeholder(R.drawable.ic_launcher_background)
									.into(binding.ivProfile);
						}
					}
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Toast.makeText(ProfileActivity.this, "Load failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
			}
		});
	}
}