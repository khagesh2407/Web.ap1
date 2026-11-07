package com.zkelite.compucareservice;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecureTokenStore {
    private static final String ALIAS = "compucare_native_access_token";
    private static final String PREFS = "secure_session";
    private SecureTokenStore() {}

    static void save(Context context, String token) throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        SecretKey key;
        if (store.containsAlias(ALIAS)) {
            key = ((javax.crypto.SecretKey) store.getKey(ALIAS, null));
        } else {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build());
            key = generator.generateKey();
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ByteBuffer packed = ByteBuffer.allocate(4 + cipher.getIV().length + encrypted.length);
        packed.putInt(cipher.getIV().length).put(cipher.getIV()).put(encrypted);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("token", Base64.encodeToString(packed.array(), Base64.NO_WRAP)).apply();
    }

    static String read(Context context) {
        try {
            String stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("token", null);
            if (stored == null) return null;
            ByteBuffer packed = ByteBuffer.wrap(Base64.decode(stored, Base64.NO_WRAP));
            int ivLength = packed.getInt();
            byte[] iv = new byte[ivLength];
            packed.get(iv);
            byte[] encrypted = new byte[packed.remaining()];
            packed.get(encrypted);
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            store.load(null);
            SecretKey key = (SecretKey) store.getKey(ALIAS, null);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ignored) { return null; }
    }

    static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
        try {
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            store.load(null);
            if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS);
        } catch (Exception ignored) { }
    }
}
