package com.example.autoque.model;

import com.google.firebase.firestore.DocumentId;

public class Usuario {

    public static final String ROL_CLIENTE = "CLIENTE";
    public static final String ROL_TALLER = "TALLER";

    @DocumentId
    private String uid;
    private String rol;
    private String nombre;
    private String telefono;
    private String email;
    private long creadoEn;

    public Usuario() {
    }

    public Usuario(String uid, String rol, String nombre, String telefono, String email) {
        this.uid = uid;
        this.rol = rol;
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.creadoEn = System.currentTimeMillis();
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(long creadoEn) {
        this.creadoEn = creadoEn;
    }

    public boolean esTaller() {
        return ROL_TALLER.equals(rol);
    }

    public String primerNombre() {
        if (nombre == null) return "";
        String limpio = nombre.trim();
        int espacio = limpio.indexOf(' ');
        return espacio > 0 ? limpio.substring(0, espacio) : limpio;
    }
}
