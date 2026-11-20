package com.example.autoque.ui.common;

import android.content.Context;
import android.content.Intent;

import com.example.autoque.R;
import com.example.autoque.model.Taller;
import com.example.autoque.ui.cliente.PerfilTallerActivity;
import com.example.autoque.util.Geo;
import com.example.autoque.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

/** Convierte los talleres de un distrito en pines amarillos para el MiniMapa del cliente. */
public final class TalleresEnMapa {

    private TalleresEnMapa() {}

    /**
     * Arma un pin por cada taller con ubicacion. La burbuja muestra el promedio de estrellas
     * y la distancia al cliente (si se conoce su ubicacion).
     */
    public static List<MiniMapa.Lugar> lugares(Context context, List<Taller> talleres,
                                               Double latCliente, Double lonCliente) {
        List<MiniMapa.Lugar> lugares = new ArrayList<>();
        for (Taller t : talleres) {
            if (!t.tieneUbicacion() || t.getUid() == null) continue;
            String detalle = t.getTotalResenas() > 0
                    ? UiUtils.estrellas(t.promedio()) + " " + UiUtils.promedio(t.promedio())
                    : context.getString(R.string.sin_calificaciones);
            Double km = Geo.distanciaKm(latCliente, lonCliente, t.getLatitud(), t.getLongitud());
            if (km != null) detalle += " · " + Geo.formatearKm(km);
            lugares.add(new MiniMapa.Lugar(t.getUid(), t.getNombreTaller(), detalle,
                    t.getLatitud(), t.getLongitud()));
        }
        return lugares;
    }

    public static void abrirPerfil(Context context, String tallerUid) {
        Intent intent = new Intent(context, PerfilTallerActivity.class);
        intent.putExtra(PerfilTallerActivity.EXTRA_TALLER_UID, tallerUid);
        context.startActivity(intent);
    }
}
