package com.example.autoque.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.autoque.model.Usuario;

public class SessionManager {

    private static final String PREFS = "autoque_sesion";
    private static final String KEY_UID = "uid";
    private static final String KEY_ROL = "rol";
    private static final String KEY_NOMBRE = "nombre";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void iniciar(Usuario usuario) {
        prefs.edit()
                .putString(KEY_UID, usuario.getUid())
                .putString(KEY_ROL, usuario.getRol())
                .putString(KEY_NOMBRE, usuario.getNombre())
                .apply();
    }

    public void actualizarNombre(String nombre) {
        prefs.edit().putString(KEY_NOMBRE, nombre).apply();
    }

    public boolean haySesion() {
        return prefs.getString(KEY_UID, null) != null;
    }

    public String getUid() {
        return prefs.getString(KEY_UID, null);
    }

    public String getRol() {
        return prefs.getString(KEY_ROL, null);
    }

    public String getNombre() {
        return prefs.getString(KEY_NOMBRE, "");
    }

    public void cerrar() {
        prefs.edit().clear().apply();
    }
}
