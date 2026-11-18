package com.example.autoque.model;

import com.google.firebase.firestore.DocumentId;

public class Solicitud {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String ACEPTADA = "ACEPTADA";
    public static final String FINALIZADA = "FINALIZADA";
    public static final String CANCELADA = "CANCELADA";

    @DocumentId
    private String id;
    private long numero;
    private String clienteUid;
    private String nombreContacto;
    private String telefonoContacto;
    private String direccion;
    private String distrito;
    private Double latitud;
    private Double longitud;
    private String tipoIncidente;
    private String descripcion;
    private int costoMin;
    private int costoMax;
    private String estado = PENDIENTE;
    private String tallerUid;
    private String nombreTaller;
    private String telefonoTaller;
    private long creadoEn;
    private Long aceptadoEn;
    private Long finalizadoEn;
    private boolean calificada;

    public Solicitud() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getNumero() {
        return numero;
    }

    public void setNumero(long numero) {
        this.numero = numero;
    }

    public String getClienteUid() {
        return clienteUid;
    }

    public void setClienteUid(String clienteUid) {
        this.clienteUid = clienteUid;
    }

    public String getNombreContacto() {
        return nombreContacto;
    }

    public void setNombreContacto(String nombreContacto) {
        this.nombreContacto = nombreContacto;
    }

    public String getTelefonoContacto() {
        return telefonoContacto;
    }

    public void setTelefonoContacto(String telefonoContacto) {
        this.telefonoContacto = telefonoContacto;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getDistrito() {
        return distrito;
    }

    public void setDistrito(String distrito) {
        this.distrito = distrito;
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

    public String getTipoIncidente() {
        return tipoIncidente;
    }

    public void setTipoIncidente(String tipoIncidente) {
        this.tipoIncidente = tipoIncidente;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getCostoMin() {
        return costoMin;
    }

    public void setCostoMin(int costoMin) {
        this.costoMin = costoMin;
    }

    public int getCostoMax() {
        return costoMax;
    }

    public void setCostoMax(int costoMax) {
        this.costoMax = costoMax;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTallerUid() {
        return tallerUid;
    }

    public void setTallerUid(String tallerUid) {
        this.tallerUid = tallerUid;
    }

    public String getNombreTaller() {
        return nombreTaller;
    }

    public void setNombreTaller(String nombreTaller) {
        this.nombreTaller = nombreTaller;
    }

    public String getTelefonoTaller() {
        return telefonoTaller;
    }

    public void setTelefonoTaller(String telefonoTaller) {
        this.telefonoTaller = telefonoTaller;
    }

    public long getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(long creadoEn) {
        this.creadoEn = creadoEn;
    }

    public Long getAceptadoEn() {
        return aceptadoEn;
    }

    public void setAceptadoEn(Long aceptadoEn) {
        this.aceptadoEn = aceptadoEn;
    }

    public Long getFinalizadoEn() {
        return finalizadoEn;
    }

    public void setFinalizadoEn(Long finalizadoEn) {
        this.finalizadoEn = finalizadoEn;
    }

    public boolean isCalificada() {
        return calificada;
    }

    public void setCalificada(boolean calificada) {
        this.calificada = calificada;
    }

    public String rangoCosto() {
        return "S/ " + costoMin + " – " + costoMax;
    }

    public boolean tieneUbicacion() {
        return latitud != null && longitud != null;
    }

    public boolean asignadaA(String uidTaller) {
        return tallerUid != null && tallerUid.equals(uidTaller);
    }

    public boolean activa() {
        return PENDIENTE.equals(estado) || ACEPTADA.equals(estado);
    }

    public static String etiquetaEstado(String estado) {
        if (estado == null) return "";
        switch (estado) {
            case ACEPTADA: return "Aceptada";
            case FINALIZADA: return "Finalizada";
            case CANCELADA: return "Cancelada";
            default: return "Pendiente";
        }
    }
}
