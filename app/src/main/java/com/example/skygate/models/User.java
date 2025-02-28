package com.example.skygate.models;

public class User {
    private String email;
    private String role;

    // Default constructor required for Firestore
    public User() {}

    public User(String email, String role) {
        this.email = email;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}