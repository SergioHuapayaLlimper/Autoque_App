package com.example.autoque.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AutoqueDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "AutoqueDb";
    public static final String DB_NAME = "autoque.db";
    public static final int DB_VERSION = 2;

    private static AutoqueDbHelper instance;

    public static synchronized AutoqueDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new AutoqueDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private AutoqueDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE usuario ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "rol TEXT NOT NULL CHECK (rol IN ('CLIENTE', 'TALLER')), "
                + "nombre TEXT NOT NULL, "
                + "telefono TEXT NOT NULL, "
                + "email TEXT NOT NULL UNIQUE COLLATE NOCASE, "
                + "password_hash TEXT NOT NULL, "
                + "password_salt TEXT NOT NULL, "
                + "creado_en INTEGER NOT NULL, "
                + "firebase_uid TEXT UNIQUE)");

        db.execSQL("CREATE TABLE taller ("
                + "usuario_id INTEGER PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE, "
                + "nombre_taller TEXT NOT NULL, "
                + "distrito TEXT NOT NULL, "
                + "direccion TEXT NOT NULL, "
                + "otros_servicios TEXT)");
        db.execSQL("CREATE INDEX idx_taller_distrito ON taller(distrito)");

        db.execSQL("CREATE TABLE taller_servicio ("
                + "taller_id INTEGER NOT NULL REFERENCES taller(usuario_id) ON DELETE CASCADE, "
                + "servicio TEXT NOT NULL, "
                + "PRIMARY KEY (taller_id, servicio))");

        db.execSQL("CREATE TABLE solicitud ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "cliente_id INTEGER NOT NULL REFERENCES usuario(id), "
                + "nombre_contacto TEXT NOT NULL, "
                + "telefono_contacto TEXT NOT NULL, "
                + "direccion TEXT NOT NULL, "
                + "distrito TEXT NOT NULL, "
                + "latitud REAL, "
                + "longitud REAL, "
                + "tipo_incidente TEXT NOT NULL, "
                + "descripcion TEXT, "
                + "costo_min INTEGER NOT NULL, "
                + "costo_max INTEGER NOT NULL, "
                + "estado TEXT NOT NULL DEFAULT 'PENDIENTE' "
                + "  CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'FINALIZADA', 'CANCELADA')), "
                + "taller_id INTEGER REFERENCES taller(usuario_id), "
                + "creado_en INTEGER NOT NULL, "
                + "aceptado_en INTEGER, "
                + "finalizado_en INTEGER)");
        db.execSQL("CREATE INDEX idx_solicitud_distrito_estado ON solicitud(distrito, estado)");
        db.execSQL("CREATE INDEX idx_solicitud_taller ON solicitud(taller_id, estado)");
        db.execSQL("CREATE INDEX idx_solicitud_cliente ON solicitud(cliente_id)");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        try (Cursor c = db.rawQuery("SELECT sqlite_version()", null)) {
            if (c.moveToFirst()) {
                Log.i(TAG, "SQLite version: " + c.getString(0));
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE usuario ADD COLUMN firebase_uid TEXT");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_usuario_firebase_uid "
                    + "ON usuario(firebase_uid)");
            db.execSQL("DROP TABLE IF EXISTS codigo_recuperacion");
        }
    }
}
