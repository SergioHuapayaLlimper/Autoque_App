package com.example.autoque.util;

import android.content.Context;

import com.example.autoque.data.Escucha;
import com.example.autoque.data.PerfilRepository;
import com.example.autoque.data.SolicitudRepository;
import com.example.autoque.model.Solicitud;
import com.example.autoque.model.Taller;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * HU05: avisa al taller con una notificacion cuando llega una solicitud nueva de su distrito.
 * Funciona mientras la app del taller esta abierta o en segundo plano (sin servidor de push).
 */
public final class AvisoSolicitudesTaller {

    private static final long ANTIGUEDAD_MAXIMA_MS = 10 * 60 * 1000L;

    private static ListenerRegistration registroTaller;
    private static ListenerRegistration registroSolicitudes;
    private static String uidActual;
    private static String distritoActual;
    private static final Set<String> conocidas = new HashSet<>();
    private static boolean primeraCarga;

    private AvisoSolicitudesTaller() {}

    public static synchronized void iniciar(Context context, String tallerUid) {
        if (registroTaller != null && tallerUid.equals(uidActual)) return;
        detener();
        Context app = context.getApplicationContext();
        uidActual = tallerUid;
        registroTaller = new PerfilRepository().escucharTaller(tallerUid, new Escucha<>() {
            @Override
            public void onCambio(Taller taller) {
                if (taller != null) escucharDistrito(app, taller.getDistrito());
            }
        });
    }

    public static synchronized void detener() {
        if (registroTaller != null) registroTaller.remove();
        if (registroSolicitudes != null) registroSolicitudes.remove();
        registroTaller = null;
        registroSolicitudes = null;
        uidActual = null;
        distritoActual = null;
        conocidas.clear();
    }

    private static synchronized void escucharDistrito(Context context, String distrito) {
        if (distrito == null || distrito.equals(distritoActual)) return;
        if (registroSolicitudes != null) registroSolicitudes.remove();
        distritoActual = distrito;
        conocidas.clear();
        primeraCarga = true;
        registroSolicitudes = new SolicitudRepository().escucharPendientesDelDistrito(distrito,
                new Escucha<>() {
                    @Override
                    public void onCambio(List<Solicitud> lista) {
                        revisar(context, lista);
                    }
                });
    }

    private static synchronized void revisar(Context context, List<Solicitud> lista) {
        long ahora = System.currentTimeMillis();
        for (Solicitud s : lista) {
            if (s.getId() == null || !conocidas.add(s.getId())) continue;
            // En la primera carga solo se registran las que ya existian: no se notifican
            if (!primeraCarga && ahora - s.getCreadoEn() < ANTIGUEDAD_MAXIMA_MS) {
                Notificaciones.nuevaSolicitud(context, s);
            }
        }
        primeraCarga = false;
    }
}
