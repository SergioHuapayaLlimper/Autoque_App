package com.example.autoque.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.example.autoque.R;
import com.example.autoque.model.Solicitud;

import java.util.Locale;

public final class Intents {

    private Intents() {}

    public static void llamar(Context context, String telefono) {
        Uri uri = Uri.parse("tel:" + telefono.replace(" ", ""));
        context.startActivity(new Intent(Intent.ACTION_DIAL, uri));
    }

    public static void abrirMapa(Context context, Solicitud s) {
        String consulta;
        if (s.tieneUbicacion()) {
            consulta = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                    s.getLatitud(), s.getLongitud(), s.getLatitud(), s.getLongitud(), Uri.encode(s.getNombreContacto()));
        } else {
            consulta = "geo:0,0?q=" + Uri.encode(s.getDireccion() + ", " + s.getDistrito() + ", Lima, Perú");
        }
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(consulta)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.no_hay_app_mapas, Toast.LENGTH_SHORT).show();
        }
    }
}
