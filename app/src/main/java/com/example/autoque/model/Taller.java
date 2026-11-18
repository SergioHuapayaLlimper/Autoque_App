package com.example.autoque.model;

import com.google.firebase.firestore.DocumentId;

import java.util.ArrayList;
import java.util.List;

public class Taller {

    @DocumentId
    private String uid;
    private String nombreTaller;
    private String encargado;
    private String telefono;
    private String email;
    private String distrito;
    private String direccion;
    private List<String> servicios = new ArrayList<>();
    private String otrosServicios;
    private Double latitud;
    private Double longitud;
    private long sumaEstrellas;
    private long totalResenas;
    private long creadoEn;

    public Taller() {
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNombreTaller() {
        return nombreTaller;
    }

    public void setNombreTaller(String nombreTaller) {
        this.nombreTaller = nombreTaller;
    }

    public String getEncargado() {
        return encargado;
    }

    public void setEncargado(String encargado) {
        this.encargado = encargado;
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

    public String getDistrito() {
        return distrito;
    }

    public void setDistrito(String distrito) {
        this.distrito = distrito;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public List<String> getServicios() {
        return servicios;
    }

    public void setServicios(List<String> servicios) {
        this.servicios = servicios;
    }

    public String getOtrosServicios() {
        return otrosServicios;
    }

    public void setOtrosServicios(String otrosServicios) {
        this.otrosServicios = otrosServicios;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public long getSumaEstrellas() {
        return sumaEstrellas;
    }

    public void setSumaEstrellas(long sumaEstrellas) {
        this.sumaEstrellas = sumaEstrellas;
    }

    public long getTotalResenas() {
        return totalResenas;
    }

    public void setTotalResenas(long totalResenas) {
        this.totalResenas = totalResenas;
    }

    public long getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(long creadoEn) {
        this.creadoEn = creadoEn;
    }

    public double promedio() {
        return totalResenas == 0 ? 0 : (double) sumaEstrellas / totalResenas;
    }

    public boolean tieneUbicacion() {
        return latitud != null && longitud != null;
    }

    public List<String> serviciosVisibles() {
        List<String> lista = new ArrayList<>();
        if (servicios == null) return lista;
        for (String s : servicios) {
            if (Catalogos.SERVICIO_OTROS.equals(s)) {
                if (otrosServicios != null && !otrosServicios.trim().isEmpty()) {
                    lista.add(otrosServicios.trim());
                }
            } else {
                lista.add(s);
            }
        }
        return lista;
    }
}
