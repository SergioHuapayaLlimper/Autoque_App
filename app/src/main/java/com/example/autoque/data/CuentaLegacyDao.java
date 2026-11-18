package com.example.autoque.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.autoque.model.Taller;
import com.example.autoque.model.Usuario;
import com.example.autoque.util.PasswordUtils;

public class CuentaLegacyDao {

    private final AutoqueDbHelper helper;

    public CuentaLegacyDao(Context context) {
        helper = AutoqueDbHelper.getInstance(context);
    }

    public static class CuentaLegacy {
        private final long id;
        private final Usuario usuario;
        private final String hash;
        private final String sal;

        CuentaLegacy(long id, Usuario usuario, String hash, String sal) {
            this.id = id;
            this.usuario = usuario;
            this.hash = hash;
            this.sal = sal;
        }

        public long getId() {
            return id;
        }

        public Usuario getUsuario() {
            return usuario;
        }

        public boolean passwordCorrecta(String password) {
            return PasswordUtils.verificar(password, sal, hash);
        }
    }

    public CuentaLegacy buscarPorEmail(String email) {
        SQLiteDatabase db = helper.getReadableDatabase();
        if (!AutoqueDbHelper.existeTabla(db, "usuario")) return null;
        try (Cursor c = db.rawQuery(
                "SELECT id, rol, nombre, telefono, email, password_hash, password_salt "
                        + "FROM usuario WHERE email = ? COLLATE NOCASE",
                new String[]{email.trim()})) {
            if (!c.moveToFirst()) return null;
            Usuario u = new Usuario(null, c.getString(1), c.getString(2), c.getString(3),
                    c.getString(4));
            return new CuentaLegacy(c.getLong(0), u, c.getString(5), c.getString(6));
        }
    }

    public Taller obtenerTaller(long usuarioId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        if (!AutoqueDbHelper.existeTabla(db, "taller")) return null;
        Taller t;
        try (Cursor c = db.rawQuery(
                "SELECT t.nombre_taller, u.nombre, u.telefono, u.email, t.distrito, t.direccion, "
                        + "t.otros_servicios FROM taller t JOIN usuario u ON u.id = t.usuario_id "
                        + "WHERE t.usuario_id = ?", new String[]{String.valueOf(usuarioId)})) {
            if (!c.moveToFirst()) return null;
            t = new Taller();
            t.setNombreTaller(c.getString(0));
            t.setEncargado(c.getString(1));
            t.setTelefono(c.getString(2));
            t.setEmail(c.getString(3));
            t.setDistrito(c.getString(4));
            t.setDireccion(c.getString(5));
            t.setOtrosServicios(c.getString(6));
        }
        try (Cursor c = db.rawQuery("SELECT servicio FROM taller_servicio WHERE taller_id = ?",
                new String[]{String.valueOf(usuarioId)})) {
            while (c.moveToNext()) t.getServicios().add(c.getString(0));
        }
        return t;
    }
}
