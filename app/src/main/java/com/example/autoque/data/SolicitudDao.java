package com.example.autoque.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.autoque.model.Solicitud;

import java.util.ArrayList;
import java.util.List;

public class SolicitudDao {

    private static final String SELECT_BASE =
            "SELECT s.id, s.cliente_id, s.nombre_contacto, s.telefono_contacto, s.direccion, "
                    + "s.distrito, s.latitud, s.longitud, s.tipo_incidente, s.descripcion, "
                    + "s.costo_min, s.costo_max, s.estado, s.taller_id, t.nombre_taller, "
                    + "s.creado_en, s.aceptado_en, s.finalizado_en "
                    + "FROM solicitud s LEFT JOIN taller t ON t.usuario_id = s.taller_id ";

    private final AutoqueDbHelper helper;

    public SolicitudDao(Context context) {
        helper = AutoqueDbHelper.getInstance(context);
    }

    public long crear(Solicitud s) {
        ContentValues v = new ContentValues();
        v.put("cliente_id", s.getClienteId());
        v.put("nombre_contacto", s.getNombreContacto());
        v.put("telefono_contacto", s.getTelefonoContacto());
        v.put("direccion", s.getDireccion());
        v.put("distrito", s.getDistrito());
        if (s.tieneUbicacion()) {
            v.put("latitud", s.getLatitud());
            v.put("longitud", s.getLongitud());
        }
        v.put("tipo_incidente", s.getTipoIncidente());
        v.put("descripcion", s.getDescripcion());
        v.put("costo_min", s.getCostoMin());
        v.put("costo_max", s.getCostoMax());
        v.put("estado", Solicitud.PENDIENTE);
        v.put("creado_en", System.currentTimeMillis());
        return helper.getWritableDatabase().insert("solicitud", null, v);
    }

    public Solicitud obtener(long id) {
        List<Solicitud> lista = consultar("WHERE s.id = ?", String.valueOf(id));
        return lista.isEmpty() ? null : lista.get(0);
    }

    public Solicitud solicitudActivaDelCliente(long clienteId) {
        List<Solicitud> lista = consultar(
                "WHERE s.cliente_id = ? AND s.estado IN ('PENDIENTE', 'ACEPTADA') "
                        + "ORDER BY s.creado_en DESC LIMIT 1", String.valueOf(clienteId));
        return lista.isEmpty() ? null : lista.get(0);
    }

    public List<Solicitud> pendientesDelDistritoDelTaller(long tallerId) {
        return consultar(
                "WHERE s.estado = 'PENDIENTE' "
                        + "AND s.distrito = (SELECT distrito FROM taller WHERE usuario_id = ?) "
                        + "ORDER BY s.creado_en ASC", String.valueOf(tallerId));
    }

    public boolean aceptar(long solicitudId, long tallerId) {
        ContentValues v = new ContentValues();
        v.put("estado", Solicitud.ACEPTADA);
        v.put("taller_id", tallerId);
        v.put("aceptado_en", System.currentTimeMillis());
        int filas = helper.getWritableDatabase().update("solicitud", v,
                "id = ? AND estado = 'PENDIENTE' "
                        + "AND distrito = (SELECT distrito FROM taller WHERE usuario_id = ?)",
                new String[]{String.valueOf(solicitudId), String.valueOf(tallerId)});
        return filas == 1;
    }

    public boolean cancelar(long solicitudId, long clienteId) {
        ContentValues v = new ContentValues();
        v.put("estado", Solicitud.CANCELADA);
        int filas = helper.getWritableDatabase().update("solicitud", v,
                "id = ? AND cliente_id = ? AND estado = 'PENDIENTE'",
                new String[]{String.valueOf(solicitudId), String.valueOf(clienteId)});
        return filas == 1;
    }

    public boolean finalizar(long solicitudId, long tallerId) {
        ContentValues v = new ContentValues();
        v.put("estado", Solicitud.FINALIZADA);
        v.put("finalizado_en", System.currentTimeMillis());
        int filas = helper.getWritableDatabase().update("solicitud", v,
                "id = ? AND taller_id = ? AND estado = 'ACEPTADA'",
                new String[]{String.valueOf(solicitudId), String.valueOf(tallerId)});
        return filas == 1;
    }

    public List<Solicitud> atencionesEnCurso(long tallerId) {
        return consultar("WHERE s.taller_id = ? AND s.estado = 'ACEPTADA' "
                + "ORDER BY s.aceptado_en DESC", String.valueOf(tallerId));
    }

    public List<Solicitud> atencionesFinalizadas(long tallerId) {
        return consultar("WHERE s.taller_id = ? AND s.estado = 'FINALIZADA' "
                + "ORDER BY s.finalizado_en DESC", String.valueOf(tallerId));
    }

    private List<Solicitud> consultar(String where, String... args) {
        List<Solicitud> lista = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(SELECT_BASE + where, args)) {
            while (c.moveToNext()) lista.add(leer(c));
        }
        return lista;
    }

    private static Solicitud leer(Cursor c) {
        Solicitud s = new Solicitud();
        s.setId(c.getLong(0));
        s.setClienteId(c.getLong(1));
        s.setNombreContacto(c.getString(2));
        s.setTelefonoContacto(c.getString(3));
        s.setDireccion(c.getString(4));
        s.setDistrito(c.getString(5));
        s.setLatitud(c.isNull(6) ? null : c.getDouble(6));
        s.setLongitud(c.isNull(7) ? null : c.getDouble(7));
        s.setTipoIncidente(c.getString(8));
        s.setDescripcion(c.getString(9));
        s.setCostoMin(c.getInt(10));
        s.setCostoMax(c.getInt(11));
        s.setEstado(c.getString(12));
        s.setTallerId(c.isNull(13) ? null : c.getLong(13));
        s.setNombreTaller(c.getString(14));
        s.setCreadoEn(c.getLong(15));
        s.setAceptadoEn(c.isNull(16) ? null : c.getLong(16));
        s.setFinalizadoEn(c.isNull(17) ? null : c.getLong(17));
        return s;
    }
}
