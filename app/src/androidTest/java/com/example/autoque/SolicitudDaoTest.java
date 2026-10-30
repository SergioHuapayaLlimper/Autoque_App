package com.example.autoque;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.autoque.data.AutoqueDbHelper;
import com.example.autoque.data.SolicitudDao;
import com.example.autoque.data.UsuarioDao;
import com.example.autoque.model.Incidente;
import com.example.autoque.model.Solicitud;
import com.example.autoque.model.Usuario;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SolicitudDaoTest {

    private UsuarioDao usuarios;
    private SolicitudDao solicitudes;
    private String sufijo;
    private long cliente;
    private long tallerOlivos1;
    private long tallerOlivos2;
    private long tallerComas;

    @Before
    public void setUp() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        usuarios = new UsuarioDao(ctx);
        solicitudes = new SolicitudDao(ctx);
        sufijo = String.valueOf(System.nanoTime());

        cliente = usuarios.registrarCliente("Juan Pérez", "+51 987 654 321",
                "cliente" + sufijo + "@test.pe", "secreto1");
        tallerOlivos1 = registrarTaller("t1", "Los Olivos");
        tallerOlivos2 = registrarTaller("t2", "Los Olivos");
        tallerComas = registrarTaller("t3", "Comas");
    }

    @After
    public void tearDown() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SQLiteDatabase db = AutoqueDbHelper.getInstance(ctx).getWritableDatabase();
        db.delete("solicitud", "cliente_id = ?", new String[]{String.valueOf(cliente)});
        db.delete("usuario", "email LIKE ?", new String[]{"%" + sufijo + "@test.pe"});
    }

    private long registrarTaller(String nombre, String distrito) {
        return usuarios.registrarTaller("Taller " + nombre, "Encargado " + nombre,
                "+51 987 111 222", distrito, "Av. Siempre Viva 123",
                Arrays.asList("Mecánica general", "Otros"), "Escáner",
                nombre + sufijo + "@test.pe", "secreto1");
    }

    private long crearSolicitud(String distrito) {
        Solicitud s = new Solicitud();
        s.setClienteId(cliente);
        s.setNombreContacto("Juan Pérez");
        s.setTelefonoContacto("+51 987 654 321");
        s.setDireccion("Av. Carlos Izaguirre 123");
        s.setDistrito(distrito);
        s.setTipoIncidente(Incidente.PINCHAZO.etiqueta);
        s.setCostoMin(Incidente.PINCHAZO.costoMin);
        s.setCostoMax(Incidente.PINCHAZO.costoMax);
        return solicitudes.crear(s);
    }

    @Test
    public void registro_rechazaCorreoDuplicado_yAutenticaPorRol() {
        assertTrue(cliente > 0);
        long duplicado = usuarios.registrarCliente("Otro", "+51 900 000 000",
                "cliente" + sufijo + "@test.pe", "secreto1");
        assertEquals(-1, duplicado);

        Usuario u = usuarios.autenticar("t1" + sufijo + "@test.pe", "secreto1");
        assertNotNull(u);
        assertEquals(Usuario.ROL_TALLER, u.getRol());
        assertNull(usuarios.autenticar("t1" + sufijo + "@test.pe", "incorrecta"));
        assertEquals(2, usuarios.obtenerTaller(tallerOlivos1).getServicios().size());
    }

    @Test
    public void filtroPorDistrito_soloMuestraPendientesDelMismoDistrito() {
        long enOlivos = crearSolicitud("Los Olivos");
        long enComas = crearSolicitud("Comas");

        List<Solicitud> paraOlivos = solicitudes.pendientesDelDistritoDelTaller(tallerOlivos1);
        assertTrue(contiene(paraOlivos, enOlivos));
        assertFalse(contiene(paraOlivos, enComas));

        List<Solicitud> paraComas = solicitudes.pendientesDelDistritoDelTaller(tallerComas);
        assertTrue(contiene(paraComas, enComas));
        assertFalse(contiene(paraComas, enOlivos));
    }

    @Test
    public void firstComeFirstServed_soloElPrimerTallerSeAdjudica() {
        long id = crearSolicitud("Los Olivos");

        assertTrue(solicitudes.aceptar(id, tallerOlivos1));
        assertFalse("El segundo taller no debe poder tomarla", solicitudes.aceptar(id, tallerOlivos2));

        Solicitud s = solicitudes.obtener(id);
        assertEquals(Solicitud.ACEPTADA, s.getEstado());
        assertEquals(Long.valueOf(tallerOlivos1), s.getTallerId());
        assertFalse("Ya no aparece para otros talleres",
                contiene(solicitudes.pendientesDelDistritoDelTaller(tallerOlivos2), id));
    }

    @Test
    public void tallerDeOtroDistrito_noPuedeAceptar() {
        long id = crearSolicitud("Los Olivos");
        assertFalse(solicitudes.aceptar(id, tallerComas));
        assertEquals(Solicitud.PENDIENTE, solicitudes.obtener(id).getEstado());
    }

    @Test
    public void soloElTallerAsignadoPuedeFinalizar() {
        long id = crearSolicitud("Los Olivos");
        assertTrue(solicitudes.aceptar(id, tallerOlivos1));

        assertFalse(solicitudes.finalizar(id, tallerOlivos2));
        assertTrue(solicitudes.finalizar(id, tallerOlivos1));
        assertEquals(Solicitud.FINALIZADA, solicitudes.obtener(id).getEstado());
        assertNull(solicitudes.solicitudActivaDelCliente(cliente));
    }

    @Test
    public void cliente_soloCancelaMientrasEstaPendiente() {
        long id = crearSolicitud("Los Olivos");
        assertTrue(solicitudes.cancelar(id, cliente));
        assertEquals(Solicitud.CANCELADA, solicitudes.obtener(id).getEstado());
        assertNull(solicitudes.solicitudActivaDelCliente(cliente));
        assertFalse("Cancelada ya no aparece para los talleres",
                contiene(solicitudes.pendientesDelDistritoDelTaller(tallerOlivos1), id));
        assertFalse("Un taller no puede aceptar una cancelada", solicitudes.aceptar(id, tallerOlivos1));

        long aceptada = crearSolicitud("Los Olivos");
        assertTrue(solicitudes.aceptar(aceptada, tallerOlivos1));
        assertFalse("No se cancela si un taller ya la aceptó", solicitudes.cancelar(aceptada, cliente));
        assertEquals(Solicitud.ACEPTADA, solicitudes.obtener(aceptada).getEstado());
    }

    private static boolean contiene(List<Solicitud> lista, long id) {
        for (Solicitud s : lista) if (s.getId() == id) return true;
        return false;
    }
}
