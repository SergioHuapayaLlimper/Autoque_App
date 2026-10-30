package com.example.autoque.model;

public enum Incidente {
    PINCHAZO("Pinchazo de llanta", 40, 60),
    BATERIA("Batería descargada", 50, 70),
    CHOQUE("Choque leve", 120, 200),
    FALLA("Falla mecánica", 80, 150),
    COMBUSTIBLE("Sin combustible", 30, 50),
    LLAVES("Llaves dentro del auto", 60, 90),
    OTRO("Otro", 50, 150);

    public final String etiqueta;
    public final int costoMin;
    public final int costoMax;

    Incidente(String etiqueta, int costoMin, int costoMax) {
        this.etiqueta = etiqueta;
        this.costoMin = costoMin;
        this.costoMax = costoMax;
    }

    public String rangoCosto() {
        return "S/ " + costoMin + " – " + costoMax;
    }
}
