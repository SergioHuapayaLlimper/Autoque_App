package com.example.autoque.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.autoque.model.Catalogos;
import com.example.autoque.model.Taller;
import com.example.autoque.model.Usuario;
import com.example.autoque.util.PasswordUtils;

import java.util.List;

public class UsuarioDao {

    private static final String COLUMNAS_USUARIO = "id, rol, nombre, telefono, email, firebase_uid";

    private final AutoqueDbHelper helper;

    public UsuarioDao(Context context) {
        helper = AutoqueDbHelper.getInstance(context);
    }

    public boolean existeEmail(String email) {
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT 1 FROM usuario WHERE email = ? LIMIT 1", new String[]{email.trim()})) {
            return c.moveToFirst();
        }
    }

    public long registrarCliente(String nombre, String telefono, String email, String password) {
        return registrarCliente(nombre, telefono, email, password, null);
    }

    public long registrarCliente(String nombre, String telefono, String email, String password,
                                 String firebaseUid) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.insert("usuario", null,
                valoresUsuario(Usuario.ROL_CLIENTE, nombre, telefono, email, password, firebaseUid));
    }

    public long registrarTaller(String nombreTaller, String encargado, String telefono,
                                String distrito, String direccion, List<String> servicios,
                                String otrosServicios, String email, String password) {
        return registrarTaller(nombreTaller, encargado, telefono, distrito, direccion, servicios,
                otrosServicios, email, password, null);
    }

    public long registrarTaller(String nombreTaller, String encargado, String telefono,
                                String distrito, String direccion, List<String> servicios,
                                String otrosServicios, String email, String password,
                                String firebaseUid) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long id = db.insert("usuario", null, valoresUsuario(
                    Usuario.ROL_TALLER, encargado, telefono, email, password, firebaseUid));
            if (id == -1) return -1;

            ContentValues taller = new ContentValues();
            taller.put("usuario_id", id);
            taller.put("nombre_taller", nombreTaller.trim());
            taller.put("distrito", distrito);
            taller.put("direccion", direccion.trim());
            if (servicios.contains(Catalogos.SERVICIO_OTROS)) {
                taller.put("otros_servicios", otrosServicios.trim());
            }
            db.insertOrThrow("taller", null, taller);

            for (String servicio : servicios) {
                ContentValues fila = new ContentValues();
                fila.put("taller_id", id);
                fila.put("servicio", servicio);
                db.insertOrThrow("taller_servicio", null, fila);
            }
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public Usuario autenticar(String email, String password) {
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT " + COLUMNAS_USUARIO + ", password_hash, password_salt "
                        + "FROM usuario WHERE email = ?", new String[]{email.trim()})) {
            if (!c.moveToFirst()) return null;
            if (!PasswordUtils.verificar(password, c.getString(7), c.getString(6))) return null;
            return leerUsuario(c);
        }
    }

    public Usuario buscarPorEmail(String email) {
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT " + COLUMNAS_USUARIO + " FROM usuario WHERE email = ?",
                new String[]{email.trim()})) {
            return c.moveToFirst() ? leerUsuario(c) : null;
        }
    }

    public Usuario obtenerUsuario(long id) {
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT " + COLUMNAS_USUARIO + " FROM usuario WHERE id = ?",
                new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? leerUsuario(c) : null;
        }
    }

    public void vincularFirebase(long usuarioId, String firebaseUid) {
        ContentValues v = new ContentValues();
        v.put("firebase_uid", firebaseUid);
        helper.getWritableDatabase().update("usuario", v, "id = ?",
                new String[]{String.valueOf(usuarioId)});
    }

    public void actualizarPasswordLocal(long usuarioId, String password) {
        String sal = PasswordUtils.generarSal();
        ContentValues v = new ContentValues();
        v.put("password_salt", sal);
        v.put("password_hash", PasswordUtils.hash(password, sal));
        helper.getWritableDatabase().update("usuario", v, "id = ?",
                new String[]{String.valueOf(usuarioId)});
    }

    public Taller obtenerTaller(long usuarioId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Taller t;
        try (Cursor c = db.rawQuery(
                "SELECT t.usuario_id, t.nombre_taller, u.nombre, u.telefono, u.email, "
                        + "t.distrito, t.direccion, t.otros_servicios "
                        + "FROM taller t JOIN usuario u ON u.id = t.usuario_id "
                        + "WHERE t.usuario_id = ?", new String[]{String.valueOf(usuarioId)})) {
            if (!c.moveToFirst()) return null;
            t = new Taller();
            t.setUsuarioId(c.getLong(0));
            t.setNombreTaller(c.getString(1));
            t.setEncargado(c.getString(2));
            t.setTelefono(c.getString(3));
            t.setEmail(c.getString(4));
            t.setDistrito(c.getString(5));
            t.setDireccion(c.getString(6));
            t.setOtrosServicios(c.getString(7));
        }
        try (Cursor c = db.rawQuery("SELECT servicio FROM taller_servicio WHERE taller_id = ?",
                new String[]{String.valueOf(usuarioId)})) {
            while (c.moveToNext()) t.getServicios().add(c.getString(0));
        }
        return t;
    }

    private static ContentValues valoresUsuario(String rol, String nombre, String telefono,
                                                String email, String password, String firebaseUid) {
        String sal = PasswordUtils.generarSal();
        ContentValues v = new ContentValues();
        v.put("rol", rol);
        v.put("nombre", nombre.trim());
        v.put("telefono", telefono);
        v.put("email", email.trim().toLowerCase());
        v.put("password_salt", sal);
        v.put("password_hash", PasswordUtils.hash(password, sal));
        v.put("creado_en", System.currentTimeMillis());
        if (firebaseUid != null) v.put("firebase_uid", firebaseUid);
        return v;
    }

    private static Usuario leerUsuario(Cursor c) {
        return new Usuario(c.getLong(0), c.getString(1), c.getString(2), c.getString(3),
                c.getString(4), c.getString(5));
    }
}
