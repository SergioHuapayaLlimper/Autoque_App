package com.example.autoque.util;

import java.util.Locale;

public final class Geo {

    private static final double RADIO_TIERRA_KM = 6371.0;
    private static final double MINUTOS_POR_KM = 3.0;
    private static final int MINUTOS_PREPARACION = 5;

    private Geo() {}

    public static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return RADIO_TIERRA_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static Double distanciaKm(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null;
        return distanciaKm(lat1.doubleValue(), lon1.doubleValue(), lat2.doubleValue(), lon2.doubleValue());
    }

    public static String formatearKm(double km) {
        return String.format(Locale.US, "%.1f km", km);
    }

    public static int[] llegadaEstimadaMin(double km) {
        int base = (int) Math.round(km * MINUTOS_POR_KM) + MINUTOS_PREPARACION;
        return new int[]{base, base + 5};
    }
}
