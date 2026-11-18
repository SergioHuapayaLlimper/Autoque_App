package com.example.autoque.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;

public final class Ubicaciones {

    public interface Coordenadas {
        void onResultado(Double latitud, Double longitud);
    }

    private Ubicaciones() {}

    public static void geocodificar(Context context, String direccion, String distrito, Coordenadas cb) {
        if (!Geocoder.isPresent()) {
            cb.onResultado(null, null);
            return;
        }
        Geocoder geocoder = new Geocoder(context, Locale.forLanguageTag("es-PE"));
        String consulta = direccion + ", " + distrito + ", Lima, Perú";
        geocoder.getFromLocationName(consulta, 1, new Geocoder.GeocodeListener() {
            @Override
            public void onGeocode(@NonNull List<Address> lista) {
                Double lat = lista.isEmpty() ? null : lista.get(0).getLatitude();
                Double lon = lista.isEmpty() ? null : lista.get(0).getLongitude();
                ContextCompat.getMainExecutor(context).execute(() -> cb.onResultado(lat, lon));
            }

            @Override
            public void onError(String mensaje) {
                ContextCompat.getMainExecutor(context).execute(() -> cb.onResultado(null, null));
            }
        });
    }
}
