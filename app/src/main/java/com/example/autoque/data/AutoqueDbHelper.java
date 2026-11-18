package com.example.autoque.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AutoqueDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "AutoqueDb";
    public static final String DB_NAME = "autoque.db";
    public static final int DB_VERSION = 3;

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
    public void onCreate(SQLiteDatabase db) {
        crearPerfilLocal(db);
    }

    private static void crearPerfilLocal(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS perfil_local ("
                + "uid TEXT PRIMARY KEY, "
                + "rol TEXT NOT NULL CHECK (rol IN ('CLIENTE', 'TALLER')), "
                + "nombre TEXT NOT NULL, "
                + "telefono TEXT, "
                + "email TEXT NOT NULL, "
                + "nombre_taller TEXT, "
                + "distrito TEXT, "
                + "actualizado_en INTEGER NOT NULL)");
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
        if (oldVersion < 3) {
            crearPerfilLocal(db);
        }
    }

    static boolean existeTabla(SQLiteDatabase db, String tabla) {
        try (Cursor c = db.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
                new String[]{tabla})) {
            return c.moveToFirst();
        }
    }
}
