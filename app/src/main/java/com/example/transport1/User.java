package com.example.transport1;

/**
 * User model class for Firebase.
 * Stores user profile data including name, email, company, photo URL, and role.
 */
public class User {

	public String name;
	public String email;
	public String companyName;
	public String photoUrl;
	public String role; // "user" or "admin"

	// Default constructor required for Firebase deserialization
	public User() {
	}

	// Constructor to create user object
	public User(String name, String email, String companyName, String photoUrl, String role) {
		this.name = name;
		this.email = email;
		this.companyName = companyName;
		this.photoUrl = photoUrl;
		this.role = role;
	}

	// Optional: you can add helper methods like isAdmin()
	public boolean isAdmin() {
		return "admin".equalsIgnoreCase(this.role);
	}
}