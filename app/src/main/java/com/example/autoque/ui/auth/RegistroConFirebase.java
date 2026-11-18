package com.example.autoque.ui.auth;

import android.app.Activity;
import android.widget.Toast;

import com.example.autoque.R;
import com.example.autoque.data.AuthRepository;
import com.example.autoque.model.Taller;
import com.example.autoque.model.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

final class RegistroConFirebase {

    interface AlTerminar {
        void conUsuario(Usuario usuario);
    }

    private RegistroConFirebase() {}

    static void registrar(Activity activity, MaterialButton boton, int textoBoton,
                          TextInputLayout tilEmail, TextInputLayout tilPassword,
                          Usuario usuario, Taller taller, String password, AlTerminar alTerminar) {
        boton.setEnabled(false);
        boton.setText(R.string.procesando);
        new AuthRepository(activity).registrar(usuario, taller, password, new AuthRepository.Callback<>() {
            @Override
            public void onExito(Usuario registrado) {
                if (activity.isFinishing()) return;
                alTerminar.conUsuario(registrado);
            }

            @Override
            public void onError(int mensaje, AuthRepository.Campo campo) {
                if (activity.isFinishing()) return;
                boton.setEnabled(true);
                boton.setText(textoBoton);
                if (campo == AuthRepository.Campo.EMAIL) {
                    tilEmail.setError(activity.getString(mensaje));
                } else if (campo == AuthRepository.Campo.PASSWORD) {
                    tilPassword.setError(activity.getString(mensaje));
                } else {
                    Snackbar.make(boton, mensaje, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    static void bienvenida(Activity activity, int mensaje) {
        Toast.makeText(activity, mensaje, Toast.LENGTH_LONG).show();
    }
}
