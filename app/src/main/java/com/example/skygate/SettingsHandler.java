package com.example.skygate;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class    SettingsHandler {
    private static final String TAG = "SettingsHandler";
    public static final String PREFS_NAME = "AppSettings";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_EMAIL_ALERTS = "email_alerts_enabled";
    private static final String KEY_THEME = "app_theme";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String KEY_LAST_BACKUP = "last_backup_date";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "SettingsKey";
    private static final String FCM_TOKEN_PREF = "fcm_token";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final Context context;
    private final SharedPreferences preferences;
    private final Executor backgroundExecutor;
    private final KeyStore keyStore;
    private final SecretKey secretKey;
    private final SimpleDateFormat dateFormat;
    private final File backupDir;

    // Callback interfaces
    public interface SettingsCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface PasswordChangeCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface BackupCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface RestoreCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface CacheCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public enum Theme {
        LIGHT,
        DARK,
        SYSTEM_DEFAULT
    }

    public enum Language {
        ENGLISH("en"),
        SPANISH("es"),
        FRENCH("fr"),
        GERMAN("de"),
        CHINESE("zh");

        private final String code;

        Language(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public SettingsHandler(Context context) throws Exception {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.backgroundExecutor = Executors.newCachedThreadPool();
        this.keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        this.keyStore.load(null);
        this.secretKey = getOrCreateSecretKey();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
        this.backupDir = new File(context.getExternalFilesDir(null), "backups");

        initializeBackupDirectory();
        updateFCMToken();
    }

    private void initializeBackupDirectory() {
        if (!backupDir.exists()) {
            if (!backupDir.mkdirs()) {
                Log.e(TAG, "Failed to create backup directory");
            }
        }
    }

    private SecretKey getOrCreateSecretKey() throws Exception {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
            KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(false)
                    .build();
            keyGenerator.init(keySpec);
            return keyGenerator.generateKey();
        }
        return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
    }

    private void updateFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        setFCMToken(task.getResult());
                    }
                });
    }

    public void handleClearCache(CacheCallback callback) {
        Thread clearCacheThread = new Thread(() -> {
            try {
                // Clear internal cache
                File internalCacheDir = context.getCacheDir();
                boolean internalCacheCleared = deleteDir(internalCacheDir);

                // Clear external cache if available
                File externalCacheDir = context.getExternalCacheDir();
                boolean externalCacheCleared = externalCacheDir != null && deleteDir(externalCacheDir);

                // Calculate total cleared space
                long clearedSpace = calculateClearedSpace(internalCacheDir, externalCacheDir);

                if (internalCacheCleared || externalCacheCleared) {
                    String message = String.format("Cache cleared successfully (%.2f MB freed)",
                            clearedSpace / (1024.0 * 1024.0));
                    callback.onSuccess(message);
                } else {
                    callback.onError("No cache to clear");
                }
            } catch (Exception e) {
                callback.onError("Error clearing cache: " + e.getMessage());
            }
        });
        clearCacheThread.start();
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDir(file);
                    } else {
                        file.delete();
                    }
                }
            }
            // Don't delete the cache directory itself, just its contents
            return true;
        }
        return false;
    }

    private long calculateClearedSpace(File... dirs) {
        long totalSize = 0;
        for (File dir : dirs) {
            if (dir != null && dir.exists()) {
                totalSize += getDirSize(dir);
            }
        }
        return totalSize;
    }

    private long getDirSize(File dir) {
        long size = 0;
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += getDirSize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }

    // Password Management
    public void handleChangePassword(String currentPassword, String newPassword,
                                     PasswordChangeCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            callback.onError("User not authenticated");
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    user.updatePassword(newPassword)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    callback.onSuccess("Password updated successfully");
                                    handleBackup(null); // Automatic backup after password change
                                } else {
                                    callback.onError(task.getException() != null ?
                                            task.getException().getMessage() :
                                            "Failed to update password");
                                }
                            });
                })
                .addOnFailureListener(e -> callback.onError("Authentication failed: " +
                        e.getMessage()));
    }

    // Backup Management
    public void handleBackup(BackupCallback callback) {
        backgroundExecutor.execute(() -> {
            try {
                String backupFileName = "settings_backup_" + dateFormat.format(new Date()) + ".json";
                File backupFile = new File(backupDir, backupFileName);

                JSONObject backupData = new JSONObject();
                Map<String, ?> allPrefs = preferences.getAll();
                for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
                    backupData.put(entry.getKey(), entry.getValue().toString());
                }

                // Encrypt backup data
                byte[] encryptedData = encrypt(backupData.toString().getBytes(StandardCharsets.UTF_8));

                try (FileOutputStream fos = new FileOutputStream(backupFile)) {
                    fos.write(Base64.encode(encryptedData, Base64.DEFAULT));
                }

                preferences.edit()
                        .putString(KEY_LAST_BACKUP, dateFormat.format(new Date()))
                        .apply();

                callback.onSuccess("Backup created successfully: " + backupFileName);
            } catch (Exception e) {
                Log.e(TAG, "Backup failed", e);
                callback.onError("Backup failed: " + e.getMessage());
            }
        });
    }

    public void handleRestore(File backupFile, RestoreCallback callback) {
        backgroundExecutor.execute(() -> {
            try {
                byte[] encryptedData = new byte[(int) backupFile.length()];
                try (FileInputStream fis = new FileInputStream(backupFile)) {
                    if (fis.read(encryptedData) == -1) {
                        throw new IOException("Failed to read backup file");
                    }
                }

                byte[] decryptedData = decrypt(Base64.decode(encryptedData, Base64.DEFAULT));
                JSONObject backupData = new JSONObject(new String(decryptedData,
                        StandardCharsets.UTF_8));

                SharedPreferences.Editor editor = preferences.edit();
                Iterator<String> keys = backupData.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = backupData.getString(key);

                    // Handle different types of preferences
                    if (value.equals("true") || value.equals("false")) {
                        editor.putBoolean(key, Boolean.parseBoolean(value));
                    } else if (value.matches("\\d+")) {
                        editor.putInt(key, Integer.parseInt(value));
                    } else {
                        editor.putString(key, value);
                    }
                }
                editor.apply();

                callback.onSuccess("Settings restored successfully");
            } catch (Exception e) {
                Log.e(TAG, "Restore failed", e);
                callback.onError("Restore failed: " + e.getMessage());
            }
        });
    }

    // Encryption utilities
    private byte[] encrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(data);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(iv);
        outputStream.write(encrypted);
        return outputStream.toByteArray();
    }

    private byte[] decrypt(byte[] encryptedData) throws Exception {
        byte[] iv = Arrays.copyOfRange(encryptedData, 0, GCM_IV_LENGTH);
        byte[] encrypted = Arrays.copyOfRange(encryptedData, GCM_IV_LENGTH, encryptedData.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        return cipher.doFinal(encrypted);
    }

    // Settings Management
    public void handleNotificationSettings(boolean enabled, SettingsCallback callback) {
        try {
            setNotificationsEnabled(enabled);
            if (enabled) {
                updateFCMToken();
            }
            callback.onSuccess("Notification settings updated");
        } catch (Exception e) {
            Log.e(TAG, "Failed to update notification settings", e);
            callback.onError("Failed to update notification settings: " + e.getMessage());
        }
    }

    public void handleEmailAlerts(boolean enabled, SettingsCallback callback) {
        try {
            setEmailAlertsEnabled(enabled);
            callback.onSuccess("Email alerts settings updated");
        } catch (Exception e) {
            Log.e(TAG, "Failed to update email alerts", e);
            callback.onError("Failed to update email alerts: " + e.getMessage());
        }
    }

    public void handleThemeChange(Theme theme, SettingsCallback callback) {
        try {
            preferences.edit().putString(KEY_THEME, theme.name()).apply();
            callback.onSuccess("Theme updated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to update theme", e);
            callback.onError("Failed to update theme: " + e.getMessage());
        }
    }

    public void handleLanguageChange(Language language, SettingsCallback callback) {
        try {
            preferences.edit().putString(KEY_LANGUAGE, language.getCode()).apply();
            callback.onSuccess("Language updated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to update language", e);
            callback.onError("Failed to update language: " + e.getMessage());
        }
    }

    // Getters and Setters
    public void setNotificationsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    public boolean areNotificationsEnabled() {
        return preferences.getBoolean(KEY_NOTIFICATIONS, true);
    }

    public void setEmailAlertsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_EMAIL_ALERTS, enabled).apply();
    }

    public boolean areEmailAlertsEnabled() {
        return preferences.getBoolean(KEY_EMAIL_ALERTS, true);
    }

    public void setFCMToken(String token) {
        preferences.edit().putString(FCM_TOKEN_PREF, token).apply();
    }

    public String getFCMToken() {
        return preferences.getString(FCM_TOKEN_PREF, "");
    }

    public Theme getCurrentTheme() {
        String themeName = preferences.getString(KEY_THEME, Theme.SYSTEM_DEFAULT.name());
        return Theme.valueOf(themeName);
    }

    public Language getCurrentLanguage() {
        String languageCode = preferences.getString(KEY_LANGUAGE, Language.ENGLISH.getCode());
        for (Language language : Language.values()) {
            if (language.getCode().equals(languageCode)) {
                return language;
            }
        }
        return Language.ENGLISH;
    }

    public String getUserEmail() {
        return preferences.getString(KEY_USER_EMAIL, "");
    }

    public void setUserEmail(String email) {
        preferences.edit().putString(KEY_USER_EMAIL, email).apply();
    }

    public String getLastBackupDate() {
        return preferences.getString(KEY_LAST_BACKUP, null);
    }

    public void clearAllData(SettingsCallback callback) {
        backgroundExecutor.execute(() -> {
            try {
                // Clear preferences
                preferences.edit().clear().apply();

                // Clear cache
                File cacheDir = context.getCacheDir();
                deleteRecursive(cacheDir);

                // Clear backup files
                for (File file : backupDir.listFiles()) {
                    file.delete();
                }

                callback.onSuccess("All data cleared successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear all data", e);
                callback.onError("Failed to clear data: " + e.getMessage());
            }
        });
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }
}