package com.example.autoque.ui.auth;

import android.app.Activity;
import android.widget.Toast;

import com.example.autoque.R;
import com.example.autoque.data.AuthRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

final class RegistroConFirebase {

    interface GuardarPerfil {
        long guardar(String firebaseUid);
    }

    interface AlTerminar {
        void conId(long usuarioId);
    }

    private RegistroConFirebase() {}

    static void registrar(Activity activity, MaterialButton boton, int textoBoton,
                          TextInputLayout tilEmail, TextInputLayout tilPassword,
                          String email, String password,
                          GuardarPerfil guardarPerfil, AlTerminar alTerminar) {
        AuthRepository auth = new AuthRepository(activity);
        boton.setEnabled(false);
        boton.setText(R.string.procesando);

        auth.crearCredenciales(email, password, new AuthRepository.Callback<>() {
            @Override
            public void onExito(String firebaseUid) {
                if (activity.isFinishing()) return;
                long id = guardarPerfil.guardar(firebaseUid);
                if (id == -1) {
                    auth.deshacerCredenciales();
                    restaurar();
                    tilEmail.setError(activity.getString(R.string.correo_registrado));
                    return;
                }
                alTerminar.conId(id);
            }

            @Override
            public void onError(int mensaje, AuthRepository.Campo campo) {
                if (activity.isFinishing()) return;
                restaurar();
                if (campo == AuthRepository.Campo.EMAIL) {
                    tilEmail.setError(activity.getString(mensaje));
                } else if (campo == AuthRepository.Campo.PASSWORD) {
                    tilPassword.setError(activity.getString(mensaje));
                } else {
                    Snackbar.make(boton, mensaje, Snackbar.LENGTH_LONG).show();
                }
            }

            private void restaurar() {
                boton.setEnabled(true);
                boton.setText(textoBoton);
            }
        });
    }

    static void bienvenida(Activity activity, int mensaje) {
        Toast.makeText(activity, mensaje, Toast.LENGTH_LONG).show();
    }
}
