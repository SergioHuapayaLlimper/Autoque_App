package com.example.autoque.model;

public class Usuario {

    public static final String ROL_CLIENTE = "CLIENTE";
    public static final String ROL_TALLER = "TALLER";

    private long id;
    private String rol;
    private String nombre;
    private String telefono;
    private String email;
    private String firebaseUid;

    public Usuario() {
    }

    public Usuario(long id, String rol, String nombre, String telefono, String email,
                   String firebaseUid) {
        this.id = id;
        this.rol = rol;
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.firebaseUid = firebaseUid;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
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
