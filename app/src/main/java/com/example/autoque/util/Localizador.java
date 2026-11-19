package com.example.autoque.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
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

    /** Seguimiento continuo de la ubicacion; llamar a detener() al salir de la pantalla. */
    public interface Seguimiento {
        void detener();
    }

    public static final String[] PERMISOS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};

    private static final long ESPERA_MAXIMA_MS = 10000;
    private static final String PREFS_PERMISOS = "autoque_permisos";
    private static final String KEY_UBICACION_PEDIDA = "ubicacion_pedida";
    private static final long INTERVALO_SEGUIMIENTO_MS = 5000;
    private static final float DISTANCIA_MINIMA_M = 10f;

    private Localizador() {}

    public static boolean tienePermiso(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Guarda que ya se mostro el dialogo de permiso de ubicacion al menos una vez. */
    public static void marcarPermisoPedido(Context context) {
        context.getSharedPreferences(PREFS_PERMISOS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_UBICACION_PEDIDA, true).apply();
    }

    /**
     * true si el usuario rechazo el permiso de forma permanente: Android ya no muestra el
     * dialogo y solo se puede activar desde los Ajustes de la app.
     */
    public static boolean permisoBloqueado(Activity activity) {
        if (tienePermiso(activity)) return false;
        boolean yaPedido = activity.getSharedPreferences(PREFS_PERMISOS, Context.MODE_PRIVATE)
                .getBoolean(KEY_UBICACION_PEDIDA, false);
        return yaPedido && !activity.shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION);
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
        String proveedor = proveedor(lm);
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

    /**
     * Escucha la ubicacion de forma continua (cada 5 s o al moverse 10 m) hasta llamar a
     * detener(). Devuelve null si no hay permiso o el GPS esta apagado.
     */
    @SuppressLint("MissingPermission")
    public static Seguimiento seguir(Context context, UbicacionCallback cb) {
        if (estado(context) != Estado.LISTO) return null;
        LocationManager lm = context.getSystemService(LocationManager.class);
        LocationRequest solicitud = new LocationRequest.Builder(INTERVALO_SEGUIMIENTO_MS)
                .setMinUpdateDistanceMeters(DISTANCIA_MINIMA_M)
                .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
                .build();
        LocationListener escucha = cb::onUbicacion;
        lm.requestLocationUpdates(proveedor(lm), solicitud,
                ContextCompat.getMainExecutor(context), escucha);
        return () -> lm.removeUpdates(escucha);
    }

    private static String proveedor(LocationManager lm) {
        if (lm.hasProvider(LocationManager.FUSED_PROVIDER)
                && lm.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
            return LocationManager.FUSED_PROVIDER;
        } else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        return LocationManager.NETWORK_PROVIDER;
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
