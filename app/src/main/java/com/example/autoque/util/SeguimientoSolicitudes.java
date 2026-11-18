package com.example.autoque.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.autoque.data.Escucha;
import com.example.autoque.data.SolicitudRepository;
import com.example.autoque.model.Solicitud;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SeguimientoSolicitudes {

    private static final String PREFS = "autoque_notificadas";
    private static final String KEY_IDS = "ids";
    private static final long VENTANA_MS = 2 * 60 * 60 * 1000L;

    private static ListenerRegistration registro;
    private static String uidActual;

    private SeguimientoSolicitudes() {}

    public static synchronized void iniciar(Context context, String clienteUid) {
        if (registro != null && clienteUid.equals(uidActual)) return;
        detener();
        Context app = context.getApplicationContext();
        uidActual = clienteUid;
        registro = new SolicitudRepository().escucharDelCliente(clienteUid, new Escucha<>() {
            @Override
            public void onCambio(List<Solicitud> lista) {
                revisar(app, lista);
            }
        });
    }

    public static synchronized void detener() {
        if (registro != null) registro.remove();
        registro = null;
        uidActual = null;
    }

    private static void revisar(Context context, List<Solicitud> lista) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> notificadas = new HashSet<>(prefs.getStringSet(KEY_IDS, new HashSet<>()));
        boolean cambios = false;
        long ahora = System.currentTimeMillis();
        for (Solicitud s : lista) {
            if (!Solicitud.ACEPTADA.equals(s.getEstado()) || notificadas.contains(s.getId())) continue;
            Long aceptadoEn = s.getAceptadoEn();
            if (aceptadoEn != null && ahora - aceptadoEn < VENTANA_MS) {
                Notificaciones.solicitudAceptada(context, s);
            }
            notificadas.add(s.getId());
            cambios = true;
        }
        if (cambios) prefs.edit().putStringSet(KEY_IDS, notificadas).apply();
    }
}
