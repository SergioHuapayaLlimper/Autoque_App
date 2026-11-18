package com.example.autoque.ui.common;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.autoque.R;
import com.example.autoque.databinding.ActivityAyudaBinding;
import com.example.autoque.ui.BaseActivity;
import com.google.android.material.card.MaterialCardView;

public class AyudaActivity extends BaseActivity {

    private static final int[][] PREGUNTAS = {
            {R.string.ayuda_p1, R.string.ayuda_r1},
            {R.string.ayuda_p2, R.string.ayuda_r2},
            {R.string.ayuda_p3, R.string.ayuda_r3},
            {R.string.ayuda_p4, R.string.ayuda_r4},
            {R.string.ayuda_p5, R.string.ayuda_r5}};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAyudaBinding binding = ActivityAyudaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);

        int margen = (int) (12 * getResources().getDisplayMetrics().density);
        int relleno = (int) (16 * getResources().getDisplayMetrics().density);
        for (int[] p : PREGUNTAS) {
            MaterialCardView card = new MaterialCardView(this, null,
                    com.google.android.material.R.attr.materialCardViewOutlinedStyle);
            card.setStrokeColor(ContextCompat.getColor(this, R.color.outline));
            card.setRadius(16 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = margen;

            LinearLayout contenido = new LinearLayout(this);
            contenido.setOrientation(LinearLayout.VERTICAL);
            contenido.setPadding(relleno, relleno, relleno, relleno);

            TextView pregunta = new TextView(this);
            pregunta.setText(p[0]);
            pregunta.setTextAppearance(R.style.TextAppearance_Autoque_CardTitle);
            TextView respuesta = new TextView(this);
            respuesta.setText(p[1]);
            respuesta.setTextAppearance(R.style.TextAppearance_Autoque_Body);
            respuesta.setPadding(0, margen / 2, 0, 0);

            contenido.addView(pregunta);
            contenido.addView(respuesta);
            card.addView(contenido);
            binding.contenedor.addView(card, lp);
        }
    }
}
