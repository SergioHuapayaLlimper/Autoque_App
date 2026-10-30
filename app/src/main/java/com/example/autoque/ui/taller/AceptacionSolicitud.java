package com.example.autoque.ui.taller;

import android.content.Context;

import com.example.autoque.R;
import com.example.autoque.data.SolicitudDao;
import com.example.autoque.model.Solicitud;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

final class AceptacionSolicitud {

    interface Callback {
        void onListaCambio();

        void onVerAtencion();

        default void onNoDisponible() {}
    }

    private AceptacionSolicitud() {}

    static void aceptar(Context context, Solicitud solicitud, long tallerId, Callback callback) {
        boolean asignada = new SolicitudDao(context).aceptar(solicitud.getId(), tallerId);
        callback.onListaCambio();
        if (asignada) {
            new MaterialAlertDialogBuilder(context)
                    .setIcon(R.drawable.ic_check)
                    .setTitle(R.string.solicitud_aceptada_titulo)
                    .setMessage(context.getString(R.string.solicitud_aceptada_msg,
                            solicitud.getNombreContacto()))
                    .setNegativeButton(R.string.seguir_aqui, null)
                    .setPositiveButton(R.string.ver_atencion, (d, w) -> callback.onVerAtencion())
                    .show();
        } else {
            new MaterialAlertDialogBuilder(context)
                    .setIcon(R.drawable.ic_alert)
                    .setTitle(R.string.no_disponible_titulo)
                    .setMessage(R.string.no_disponible_msg)
                    .setPositiveButton(R.string.entendido, (d, w) -> callback.onNoDisponible())
                    .setCancelable(false)
                    .show();
        }
    }
}
