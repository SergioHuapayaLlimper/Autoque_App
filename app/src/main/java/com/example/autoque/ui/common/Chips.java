package com.example.autoque.ui.common;

import android.content.Context;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.autoque.R;
import com.google.android.material.chip.Chip;

public final class Chips {

    private Chips() {}

    public static Chip crear(Context context, String texto, Object tag) {
        float densidad = context.getResources().getDisplayMetrics().density;
        Chip chip = new Chip(context, null, com.google.android.material.R.attr.chipStyle);
        chip.setId(View.generateViewId());
        chip.setTag(tag);
        chip.setText(texto);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.chip_bg);
        chip.setTextColor(ContextCompat.getColorStateList(context, R.color.chip_text));
        chip.setChipStrokeColorResource(R.color.outline);
        chip.setChipStrokeWidth(densidad);
        chip.setCheckedIconResource(R.drawable.ic_check);
        chip.setCheckedIconTintResource(R.color.autoque_yellow);
        chip.setChipMinHeight(44 * densidad);
        chip.setShapeAppearanceModel(chip.getShapeAppearanceModel().withCornerSize(22 * densidad));
        return chip;
    }

    public static Chip etiqueta(Context context, String texto) {
        Chip chip = crear(context, texto, null);
        chip.setCheckable(false);
        chip.setClickable(false);
        chip.setChipMinHeight(36 * context.getResources().getDisplayMetrics().density);
        return chip;
    }
}
