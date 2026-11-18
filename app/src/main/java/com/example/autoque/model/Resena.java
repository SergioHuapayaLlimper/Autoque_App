package com.example.autoque.model;

import com.google.firebase.firestore.DocumentId;

public class Resena {

    @DocumentId
    private String id;
    private String tallerUid;
    private String solicitudId;
    private String autorUid;
    private String autorNombre;
    private int estrellas;
    private String comentario;
    private long creadoEn;

    public Resena() {
    }

    public Resena(String autorUid, String autorNombre, int estrellas, String comentario) {
        this.autorUid = autorUid;
        this.autorNombre = autorNombre;
        this.estrellas = estrellas;
        this.comentario = comentario;
        this.creadoEn = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTallerUid() {
        return tallerUid;
    }

    public void setTallerUid(String tallerUid) {
        this.tallerUid = tallerUid;
    }

    public String getSolicitudId() {
        return solicitudId;
    }

    public void setSolicitudId(String solicitudId) {
        this.solicitudId = solicitudId;
    }

    public String getAutorUid() {
        return autorUid;
    }

    public void setAutorUid(String autorUid) {
        this.autorUid = autorUid;
    }

    public String getAutorNombre() {
        return autorNombre;
    }

    public void setAutorNombre(String autorNombre) {
        this.autorNombre = autorNombre;
    }

    public int getEstrellas() {
        return estrellas;
    }

    public void setEstrellas(int estrellas) {
        this.estrellas = estrellas;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public long getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(long creadoEn) {
        this.creadoEn = creadoEn;
    }
}
