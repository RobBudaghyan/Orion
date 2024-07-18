package com.example.orion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

class SharedPreferencesHelper {
    private static final String KEY_STORAGE_CODE = "nQbcYh5VfI";
    private static final String PRIVATE_KEY_CODE = "Kp!KMTzKXw";
    private static final String PREFS_NAME = "launch_prefs";
    private static final String KEY_FIRST_LAUNCH = "first_launch";

    private static final String SHARED_PREFS_SETTINGS_NAME = "settings_prefs";

    private static final String SOUNDS_KEY = "sounds_key";
    private static final String BIOMETRIC_VERIFICATION_KEY = "bio_verification_key";

    static final String CONTACTS_KEY = "contacts_key";

    // Check if the app is launched for the first time
    protected static boolean isFirstLaunch(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    // Set the first launch flag
    public static void setFirstLaunch(Context context, boolean isFirstLaunch) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_FIRST_LAUNCH, isFirstLaunch);
        editor.apply();
    }

    // Retrieve the private key from encrypted shared preferences
    protected static PrivateKey getPrivateKeyFromEncryptedSharedPreferences(Context context) {
        String masterKeyAlias = null;
        try {
            masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        SharedPreferences sharedPreferences = null;
        try {
            sharedPreferences = EncryptedSharedPreferences.create(
                    KEY_STORAGE_CODE,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        return stringToPrivateKey(sharedPreferences.getString(PRIVATE_KEY_CODE, ""));
    }

    // Save the private key to encrypted shared preferences
    protected static void savePrivateKeyToEncryptedSharedPreferences(PrivateKey privateKey, Context context) {
        String masterKeyAlias = null;
        try {
            masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        SharedPreferences sharedPreferences = null;
        try {
            sharedPreferences = EncryptedSharedPreferences.create(
                    KEY_STORAGE_CODE,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.putString(PRIVATE_KEY_CODE, privateKeyToString(privateKey));
        editor.apply();
    }

    // Convert Private Key to Base64 String
    private static String privateKeyToString(PrivateKey privateKey) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Base64.getEncoder().encodeToString(privateKey.getEncoded());
        }
        return "";
    }

    // Convert Base64 String to Private Key
    private static PrivateKey stringToPrivateKey(String privateK) {
        try {
            byte[] privateBytes = new byte[0];
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                privateBytes = Base64.getDecoder().decode(privateK);
            }
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // Save the contact list to shared preferences
    public static void saveContactsToSharedPreferences(Context context, List<Contact> contactList) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(contactList);
        editor.putString(CONTACTS_KEY, json);
        editor.apply();
    }

    // Load the contact list from shared preferences
    public static List<Contact> loadContactsFromSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(CONTACTS_KEY, null);
        Type type = new TypeToken<ArrayList<Contact>>() {}.getType();
        List<Contact> contacts = gson.fromJson(json, type);
        return contacts == null ? new ArrayList<>() : contacts;
    }

    // Save the sound preference to shared preferences
    public static void saveSoundsPreference(Context context, boolean value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_SETTINGS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SOUNDS_KEY, value);
        editor.apply();
    }

    // Retrieve the sound preference from shared preferences
    public static boolean getSoundsPreference(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_SETTINGS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(SOUNDS_KEY, false);
    }

    // Save the biometric verification preference to shared preferences
    public static void saveBioVerificationPreference(Context context, boolean value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_SETTINGS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(BIOMETRIC_VERIFICATION_KEY, value);
        editor.apply();
    }

    // Retrieve the biometric verification preference from shared preferences
    public static boolean getBioVerificationPreference(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_SETTINGS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(BIOMETRIC_VERIFICATION_KEY, false);
    }
}
