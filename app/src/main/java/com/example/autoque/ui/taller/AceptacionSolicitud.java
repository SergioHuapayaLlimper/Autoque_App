package com.example.autoque.ui.taller;

import android.app.Activity;

import com.example.autoque.R;
import com.example.autoque.data.Resultado;
import com.example.autoque.data.SolicitudRepository;
import com.example.autoque.model.Solicitud;
import com.example.autoque.model.Taller;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

final class AceptacionSolicitud {

    interface Callback {
        void onVerAtencion();

        default void onNoDisponible() {
        }
    }

    private AceptacionSolicitud() {}

    static void aceptar(Activity activity, Solicitud solicitud, Taller taller, Callback callback) {
        if (taller == null) return;
        new SolicitudRepository().aceptar(solicitud.getId(), taller, new Resultado<>() {
            @Override
            public void onExito(Boolean asignada) {
                if (activity.isFinishing()) return;
                if (asignada) {
                    new MaterialAlertDialogBuilder(activity)
                            .setIcon(R.drawable.ic_check)
                            .setTitle(R.string.solicitud_aceptada_titulo)
                            .setMessage(activity.getString(R.string.solicitud_aceptada_msg,
                                    solicitud.getNombreContacto()))
                            .setNegativeButton(R.string.seguir_aqui, null)
                            .setPositiveButton(R.string.ver_atencion, (d, w) -> callback.onVerAtencion())
                            .show();
                } else {
                    new MaterialAlertDialogBuilder(activity)
                            .setIcon(R.drawable.ic_alert)
                            .setTitle(R.string.no_disponible_titulo)
                            .setMessage(R.string.no_disponible_msg)
                            .setPositiveButton(R.string.entendido, (d, w) -> callback.onNoDisponible())
                            .setCancelable(false)
                            .show();
                }
            }

            @Override
            public void onError(int mensaje) {
                if (activity.isFinishing()) return;
                Snackbar.make(activity.findViewById(android.R.id.content), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
