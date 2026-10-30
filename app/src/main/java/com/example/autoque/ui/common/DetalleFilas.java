package com.example.autoque.ui.common;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.example.autoque.databinding.ItemDetalleFilaBinding;
import com.google.android.material.divider.MaterialDivider;

public final class DetalleFilas {

    private final LinearLayout contenedor;
    private final LayoutInflater inflater;

    public DetalleFilas(LinearLayout contenedor) {
        this.contenedor = contenedor;
        this.inflater = LayoutInflater.from(contenedor.getContext());
        contenedor.removeAllViews();
    }

    public DetalleFilas fila(int etiquetaRes, CharSequence valor) {
        return fila(etiquetaRes, valor, false);
    }

    public DetalleFilas fila(int etiquetaRes, CharSequence valor, boolean destacado) {
        if (valor == null || valor.length() == 0) return this;
        ItemDetalleFilaBinding fila = ItemDetalleFilaBinding.inflate(inflater, contenedor, false);
        fila.tvEtiqueta.setText(etiquetaRes);
        fila.tvValor.setText(valor);
        if (destacado) {
            fila.tvEtiqueta.setTextColor(fila.tvValor.getCurrentTextColor());
            fila.tvEtiqueta.setTypeface(Typeface.DEFAULT_BOLD);
            fila.tvValor.setTypeface(Typeface.DEFAULT_BOLD);
            fila.tvValor.setTextSize(20);
        }
        contenedor.addView(fila.getRoot());
        return this;
    }

    public DetalleFilas separador() {
        MaterialDivider divider = new MaterialDivider(contenedor.getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int m = (int) (8 * contenedor.getResources().getDisplayMetrics().density);
        lp.setMargins(0, m, 0, m);
        contenedor.addView(divider, lp);
        return this;
    }
}
