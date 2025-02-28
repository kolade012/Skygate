package com.example.skygate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

/** @noinspection ALL */
public class settings extends AppCompatActivity {
    private TextInputEditText emailEditText;  // Change the type here
    private SwitchMaterial notificationsSwitch;
    private SwitchMaterial emailAlertsSwitch;
    private Spinner languageSpinner;
    private Button changePasswordButton;
    private Button clearCacheButton;
    private Button backupButton;
    private Button clearAllDataButton;
    private Toolbar toolbar;
    private SettingsHandler settingsHandler;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] SUPPORTED_LANGUAGES = {"English", "Spanish", "French", "German", "Chinese"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Remove EdgeToEdge.enable() as it's causing issues
        // EdgeToEdge.enable(this);

        try {
            settingsHandler = new SettingsHandler(this);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing settings: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        setupLanguageSpinner();
        setupClickListeners();
        loadSavedSettings();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar_settings);
        emailEditText = findViewById(R.id.edit_email);
        notificationsSwitch = findViewById(R.id.switch_notifications);
        emailAlertsSwitch = findViewById(R.id.switch_email_alerts);
        languageSpinner = findViewById(R.id.spinner_language);
        changePasswordButton = findViewById(R.id.btn_change_password);
        clearCacheButton = findViewById(R.id.btn_clear_cache);
        backupButton = findViewById(R.id.btn_backup);
        clearAllDataButton = findViewById(R.id.btn_clear_all_data);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupLanguageSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                SUPPORTED_LANGUAGES
        );
        languageSpinner.setAdapter(adapter);
        languageSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                       int position, long id) {
                String selectedLanguage = parent.getItemAtPosition(position).toString();
                updateAppLanguage(selectedLanguage);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void updateAppLanguage(String language) {
        // Get current language to check if it's actually changed
        String currentLanguage = getSharedPreferences(SettingsHandler.PREFS_NAME, MODE_PRIVATE)
                .getString("selected_language", "English");

        // Only proceed if the language has actually changed
        if (!currentLanguage.equals(language)) {
            android.content.res.Configuration configuration =
                    new android.content.res.Configuration(getResources().getConfiguration());
            java.util.Locale newLocale = null;

            switch (language) {
                case "English":
                    newLocale = new java.util.Locale("en");
                    break;
                case "Spanish":
                    newLocale = new java.util.Locale("es");
                    break;
                case "French":
                    newLocale = new java.util.Locale("fr");
                    break;
                case "German":
                    newLocale = new java.util.Locale("de");
                    break;
                case "Chinese":
                    newLocale = new java.util.Locale("zh");
                    break;
            }

            if (newLocale != null) {
                configuration.setLocale(newLocale);

                // Save the selected language
                getSharedPreferences(SettingsHandler.PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putString("selected_language", language)
                        .apply();

                // Update configuration
                getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());

                // Instead of recreate(), restart the activity properly
                Intent intent = new Intent(this, settings.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        }
    }

    private void setupClickListeners() {
        changePasswordButton.setOnClickListener(v -> handleChangePassword());
        clearCacheButton.setOnClickListener(v -> handleClearCache());
        backupButton.setOnClickListener(v -> handleBackup());

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                handleNotificationSettings(isChecked));

        emailAlertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                handleEmailAlerts(isChecked));

        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateAndSaveEmail();
            }
        });
        clearAllDataButton.setOnClickListener(v -> handleClearAllData());
    }

    private void handleClearAllData() {
        settingsHandler.clearAllData(new SettingsHandler.SettingsCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(settings.this, message,
                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(settings.this, error,
                        Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadSavedSettings() {
        android.content.SharedPreferences prefs =
                getSharedPreferences(SettingsHandler.PREFS_NAME, MODE_PRIVATE);
        emailEditText.setText(prefs.getString("user_email", ""));
        notificationsSwitch.setChecked(prefs.getBoolean("notifications_enabled", false));
        emailAlertsSwitch.setChecked(prefs.getBoolean("email_alerts_enabled", false));

        String savedLanguage = prefs.getString("selected_language", "English");
        for (int i = 0; i < SUPPORTED_LANGUAGES.length; i++) {
            if (SUPPORTED_LANGUAGES[i].equals(savedLanguage)) {
                languageSpinner.setSelection(i);
                break;
            }
        }
    }

    private void validateAndSaveEmail() {
        String email = emailEditText.getText().toString().trim();
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email address");
            return;
        }
        getSharedPreferences(SettingsHandler.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString("user_email", email)
                .apply();
    }

    private void handleChangePassword() {
        settingsHandler.handleChangePassword("", "", new SettingsHandler.PasswordChangeCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(settings.this, message,
                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(settings.this, error,
                        Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void handleClearCache() {
        settingsHandler.handleClearCache(new SettingsHandler.CacheCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(settings.this, message,
                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(settings.this, error,
                        Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void handleBackup() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
            return;
        }

        settingsHandler.handleBackup(new SettingsHandler.BackupCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(settings.this, message,
                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(settings.this, error,
                        Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void handleNotificationSettings(boolean enabled) {
        settingsHandler.handleNotificationSettings(enabled, new SettingsHandler.SettingsCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(settings.this, message,
                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(settings.this, error, Toast.LENGTH_SHORT).show();
                    notificationsSwitch.setChecked(!enabled);
                });
            }
        });
    }

    private void handleEmailAlerts(boolean enabled) {
        settingsHandler.handleEmailAlerts(enabled, new SettingsHandler.SettingsCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> Toast.makeText(settings.this, message,
                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(settings.this, error, Toast.LENGTH_SHORT).show();
                    emailAlertsSwitch.setChecked(!enabled);
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handleBackup();
            } else {
                Toast.makeText(this, "Storage permission is required for backup",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        validateAndSaveEmail();
    }

    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}