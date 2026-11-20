package com.example.autoque.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.autoque.model.Catalogos;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class Ubicaciones {

    public interface Coordenadas {
        void onResultado(Double latitud, Double longitud);
    }

    /** Resultado de validar una direccion escrita por el usuario. */
    public static class Validacion {
        public enum Estado { VALIDA, NO_ENCONTRADA, OTRO_DISTRITO, SIN_SERVICIO }

        public final Estado estado;
        public final Double latitud;
        public final Double longitud;
        /** Distrito real de la direccion (solo cuando el estado es OTRO_DISTRITO). */
        public final String distritoEncontrado;

        Validacion(Estado estado, Double latitud, Double longitud, String distritoEncontrado) {
            this.estado = estado;
            this.latitud = latitud;
            this.longitud = longitud;
            this.distritoEncontrado = distritoEncontrado;
        }
    }

    public interface AlValidar {
        void onResultado(Validacion v);
    }

    private static final List<String> PALABRAS_IGNORADAS = Arrays.asList(
            "av", "avenida", "calle", "jr", "jiron", "psje", "pasaje", "pje", "mz", "lt",
            "manzana", "lote", "nro", "numero", "urb", "de", "del", "la", "las", "los", "el");

    private Ubicaciones() {}

    /**
     * Busca la direccion en Google y comprueba que: 1) exista a nivel de calle (no solo el
     * distrito), 2) se parezca a lo escrito y 3) pertenezca al distrito elegido. Si el
     * servicio no responde (sin internet), devuelve SIN_SERVICIO para no bloquear al usuario.
     */
    public static void validar(Context context, String direccion, String distrito, AlValidar cb) {
        if (!Geocoder.isPresent()) {
            cb.onResultado(new Validacion(Validacion.Estado.SIN_SERVICIO, null, null, null));
            return;
        }
        Geocoder geocoder = new Geocoder(context, Locale.forLanguageTag("es-PE"));
        String consulta = direccion + ", " + distrito + ", Lima, Perú";
        geocoder.getFromLocationName(consulta, 5, new Geocoder.GeocodeListener() {
            @Override
            public void onGeocode(@NonNull List<Address> lista) {
                Validacion v = evaluar(lista, direccion, distrito);
                ContextCompat.getMainExecutor(context).execute(() -> cb.onResultado(v));
            }

            @Override
            public void onError(String mensaje) {
                ContextCompat.getMainExecutor(context).execute(() -> cb.onResultado(
                        new Validacion(Validacion.Estado.SIN_SERVICIO, null, null, null)));
            }
        });
    }

    private static Validacion evaluar(List<Address> lista, String direccion, String distrito) {
        String otroDistrito = null;
        for (Address a : lista) {
            // Si Google solo encontro el distrito (sin calle), la direccion es inventada o vaga
            if (a.getThoroughfare() == null || !coincideCalle(direccion, a)) continue;
            String encontrado = distritoDe(a);
            if (encontrado == null || encontrado.equals(distrito)) {
                return new Validacion(Validacion.Estado.VALIDA, a.getLatitude(), a.getLongitude(), null);
            }
            if (otroDistrito == null) otroDistrito = encontrado;
        }
        if (otroDistrito != null) {
            return new Validacion(Validacion.Estado.OTRO_DISTRITO, null, null, otroDistrito);
        }
        return new Validacion(Validacion.Estado.NO_ENCONTRADA, null, null, null);
    }

    private static String distritoDe(Address a) {
        String d = Catalogos.buscarDistrito(a.getSubLocality());
        // "Lima" como ciudad no indica el distrito: se ignora para no confundirlo con el Cercado
        if (d == null && !"lima".equals(normalizar(a.getLocality()))) {
            d = Catalogos.buscarDistrito(a.getLocality());
        }
        if (d == null && !"lima".equals(normalizar(a.getSubAdminArea()))) {
            d = Catalogos.buscarDistrito(a.getSubAdminArea());
        }
        return d;
    }

    /** La calle encontrada debe contener al menos una palabra importante de lo escrito. */
    private static boolean coincideCalle(String escrito, Address a) {
        List<String> palabras = new ArrayList<>();
        for (String p : normalizar(escrito).split("[^a-z0-9]+")) {
            if (p.length() >= 3 && !PALABRAS_IGNORADAS.contains(p) && !p.matches("\\d+")) {
                palabras.add(p);
            }
        }
        if (palabras.isEmpty()) return true;
        String encontrada = normalizar(a.getThoroughfare() + " "
                + (a.getFeatureName() != null ? a.getFeatureName() : "") + " "
                + (a.getAddressLine(0) != null ? a.getAddressLine(0) : ""));
        for (String p : palabras) {
            if (encontrada.contains(p)) return true;
        }
        return false;
    }

    public static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

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
