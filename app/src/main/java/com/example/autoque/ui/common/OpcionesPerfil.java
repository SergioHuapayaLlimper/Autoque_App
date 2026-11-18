package com.example.autoque.ui.common;

import android.view.View;

import com.example.autoque.databinding.ItemOpcionPerfilBinding;

public final class OpcionesPerfil {

    private OpcionesPerfil() {}

    public static void configurar(ItemOpcionPerfilBinding fila, int titulo, Runnable accion) {
        fila.tvTitulo.setText(titulo);
        fila.tvValor.setVisibility(View.GONE);
        fila.getRoot().setOnClickListener(v -> accion.run());
    }

    public static void valor(ItemOpcionPerfilBinding fila, CharSequence valor) {
        fila.tvValor.setText(valor);
        fila.tvValor.setVisibility(valor == null || valor.length() == 0 ? View.GONE : View.VISIBLE);
    }
}
