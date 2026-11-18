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
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapa pequeno de Google Maps para mostrar la ubicacion del cliente y del taller.
 * Se encarga del ciclo de vida del MapView: la pantalla solo llama a onResume,
 * onPause y onDestroy.
 */
public class MiniMapa {

    private static final LatLng LIMA = new LatLng(-12.0464, -77.0428);
    private static final float ZOOM_CIUDAD = 12f;
    private static final float ZOOM_PUNTO = 16f;
    private static final int PADDING_DP = 48;

    private final MapView mapa;
    private final List<LatLng> puntos = new ArrayList<>();
    private final List<Runnable> pendientes = new ArrayList<>();
    private GoogleMap googleMap;
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
        });
    }

    public void marcar(double lat, double lon, String titulo, boolean principal) {
        cuandoEsteListo(() -> {
            LatLng punto = new LatLng(lat, lon);
            googleMap.addMarker(new MarkerOptions()
                    .position(punto)
                    .title(titulo)
                    .anchor(0.5f, 1f)
                    .icon(icono(principal)));
            puntos.add(punto);
            encuadrar();
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
        if (puntos.size() == 1) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(puntos.get(0), ZOOM_PUNTO));
        } else if (puntos.size() > 1) {
            LatLngBounds.Builder caja = new LatLngBounds.Builder();
            for (LatLng p : puntos) caja.include(p);
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
