package com.example.autoque.model;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class Catalogos {

    private Catalogos() {}

    public static final List<String> DISTRITOS_LIMA = Collections.unmodifiableList(Arrays.asList(
            "Ancón", "Ate", "Barranco", "Breña", "Carabayllo", "Cercado de Lima",
            "Chaclacayo", "Chorrillos", "Cieneguilla", "Comas", "El Agustino",
            "Independencia", "Jesús María", "La Molina", "La Victoria", "Lince",
            "Los Olivos", "Lurigancho-Chosica", "Lurín", "Magdalena del Mar",
            "Miraflores", "Pachacámac", "Pucusana", "Pueblo Libre", "Puente Piedra",
            "Punta Hermosa", "Punta Negra", "Rímac", "San Bartolo", "San Borja",
            "San Isidro", "San Juan de Lurigancho", "San Juan de Miraflores",
            "San Luis", "San Martín de Porres", "San Miguel", "Santa Anita",
            "Santa María del Mar", "Santa Rosa", "Santiago de Surco", "Surquillo",
            "Villa El Salvador", "Villa María del Triunfo"));

    public static final List<String> SERVICIOS_TALLER = Collections.unmodifiableList(Arrays.asList(
            "Cambio de llanta / pinchazo",
            "Carga o cambio de batería",
            "Remolque / grúa",
            "Mecánica general",
            "Electricidad automotriz",
            "Planchado y pintura",
            "Otros"));

    public static final String SERVICIO_OTROS = "Otros";

    public static String buscarDistrito(String texto) {
        if (texto == null || texto.trim().isEmpty()) return null;
        String buscado = normalizar(texto);
        for (String distrito : DISTRITOS_LIMA) {
            if (normalizar(distrito).equals(buscado)) return distrito;
        }
        if (buscado.equals("lima")) return "Cercado de Lima";
        if (buscado.equals("surco")) return "Santiago de Surco";
        return null;
    }

    private static String normalizar(String s) {
        String sinTildes = Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }
}
