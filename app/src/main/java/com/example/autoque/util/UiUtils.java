package com.example.autoque.util;

import android.content.Context;
import android.text.format.DateUtils;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.autoque.R;
import com.example.autoque.model.Solicitud;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class UiUtils {

    private static final Locale ES_PE = Locale.forLanguageTag("es-PE");

    private UiUtils() {}

    public static String iniciales(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return "?";
        String[] partes = nombre.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(partes[0].charAt(0)));
        if (partes.length > 1) sb.append(Character.toUpperCase(partes[1].charAt(0)));
        return sb.toString();
    }

    public static String haceCuanto(long millis) {
        long minutos = Math.max(0, (System.currentTimeMillis() - millis) / DateUtils.MINUTE_IN_MILLIS);
        if (minutos < 1) return "hace un momento";
        if (minutos < 60) return "hace " + minutos + " min";
        long horas = minutos / 60;
        if (horas < 24) return "hace " + horas + " h";
        return fechaHora(millis);
    }

    public static String hora(long millis) {
        return new SimpleDateFormat("h:mm a", ES_PE).format(new Date(millis));
    }

    public static String fechaHora(long millis) {
        if (DateUtils.isToday(millis)) return "Hoy, " + hora(millis);
        return new SimpleDateFormat("d MMM, h:mm a", ES_PE).format(new Date(millis));
    }

    public static void pintarEstado(TextView badge, String estado) {
        Context c = badge.getContext();
        badge.setText(Solicitud.etiquetaEstado(estado));
        int fondo;
        int texto;
        switch (estado) {
            case Solicitud.ACEPTADA:
                fondo = R.drawable.bg_badge_aceptada;
                texto = R.color.autoque_yellow;
                break;
            case Solicitud.FINALIZADA:
                fondo = R.drawable.bg_badge_finalizada;
                texto = R.color.text_primary;
                break;
            case Solicitud.CANCELADA:
                fondo = R.drawable.bg_badge_cancelada;
                texto = R.color.error;
                break;
            default:
                fondo = R.drawable.bg_badge_pendiente;
                texto = R.color.text_primary;
        }
        badge.setBackgroundResource(fondo);
        badge.setTextColor(ContextCompat.getColor(c, texto));
    }
}
