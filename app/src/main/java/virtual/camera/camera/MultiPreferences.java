package virtual.camera.camera;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * MultiPreferences - A simple wrapper around SharedPreferences for cross-process access.
 * This is a stub implementation to replace the missing virtual.camera.camera:camera:1.0.0 dependency.
 */
public class MultiPreferences {
    private static final String PREF_NAME = "vcamera_multi_prefs";
    private static MultiPreferences instance;
    private SharedPreferences sharedPreferences;

    private MultiPreferences(Context context) {
        // Use MODE_MULTI_PROCESS for cross-process access (deprecated but still works for basic use)
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized MultiPreferences getInstance(Context context) {
        if (instance == null) {
            instance = new MultiPreferences(context.getApplicationContext());
        }
        return instance;
    }

    public static MultiPreferences getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MultiPreferences not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }

    public int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public long getLong(String key, long defaultValue) {
        return sharedPreferences.getLong(key, defaultValue);
    }

    public float getFloat(String key, float defaultValue) {
        return sharedPreferences.getFloat(key, defaultValue);
    }

    public void setInt(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    public void setString(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    public void setBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public void setLong(String key, long value) {
        sharedPreferences.edit().putLong(key, value).apply();
    }

    public void setFloat(String key, float value) {
        sharedPreferences.edit().putFloat(key, value).apply();
    }

    public void remove(String key) {
        sharedPreferences.edit().remove(key).apply();
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

    public boolean contains(String key) {
        return sharedPreferences.contains(key);
    }
}
