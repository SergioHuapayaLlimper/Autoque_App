package com.example.autoque.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS = "autoque_session";
    private static final String KEY_USUARIO_ID = "usuario_id";
    private static final String KEY_ROL = "rol";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void iniciar(long usuarioId, String rol) {
        prefs.edit().putLong(KEY_USUARIO_ID, usuarioId).putString(KEY_ROL, rol).apply();
    }

    public boolean haySesion() {
        return prefs.getLong(KEY_USUARIO_ID, -1) > 0;
    }

    public long getUsuarioId() {
        return prefs.getLong(KEY_USUARIO_ID, -1);
    }

    public String getRol() {
        return prefs.getString(KEY_ROL, null);
    }

    public void cerrar() {
        prefs.edit().clear().apply();
    }
}
