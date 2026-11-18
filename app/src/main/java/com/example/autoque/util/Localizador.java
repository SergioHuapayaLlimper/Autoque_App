package com.example.autoque.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.autoque.model.Catalogos;

import java.util.List;
import java.util.Locale;

public final class Localizador {

    public enum Estado { SIN_PERMISO, GPS_APAGADO, LISTO }

    public interface UbicacionCallback {
        void onUbicacion(Location location);
    }

    public interface DireccionCallback {
        void onDireccion(String direccion, String distrito);
    }

    public static final String[] PERMISOS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};

    private static final long ESPERA_MAXIMA_MS = 10000;

    private Localizador() {}

    public static boolean tienePermiso(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static Estado estado(Context context) {
        if (!tienePermiso(context)) return Estado.SIN_PERMISO;
        LocationManager lm = context.getSystemService(LocationManager.class);
        return lm != null && lm.isLocationEnabled() ? Estado.LISTO : Estado.GPS_APAGADO;
    }

    @SuppressLint("MissingPermission")
    public static CancellationSignal obtener(Context context, UbicacionCallback cb) {
        CancellationSignal cancelar = new CancellationSignal();
        if (estado(context) != Estado.LISTO) {
            cb.onUbicacion(null);
            return cancelar;
        }
        LocationManager lm = context.getSystemService(LocationManager.class);
        String proveedor;
        if (lm.hasProvider(LocationManager.FUSED_PROVIDER)
                && lm.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
            proveedor = LocationManager.FUSED_PROVIDER;
        } else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            proveedor = LocationManager.GPS_PROVIDER;
        } else {
            proveedor = LocationManager.NETWORK_PROVIDER;
        }
        boolean[] entregado = {false};
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable porTiempo = () -> {
            if (entregado[0] || cancelar.isCanceled()) return;
            entregado[0] = true;
            cancelar.cancel();
            cb.onUbicacion(ultimaConocida(lm));
        };
        handler.postDelayed(porTiempo, ESPERA_MAXIMA_MS);
        lm.getCurrentLocation(proveedor, cancelar, ContextCompat.getMainExecutor(context), location -> {
            if (entregado[0]) return;
            entregado[0] = true;
            handler.removeCallbacks(porTiempo);
            cb.onUbicacion(location != null ? location : ultimaConocida(lm));
        });
        return cancelar;
    }

    @SuppressLint("MissingPermission")
    private static Location ultimaConocida(LocationManager lm) {
        Location mejor = null;
        for (String p : lm.getProviders(true)) {
            Location l = lm.getLastKnownLocation(p);
            if (l != null && (mejor == null || l.getTime() > mejor.getTime())) mejor = l;
        }
        return mejor;
    }

    public static void direccion(Context context, double lat, double lon, DireccionCallback cb) {
        if (!Geocoder.isPresent()) return;
        Geocoder geocoder = new Geocoder(context, Locale.forLanguageTag("es-PE"));
        geocoder.getFromLocation(lat, lon, 1, new Geocoder.GeocodeListener() {
            @Override
            public void onGeocode(@NonNull List<Address> lista) {
                if (lista.isEmpty()) return;
                Address a = lista.get(0);
                String calle = a.getThoroughfare();
                String direccion;
                if (calle != null) {
                    direccion = a.getSubThoroughfare() != null ? calle + " " + a.getSubThoroughfare() : calle;
                } else if (a.getAddressLine(0) != null) {
                    direccion = a.getAddressLine(0).split(",")[0];
                } else {
                    direccion = null;
                }
                String distrito = Catalogos.buscarDistrito(a.getSubLocality());
                if (distrito == null) distrito = Catalogos.buscarDistrito(a.getLocality());
                if (distrito == null) distrito = Catalogos.buscarDistrito(a.getSubAdminArea());
                String d = direccion;
                String dist = distrito;
                ContextCompat.getMainExecutor(context).execute(() -> cb.onDireccion(d, dist));
            }

            @Override
            public void onError(String mensaje) {
            }
        });
    }
}
