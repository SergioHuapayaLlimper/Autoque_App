package com.example.autoque.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.autoque.model.Taller;
import com.example.autoque.model.Usuario;

public class PerfilLocalDao {

    private final AutoqueDbHelper helper;

    public PerfilLocalDao(Context context) {
        helper = AutoqueDbHelper.getInstance(context);
    }

    public void guardar(Usuario usuario, Taller taller) {
        ContentValues v = new ContentValues();
        v.put("uid", usuario.getUid());
        v.put("rol", usuario.getRol());
        v.put("nombre", usuario.getNombre());
        v.put("telefono", usuario.getTelefono());
        v.put("email", usuario.getEmail());
        if (taller != null) {
            v.put("nombre_taller", taller.getNombreTaller());
            v.put("distrito", taller.getDistrito());
        }
        v.put("actualizado_en", System.currentTimeMillis());
        helper.getWritableDatabase().insertWithOnConflict(
                "perfil_local", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Usuario obtener(String uid) {
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT uid, rol, nombre, telefono, email FROM perfil_local WHERE uid = ?",
                new String[]{uid})) {
            if (!c.moveToFirst()) return null;
            Usuario u = new Usuario(c.getString(0), c.getString(1), c.getString(2),
                    c.getString(3), c.getString(4));
            return u;
        }
    }

    public String distritoTaller(String uid) {
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT distrito FROM perfil_local WHERE uid = ?", new String[]{uid})) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }
}
