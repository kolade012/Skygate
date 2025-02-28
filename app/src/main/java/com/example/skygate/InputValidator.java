package com.example.skygate;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

public class InputValidator {
    private static final String TAG = "InputValidator";
    private final Context context;

    // Constants for validation
    private static final int MAX_DRIVER_LENGTH = 50;
    private static final int MAX_QUANTITY = 9999;
    private static final String DRIVER_NAME_PATTERN = "^[a-zA-Z\\s'-]+$";

    public InputValidator(Context context) {
        this.context = context;
    }

    public boolean validateDriver(EditText driverEdit) {
        String driver = driverEdit.getText().toString().trim();

        if (TextUtils.isEmpty(driver)) {
            showError(driverEdit, "Driver name is required");
            return false;
        }

        if (driver.length() > MAX_DRIVER_LENGTH) {
            showError(driverEdit, "Driver name is too long (max " + MAX_DRIVER_LENGTH + " characters)");
            return false;
        }

        if (!driver.matches(DRIVER_NAME_PATTERN)) {
            showError(driverEdit, "Driver name can only contain letters, spaces, hyphens and apostrophes");
            return false;
        }

        clearError(driverEdit);
        return true;
    }

    public boolean validateQuantity(EditText quantityEdit, String fieldName) {
        String quantity = quantityEdit.getText().toString().trim();

        if (!TextUtils.isEmpty(quantity)) {
            try {
                int value = Integer.parseInt(quantity);
                if (value < 0) {
                    showError(quantityEdit, fieldName + " cannot be negative");
                    return false;
                }
                if (value > MAX_QUANTITY) {
                    showError(quantityEdit, fieldName + " cannot exceed " + MAX_QUANTITY);
                    return false;
                }
                clearError(quantityEdit);
                return true;
            } catch (NumberFormatException e) {
                showError(quantityEdit, "Please enter a valid number");
                return false;
            }
        }
        clearError(quantityEdit);
        return true;
    }

    public boolean validateDate(EditText dateEdit) {
        String date = dateEdit.getText().toString().trim();

        if (TextUtils.isEmpty(date)) {
            showError(dateEdit, "Date is required");
            return false;
        }

        // Date is set by DatePickerDialog so no need for format validation
        clearError(dateEdit);
        return true;
    }

    private void showError(EditText editText, String message) {
        editText.setError(message);
        editText.requestFocus();

        // Show visual feedback
        editText.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, android.R.color.holo_red_light)));

        // Show Toast message
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private void clearError(EditText editText) {
        editText.setError(null);
        editText.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, android.R.color.darker_gray)));
    }
}