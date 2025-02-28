package com.example.skygate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class UserTypeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_type);

        Button btnAdmin = findViewById(R.id.btnAdmin);
        Button btnStaff = findViewById(R.id.btnStaff);

        btnAdmin.setOnClickListener(v -> {
            startActivity(new Intent(UserTypeActivity.this, AdminLoginActivity.class));
        });

        btnStaff.setOnClickListener(v -> {
            startActivity(new Intent(UserTypeActivity.this, StaffLoginActivity.class));
        });
    }
}