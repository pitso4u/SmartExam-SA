package com.smartexam.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.smartexam.models.TeacherSettings;

public class TeacherSettingsRepository {

    private static final String PREFS_NAME = "teacher_settings_prefs";
    private static final String KEY_SETTINGS_JSON = "settings_json";

    private final SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();

    public TeacherSettingsRepository(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public TeacherSettings getSettings() {
        String json = sharedPreferences.getString(KEY_SETTINGS_JSON, null);
        if (json == null || json.isEmpty()) {
            return new TeacherSettings();
        }
        try {
            TeacherSettings stored = gson.fromJson(json, TeacherSettings.class);
            return stored != null ? stored : new TeacherSettings();
        } catch (Exception e) {
            return new TeacherSettings();
        }
    }

    public void saveSettings(TeacherSettings settings) {
        String json = gson.toJson(settings != null ? settings : new TeacherSettings());
        sharedPreferences.edit().putString(KEY_SETTINGS_JSON, json).apply();
    }
}
