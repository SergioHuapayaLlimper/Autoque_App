package com.example.autoque.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;

import com.google.android.material.textfield.TextInputLayout;

public final class Validaciones {

    private Validaciones() {}

    public static String texto(TextInputLayout campo) {
        return campo.getEditText() == null ? "" : campo.getEditText().getText().toString().trim();
    }

    public static boolean esEmailValido(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean esCelularValido(String telefono) {
        return soloDigitos(telefono).matches("9\\d{8}");
    }

    public static String soloDigitos(String s) {
        return s.replaceAll("\\D", "");
    }

    public static String ultimos9Digitos(String telefono) {
        if (telefono == null) return "";
        String d = soloDigitos(telefono);
        return d.length() > 9 ? d.substring(d.length() - 9) : d;
    }

    public static String formatearCelular(String telefono) {
        String d = soloDigitos(telefono);
        if (d.length() != 9) return telefono;
        return "+51 " + d.substring(0, 3) + " " + d.substring(3, 6) + " " + d.substring(6);
    }

    public static void limpiarErrorAlEscribir(TextInputLayout... campos) {
        for (TextInputLayout campo : campos) {
            if (campo.getEditText() == null) continue;
            campo.getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (campo.getError() != null) campo.setError(null);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    public static boolean requerido(TextInputLayout campo, String mensaje) {
        boolean ok = !texto(campo).isEmpty();
        campo.setError(ok ? null : mensaje);
        return ok;
    }
}
