package com.example.autoque.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.autoque.R;
import com.example.autoque.model.Solicitud;
import com.example.autoque.ui.cliente.EstadoSolicitudActivity;
import com.example.autoque.ui.taller.DetalleSolicitudActivity;

public final class Notificaciones {

    private static final String CANAL_SOLICITUDES = "solicitudes";
    private static final String CANAL_TALLERES = "talleres";

    private Notificaciones() {}

    public static void crearCanal(Context context) {
        NotificationChannel canal = new NotificationChannel(CANAL_SOLICITUDES,
                context.getString(R.string.canal_solicitudes), NotificationManager.IMPORTANCE_HIGH);
        canal.setDescription(context.getString(R.string.canal_solicitudes_desc));
        context.getSystemService(NotificationManager.class).createNotificationChannel(canal);
    }

    public static void crearCanalTalleres(Context context) {
        NotificationChannel canal = new NotificationChannel(CANAL_TALLERES,
                context.getString(R.string.canal_talleres), NotificationManager.IMPORTANCE_HIGH);
        canal.setDescription(context.getString(R.string.canal_talleres_desc));
        context.getSystemService(NotificationManager.class).createNotificationChannel(canal);
    }

    /** HU05: avisa al taller que un conductor de su distrito pidio asistencia. */
    public static void nuevaSolicitud(Context context, Solicitud s) {
        if (!tienePermiso(context)) return;
        Intent intent = new Intent(context, DetalleSolicitudActivity.class);
        intent.putExtra(DetalleSolicitudActivity.EXTRA_SOLICITUD_ID, s.getId());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent abrir = PendingIntent.getActivity(context, ("nueva" + s.getId()).hashCode(),
                intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String texto = context.getString(R.string.notif_nueva_texto,
                s.getTipoIncidente(), s.getDireccion());

        NotificationCompat.Builder n = new NotificationCompat.Builder(context, CANAL_TALLERES)
                .setSmallIcon(R.drawable.ic_car)
                .setColor(ContextCompat.getColor(context, R.color.autoque_yellow))
                .setContentTitle(context.getString(R.string.notif_nueva_titulo, s.getDistrito()))
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(abrir);
        try {
            NotificationManagerCompat.from(context).notify(("nueva" + s.getId()).hashCode(), n.build());
        } catch (SecurityException ignorada) {
        }
    }

    public static boolean tienePermiso(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void solicitudAceptada(Context context, Solicitud s) {
        if (!tienePermiso(context)) return;
        Intent intent = new Intent(context, EstadoSolicitudActivity.class);
        intent.putExtra(EstadoSolicitudActivity.EXTRA_SOLICITUD_ID, s.getId());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent abrir = PendingIntent.getActivity(context, s.getId().hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder n = new NotificationCompat.Builder(context, CANAL_SOLICITUDES)
                .setSmallIcon(R.drawable.ic_wrench)
                .setColor(ContextCompat.getColor(context, R.color.autoque_yellow))
                .setContentTitle(context.getString(R.string.notif_aceptada_titulo))
                .setContentText(context.getString(R.string.notif_aceptada_texto, s.getNombreTaller()))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        context.getString(R.string.notif_aceptada_texto, s.getNombreTaller())))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(abrir);
        try {
            NotificationManagerCompat.from(context).notify(s.getId().hashCode(), n.build());
        } catch (SecurityException ignorada) {
        }
    }
}
