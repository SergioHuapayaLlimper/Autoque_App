package com.example.autoque;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.autoque.model.Catalogos;
import com.example.autoque.model.Incidente;
import com.example.autoque.model.Solicitud;
import com.example.autoque.model.Taller;
import com.example.autoque.util.Geo;

import org.junit.Test;

import java.util.Arrays;

public class LogicaTest {

    @Test
    public void distanciaEntreLosOlivosYCercadoDeLima() {
        double km = Geo.distanciaKm(-11.9906, -77.0746, -12.0464, -77.0428);
        assertTrue("Esperado ~7 km, fue " + km, km > 6.5 && km < 7.5);
    }

    @Test
    public void distanciaNulaSiFaltaUnaCoordenada() {
        assertNull(Geo.distanciaKm(null, -77.0, -12.0, -77.0));
    }

    @Test
    public void llegadaEstimadaCrecConLaDistancia() {
        int[] cerca = Geo.llegadaEstimadaMin(1.0);
        int[] lejos = Geo.llegadaEstimadaMin(5.0);
        assertEquals(8, cerca[0]);
        assertEquals(cerca[0] + 5, cerca[1]);
        assertTrue(lejos[0] > cerca[0]);
    }

    @Test
    public void promedioDelTaller() {
        Taller t = new Taller();
        assertEquals(0, t.promedio(), 0.001);
        t.setSumaEstrellas(14);
        t.setTotalResenas(3);
        assertEquals(4.667, t.promedio(), 0.001);
    }

    @Test
    public void serviciosVisiblesReemplazaOtros() {
        Taller t = new Taller();
        t.setServicios(Arrays.asList("Mecánica general", Catalogos.SERVICIO_OTROS));
        t.setOtrosServicios("Diagnóstico con escáner");
        assertEquals(Arrays.asList("Mecánica general", "Diagnóstico con escáner"), t.serviciosVisibles());
    }

    @Test
    public void estadosDeSolicitud() {
        Solicitud s = new Solicitud();
        assertTrue(s.activa());
        s.setEstado(Solicitud.ACEPTADA);
        s.setTallerUid("taller-1");
        assertTrue(s.activa());
        assertTrue(s.asignadaA("taller-1"));
        assertFalse(s.asignadaA("taller-2"));
        s.setEstado(Solicitud.FINALIZADA);
        assertFalse(s.activa());
    }

    @Test
    public void costoDelPinchazo() {
        assertEquals("S/ 40 – 60", Incidente.PINCHAZO.rangoCosto());
    }

    @Test
    public void distritoDesdeGeocodificador() {
        assertEquals("Los Olivos", Catalogos.buscarDistrito("los olivos"));
        assertEquals("Santiago de Surco", Catalogos.buscarDistrito("Surco"));
        assertNull(Catalogos.buscarDistrito("Mountain View"));
    }
}
