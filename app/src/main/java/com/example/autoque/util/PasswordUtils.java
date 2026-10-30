package com.example.autoque.util;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtils {

    private static final int ITERACIONES = 20000;
    private static final int LONGITUD_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtils() {}

    public static String generarSal() {
        byte[] sal = new byte[16];
        RANDOM.nextBytes(sal);
        return Base64.encodeToString(sal, Base64.NO_WRAP);
    }

    public static String hash(String password, String salBase64) {
        try {
            byte[] sal = Base64.decode(salBase64, Base64.NO_WRAP);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), sal, ITERACIONES, LONGITUD_BITS);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        }
    }

    public static boolean verificar(String password, String salBase64, String hashEsperado) {
        byte[] calculado = hash(password, salBase64).getBytes();
        return MessageDigest.isEqual(calculado, hashEsperado.getBytes());
    }

    public static String generarClaveAleatoria() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.NO_WRAP | Base64.URL_SAFE);
    }
}
