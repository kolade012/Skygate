package com.example.skygate.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "BeverageAppSession";
    private static final String KEY_USER_ROLE = "userRole";
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void setUserRole(String role) {
        editor.putString(KEY_USER_ROLE, role);
        editor.commit();
    }

    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, null);
    }

    public void clearSession() {
        editor.clear();
        editor.commit();
    }

    public boolean isLoggedIn() {
        return getUserRole() != null;
    }
}