package com.example.autoque.model;

import com.google.firebase.firestore.DocumentId;

public class FotoTaller {

    public static final int MAXIMO_POR_TALLER = 10;

    @DocumentId
    private String id;
    private String imagenBase64;
    private String titulo;
    private long creadoEn;

    public FotoTaller() {
    }

    public FotoTaller(String imagenBase64, String titulo) {
        this.imagenBase64 = imagenBase64;
        this.titulo = titulo;
        this.creadoEn = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImagenBase64() {
        return imagenBase64;
    }

    public void setImagenBase64(String imagenBase64) {
        this.imagenBase64 = imagenBase64;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public long getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(long creadoEn) {
        this.creadoEn = creadoEn;
    }
}
