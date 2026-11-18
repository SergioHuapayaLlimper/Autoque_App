package com.example.autoque.data;

import android.util.Log;

import com.example.autoque.R;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.firestore.FirebaseFirestoreException;

final class Errores {

    private static final String TAG = "Autoque";

    private Errores() {}

    static int mensaje(Exception e) {
        Log.e(TAG, "Error de Firebase", e);
        if (e instanceof FirebaseNetworkException) return R.string.sin_conexion;
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) e).getCode();
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE) return R.string.sin_conexion;
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return R.string.sin_permiso_datos;
            if (code == FirebaseFirestoreException.Code.NOT_FOUND) return R.string.firestore_no_creado;
        }
        return R.string.error_firebase;
    }
}
