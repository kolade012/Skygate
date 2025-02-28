package com.example.skygate;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;

public class InputFormatWatcher implements TextWatcher {
    private final EditText editText;
    private final InputValidator validator;
    private final String fieldName;
    private boolean isFormatting;

    public InputFormatWatcher(EditText editText, InputValidator validator, String fieldName) {
        this.editText = editText;
        this.validator = validator;
        this.fieldName = fieldName;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Not used
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Not used
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (isFormatting) return;
        isFormatting = true;

        try {
            // Format and validate based on field type
            switch (fieldName) {
                case "driver":
                    formatDriverName(s);
                    validator.validateDriver(editText);
                    break;
                case "quantity":
                    formatQuantity(s);
                    validator.validateQuantity(editText, "Quantity");
                    break;
            }
        } finally {
            isFormatting = false;
        }
    }

    private void formatDriverName(Editable s) {
        // Capitalize first letter of each word
        String text = s.toString();
        if (!TextUtils.isEmpty(text)) {
            StringBuilder formatted = new StringBuilder();
            boolean capitalizeNext = true;

            for (char c : text.toCharArray()) {
                if (Character.isWhitespace(c) || c == '-' || c == '\'') {
                    capitalizeNext = true;
                    formatted.append(c);
                } else {
                    if (capitalizeNext) {
                        formatted.append(Character.toUpperCase(c));
                        capitalizeNext = false;
                    } else {
                        formatted.append(Character.toLowerCase(c));
                    }
                }
            }

            if (!text.equals(formatted.toString())) {
                s.replace(0, s.length(), formatted.toString());
            }
        }
    }

    private void formatQuantity(Editable s) {
        String text = s.toString();
        if (!TextUtils.isEmpty(text)) {
            // Remove leading zeros
            if (text.length() > 1 && text.startsWith("0")) {
                s.replace(0, s.length(), text.replaceFirst("^0+", ""));
            }
            // Limit length
            if (text.length() > 4) {
                s.delete(4, s.length());
            }
        }
    }
}