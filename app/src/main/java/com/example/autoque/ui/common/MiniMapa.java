package com.example.autoque.ui.common;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.example.autoque.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapa pequeno de Google Maps para mostrar la ubicacion del cliente y del taller.
 * Se encarga del ciclo de vida del MapView: la pantalla solo llama a onResume,
 * onPause y onDestroy.
 */
public class MiniMapa {

    /** Punto secundario del mapa (por ejemplo, un taller) con su burbuja de informacion. */
    public static class Lugar {
        final String id;
        final String titulo;
        final String detalle;
        final double latitud;
        final double longitud;

        public Lugar(String id, String titulo, String detalle, double latitud, double longitud) {
            this.id = id;
            this.titulo = titulo;
            this.detalle = detalle;
            this.latitud = latitud;
            this.longitud = longitud;
        }
    }

    public interface AlTocarLugar {
        void onTocar(String id);
    }

    private static final LatLng LIMA = new LatLng(-12.0464, -77.0428);
    private static final float ZOOM_CIUDAD = 12f;
    private static final float ZOOM_PUNTO = 16f;
    private static final int PADDING_DP = 48;

    private final MapView mapa;
    private final List<LatLng> puntos = new ArrayList<>();
    private final List<Runnable> pendientes = new ArrayList<>();
    private final List<Marker> marcadoresLugares = new ArrayList<>();
    private final List<LatLng> puntosLugares = new ArrayList<>();
    private AlTocarLugar alTocarLugar;
    private GoogleMap googleMap;
    private Marker principal;
    private int indicePrincipal = -1;
    private boolean destruido;

    public MiniMapa(MapView mapa) {
        this.mapa = mapa;
        mapa.onCreate(null);
        mapa.getMapAsync(this::alEstarListo);
    }

    private void alEstarListo(GoogleMap map) {
        if (destruido) return;
        googleMap = map;
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setCompassEnabled(false);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LIMA, ZOOM_CIUDAD));
        map.setOnInfoWindowClickListener(marcador -> {
            Object id = marcador.getTag();
            if (id instanceof String && alTocarLugar != null) alTocarLugar.onTocar((String) id);
        });
        for (Runnable r : pendientes) r.run();
        pendientes.clear();
    }

    private void cuandoEsteListo(Runnable accion) {
        if (googleMap != null) accion.run();
        else pendientes.add(accion);
    }

    public void limpiar() {
        cuandoEsteListo(() -> {
            googleMap.clear();
            puntos.clear();
            principal = null;
            indicePrincipal = -1;
            marcadoresLugares.clear();
            puntosLugares.clear();
        });
    }

    public void marcar(double lat, double lon, String titulo, boolean principal) {
        cuandoEsteListo(() -> {
            LatLng punto = new LatLng(lat, lon);
            Marker marcador = googleMap.addMarker(new MarkerOptions()
                    .position(punto)
                    .title(titulo)
                    .anchor(0.5f, 1f)
                    .icon(icono(principal)));
            puntos.add(punto);
            if (principal) {
                this.principal = marcador;
                indicePrincipal = puntos.size() - 1;
            }
            encuadrar();
        });
    }

    /**
     * Mueve el pin principal a una nueva posicion sin redibujar el mapa (seguimiento en
     * tiempo real). Si todavia no existe, lo crea.
     */
    public void moverPrincipal(double lat, double lon, String titulo) {
        cuandoEsteListo(() -> {
            if (principal == null) {
                marcar(lat, lon, titulo, true);
                return;
            }
            LatLng punto = new LatLng(lat, lon);
            principal.setPosition(punto);
            if (indicePrincipal >= 0 && indicePrincipal < puntos.size()) {
                puntos.set(indicePrincipal, punto);
            }
            // Si hay talleres en el mapa, solo se mueve el pin para no perder la vista general
            if (puntos.size() == 1 && puntosLugares.isEmpty()) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(punto));
            } else if (puntosLugares.isEmpty()) {
                encuadrar();
            }
        });
    }

    /**
     * Dibuja (o reemplaza) los pines secundarios, como los talleres cercanos. Al tocar la
     * burbuja de un pin se llama a alTocar con su id.
     */
    public void mostrarLugares(List<Lugar> lugares, AlTocarLugar alTocar) {
        cuandoEsteListo(() -> {
            alTocarLugar = alTocar;
            boolean primeraVez = marcadoresLugares.isEmpty();
            for (Marker m : marcadoresLugares) m.remove();
            marcadoresLugares.clear();
            puntosLugares.clear();
            for (Lugar l : lugares) {
                LatLng punto = new LatLng(l.latitud, l.longitud);
                Marker m = googleMap.addMarker(new MarkerOptions()
                        .position(punto)
                        .title(l.titulo)
                        .snippet(l.detalle)
                        .anchor(0.5f, 1f)
                        .icon(icono(false)));
                if (m != null) {
                    m.setTag(l.id);
                    marcadoresLugares.add(m);
                    puntosLugares.add(punto);
                }
            }
            if (primeraVez && !puntosLugares.isEmpty()) encuadrar();
        });
    }

    private BitmapDescriptor icono(boolean principal) {
        Drawable d = DrawableCompat.wrap(ContextCompat.getDrawable(mapa.getContext(),
                R.drawable.ic_location).mutate());
        DrawableCompat.setTint(d, ContextCompat.getColor(mapa.getContext(),
                principal ? R.color.autoque_black : R.color.autoque_yellow_dark));
        int lado = (int) (40 * mapa.getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888);
        d.setBounds(0, 0, lado, lado);
        d.draw(new Canvas(bitmap));
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void encuadrar() {
        List<LatLng> todos = new ArrayList<>(puntos);
        todos.addAll(puntosLugares);
        if (todos.size() == 1) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(todos.get(0), ZOOM_PUNTO));
        } else if (todos.size() > 1) {
            LatLngBounds.Builder caja = new LatLngBounds.Builder();
            for (LatLng p : todos) caja.include(p);
            int padding = (int) (PADDING_DP * mapa.getResources().getDisplayMetrics().density);
            Runnable mover = () -> {
                if (googleMap == null || destruido) return;
                LatLngBounds limites = caja.build();
                int ancho = mapa.getWidth();
                int alto = mapa.getHeight();
                if (ancho > 2 * padding && alto > 2 * padding) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(limites,
                            ancho, alto, padding));
                } else {
                    // El mapa aun no tiene tamano (por ejemplo, su tarjeta esta oculta)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            limites.getCenter(), ZOOM_CIUDAD + 1));
                }
            };
            if (mapa.getWidth() > 0 && mapa.getHeight() > 0) mover.run();
            else mapa.post(mover);
        }
    }

    public void onResume() {
        mapa.onStart();
        mapa.onResume();
    }

    public void onPause() {
        mapa.onPause();
        mapa.onStop();
    }

    public void onDestroy() {
        destruido = true;
        pendientes.clear();
        mapa.onDestroy();
    }
}
