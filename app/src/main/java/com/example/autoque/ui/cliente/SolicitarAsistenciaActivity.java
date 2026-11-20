package com.example.autoque.ui.cliente;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.example.autoque.R;
import com.example.autoque.data.Escucha;
import com.example.autoque.data.PerfilRepository;
import com.example.autoque.data.Resultado;
import com.example.autoque.data.SolicitudRepository;
import com.example.autoque.databinding.ActivitySolicitarAsistenciaBinding;
import com.example.autoque.model.Catalogos;
import com.example.autoque.model.Incidente;
import com.example.autoque.model.Solicitud;
import com.example.autoque.model.Taller;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.ui.common.Chips;
import com.example.autoque.ui.common.MiniMapa;
import com.example.autoque.ui.common.TalleresEnMapa;
import com.example.autoque.util.Geo;
import com.example.autoque.util.Localizador;
import com.example.autoque.util.Ubicaciones;
import com.example.autoque.util.Validaciones;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class SolicitarAsistenciaActivity extends BaseActivity {

    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LON = "lon";
    public static final String EXTRA_DIRECCION = "direccion";
    public static final String EXTRA_DISTRITO = "distrito";

    private ActivitySolicitarAsistenciaBinding binding;
    private MiniMapa miniMapa;
    private SolicitudRepository solicitudes;
    private Double latitud;
    private Double longitud;
    private boolean direccionEditadaPorUsuario;
    private boolean distritoElegidoPorUsuario;
    private boolean autollenando;
    /** Ultima direccion y distrito detectados por el GPS (para comparar con lo escrito). */
    private String direccionGps;
    private String distritoGps;
    private static final double DISTANCIA_MAXIMA_KM = 1.0;
    /** Talleres del distrito elegido: son los que recibiran la solicitud. */
    private ListenerRegistration escuchaTalleres;
    private String distritoTalleres;
    private List<Taller> talleres = new ArrayList<>();
    private CancellationSignal cancelarUbicacion;

    private final ActivityResultLauncher<String[]> permisoUbicacion = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), r -> {
                if (Localizador.tienePermiso(this)) obtenerUbicacion();
                else binding.tvGpsEstado.setText(R.string.gps_sin_permiso);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirRol(Usuario.ROL_CLIENTE)) return;
        binding = ActivitySolicitarAsistenciaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);
        solicitudes = new SolicitudRepository();
        miniMapa = new MiniMapa(binding.mapa);

        binding.actvDistrito.setSimpleItems(Catalogos.DISTRITOS_LIMA.toArray(new String[0]));
        binding.actvDistrito.setOnItemClickListener((p, v, pos, id) -> {
            distritoElegidoPorUsuario = true;
            binding.tilDistrito.setError(null);
        });
        // Al cambiar el distrito (a mano o por GPS) se muestran sus talleres en el mapa
        binding.actvDistrito.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                escucharTalleres(s.toString());
            }
        });
        binding.tilDireccion.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!autollenando) direccionEditadaPorUsuario = true;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        Validaciones.limpiarErrorAlEscribir(binding.tilNombre, binding.tilTelefono,
                binding.tilDireccion, binding.tilDescripcion);

        crearChipsIncidente();
        autocompletarContacto();

        binding.btnUsarGps.setOnClickListener(v -> {
            // El boton siempre vuelve a llenar la direccion y el distrito con el GPS,
            // aunque el usuario los haya editado o borrado antes
            direccionEditadaPorUsuario = false;
            distritoElegidoPorUsuario = false;
            pedirUbicacion();
        });
        binding.btnEnviar.setOnClickListener(v -> enviar());

        Intent intent = getIntent();
        if (savedInstanceState == null && intent.hasExtra(EXTRA_LAT)) {
            mostrarUbicacion(intent.getDoubleExtra(EXTRA_LAT, 0), intent.getDoubleExtra(EXTRA_LON, 0));
            aplicarDireccion(intent.getStringExtra(EXTRA_DIRECCION), intent.getStringExtra(EXTRA_DISTRITO));
        } else if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_LAT)) {
            mostrarUbicacion(savedInstanceState.getDouble(EXTRA_LAT), savedInstanceState.getDouble(EXTRA_LON));
        } else {
            pedirUbicacion();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (miniMapa != null) miniMapa.onResume();
    }

    @Override
    protected void onPause() {
        if (miniMapa != null) miniMapa.onPause();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (latitud != null && longitud != null) {
            outState.putDouble(EXTRA_LAT, latitud);
            outState.putDouble(EXTRA_LON, longitud);
        }
    }

    @Override
    protected void onDestroy() {
        if (cancelarUbicacion != null) cancelarUbicacion.cancel();
        if (escuchaTalleres != null) escuchaTalleres.remove();
        if (miniMapa != null) miniMapa.onDestroy();
        super.onDestroy();
    }

    private void escucharTalleres(String distrito) {
        if (!Catalogos.DISTRITOS_LIMA.contains(distrito) || distrito.equals(distritoTalleres)) return;
        if (escuchaTalleres != null) escuchaTalleres.remove();
        distritoTalleres = distrito;
        escuchaTalleres = new PerfilRepository().escucharTalleresDelDistrito(distrito,
                new Escucha<>() {
                    @Override
                    public void onCambio(List<Taller> lista) {
                        talleres = lista;
                        pintarTalleres();
                    }
                });
    }

    private void pintarTalleres() {
        if (isFinishing() || isDestroyed() || miniMapa == null) return;
        miniMapa.mostrarLugares(TalleresEnMapa.lugares(this, talleres, latitud, longitud),
                uid -> TalleresEnMapa.abrirPerfil(this, uid));
    }

    private void autocompletarContacto() {
        binding.tilNombre.getEditText().setText(session.getNombre());
        new PerfilRepository().obtenerUsuario(session.getUid(), new Resultado<>() {
            @Override
            public void onExito(Usuario u) {
                if (u == null || isFinishing()) return;
                if (Validaciones.texto(binding.tilNombre).isEmpty()) {
                    binding.tilNombre.getEditText().setText(u.getNombre());
                }
                if (Validaciones.texto(binding.tilTelefono).isEmpty()) {
                    binding.tilTelefono.getEditText().setText(Validaciones.ultimos9Digitos(u.getTelefono()));
                }
            }

            @Override
            public void onError(int mensaje) {
            }
        });
    }

    private void crearChipsIncidente() {
        for (Incidente incidente : Incidente.values()) {
            binding.chipsIncidente.addView(Chips.crear(this, incidente.etiqueta, incidente));
        }
        binding.chipsIncidente.setOnCheckedStateChangeListener((group, ids) -> {
            binding.tvErrorIncidente.setVisibility(View.GONE);
            Incidente incidente = incidenteSeleccionado();
            if (incidente == null) {
                binding.tvCosto.setText("S/ —");
                binding.tvCostoSegun.setText(R.string.costo_sin_seleccion);
            } else {
                binding.tvCosto.setText(incidente.rangoCosto());
                binding.tvCostoSegun.setText(getString(R.string.costo_segun, incidente.etiqueta));
                if (incidente != Incidente.OTRO) binding.tilDescripcion.setError(null);
            }
        });
    }

    private Incidente incidenteSeleccionado() {
        int id = binding.chipsIncidente.getCheckedChipId();
        if (id == View.NO_ID) return null;
        return (Incidente) binding.chipsIncidente.findViewById(id).getTag();
    }

    private void pedirUbicacion() {
        if (Localizador.tienePermiso(this)) {
            obtenerUbicacion();
        } else {
            Localizador.marcarPermisoPedido(this);
            permisoUbicacion.launch(Localizador.PERMISOS);
        }
    }

    private void obtenerUbicacion() {
        if (Localizador.estado(this) == Localizador.Estado.GPS_APAGADO) {
            binding.tvGpsEstado.setText(R.string.gps_desactivado);
            return;
        }
        binding.progressGps.setVisibility(View.VISIBLE);
        binding.tvGpsEstado.setText(R.string.gps_buscando);
        if (cancelarUbicacion != null) cancelarUbicacion.cancel();
        cancelarUbicacion = Localizador.obtener(this, this::onUbicacion);
    }

    private void onUbicacion(Location location) {
        if (isFinishing() || isDestroyed()) return;
        binding.progressGps.setVisibility(View.GONE);
        if (location == null) {
            binding.tvGpsEstado.setText(R.string.gps_error);
            return;
        }
        mostrarUbicacion(location.getLatitude(), location.getLongitude());
        Localizador.direccion(this, latitud, longitud, this::aplicarDireccion);
    }

    private void mostrarUbicacion(double lat, double lon) {
        latitud = lat;
        longitud = lon;
        binding.tvGpsEstado.setText(getString(R.string.gps_ok, lat, lon));
        miniMapa.limpiar();
        miniMapa.marcar(lat, lon, getString(R.string.tu_estas_aqui), true);
        // limpiar() borra tambien los talleres: se vuelven a dibujar con la nueva distancia
        pintarTalleres();
    }

    private void aplicarDireccion(String direccion, String distrito) {
        if (isFinishing() || isDestroyed()) return;
        if (direccion != null) direccionGps = direccion;
        if (distrito != null) distritoGps = distrito;
        if (direccion != null && !direccionEditadaPorUsuario) {
            autollenando = true;
            binding.tilDireccion.getEditText().setText(direccion);
            autollenando = false;
        }
        if (distrito != null && !distritoElegidoPorUsuario) {
            binding.actvDistrito.setText(distrito, false);
            binding.tilDistrito.setError(null);
        }
    }

    private void enviar() {
        String obligatorio = getString(R.string.campo_obligatorio);
        boolean ok = Validaciones.requerido(binding.tilNombre, obligatorio);

        String telefono = Validaciones.texto(binding.tilTelefono);
        if (!Validaciones.esCelularValido(telefono)) {
            binding.tilTelefono.setError(getString(R.string.telefono_invalido));
            ok = false;
        } else {
            binding.tilTelefono.setError(null);
        }

        ok &= Validaciones.requerido(binding.tilDireccion, obligatorio);

        String distrito = binding.actvDistrito.getText().toString();
        if (!Catalogos.DISTRITOS_LIMA.contains(distrito)) {
            binding.tilDistrito.setError(getString(R.string.distrito_invalido));
            ok = false;
        } else {
            binding.tilDistrito.setError(null);
        }

        Incidente incidente = incidenteSeleccionado();
        if (incidente == null) {
            binding.tvErrorIncidente.setVisibility(View.VISIBLE);
            ok = false;
        }
        String descripcion = Validaciones.texto(binding.tilDescripcion);
        if (incidente == Incidente.OTRO && descripcion.isEmpty()) {
            binding.tilDescripcion.setError(getString(R.string.describe_obligatorio));
            ok = false;
        } else {
            binding.tilDescripcion.setError(null);
        }
        if (!ok) return;

        Solicitud s = new Solicitud();
        s.setClienteUid(session.getUid());
        s.setNombreContacto(Validaciones.texto(binding.tilNombre));
        s.setTelefonoContacto(Validaciones.formatearCelular(telefono));
        s.setDireccion(Validaciones.texto(binding.tilDireccion));
        s.setDistrito(distrito);
        s.setLatitud(latitud);
        s.setLongitud(longitud);
        s.setTipoIncidente(incidente.etiqueta);
        s.setDescripcion(descripcion.isEmpty() ? null : descripcion);
        s.setCostoMin(incidente.costoMin);
        s.setCostoMax(incidente.costoMax);

        cargando(binding.btnEnviar, true, R.string.enviar_solicitud);
        validarDireccionYEnviar(s);
    }

    /**
     * Compara la direccion escrita con la del GPS antes de enviar. Si el cliente no cambio la
     * direccion del GPS se envia directo; si la cambio, se valida y se usan sus coordenadas.
     * Si el servicio de direcciones no responde, se envia igual: una emergencia no se bloquea.
     */
    private void validarDireccionYEnviar(Solicitud s) {
        boolean hayGps = latitud != null && longitud != null;
        boolean direccionDelGps = hayGps && direccionGps != null && Ubicaciones.normalizar(
                s.getDireccion()).equals(Ubicaciones.normalizar(direccionGps));
        boolean distritoDistinto = distritoGps != null && !distritoGps.equals(s.getDistrito());

        if (direccionDelGps) {
            if (distritoDistinto) {
                // La direccion del GPS es de otro distrito: enviarla asi haria que la solicitud
                // le llegue a talleres que no estan cerca. Se exige escribir la direccion real.
                binding.tilDistrito.setError(getString(R.string.distrito_no_coincide_gps,
                        distritoGps));
                cargando(binding.btnEnviar, false, R.string.enviar_solicitud);
            } else {
                crear(s);
            }
            return;
        }
        Ubicaciones.validar(this, s.getDireccion(), s.getDistrito(), v -> {
            if (isFinishing() || isDestroyed()) return;
            switch (v.estado) {
                case NO_ENCONTRADA:
                    binding.tilDireccion.setError(getString(R.string.dir_no_encontrada,
                            s.getDistrito()));
                    cargando(binding.btnEnviar, false, R.string.enviar_solicitud);
                    break;
                case OTRO_DISTRITO:
                    binding.tilDistrito.setError(getString(R.string.dir_otro_distrito,
                            v.distritoEncontrado, s.getDistrito()));
                    cargando(binding.btnEnviar, false, R.string.enviar_solicitud);
                    break;
                case VALIDA:
                    Double km = Geo.distanciaKm(latitud, longitud, v.latitud, v.longitud);
                    if (km != null && km > DISTANCIA_MAXIMA_KM && direccionGps != null) {
                        elegirDireccion(s, v, km);
                    } else {
                        s.setLatitud(v.latitud);
                        s.setLongitud(v.longitud);
                        crear(s);
                    }
                    break;
                default:
                    // Sin internet no se puede validar la direccion escrita: solo se advierte
                    if (distritoDistinto) confirmarDistrito(s);
                    else crear(s);
            }
        });
    }

    private void elegirDireccion(Solicitud s, Ubicaciones.Validacion v, double km) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dir_lejos_titulo)
                .setMessage(getString(R.string.dir_lejos_msg, Geo.formatearKm(km), direccionGps))
                .setCancelable(false)
                .setPositiveButton(R.string.usar_la_escrita, (d, w) -> {
                    s.setLatitud(v.latitud);
                    s.setLongitud(v.longitud);
                    crear(s);
                })
                .setNegativeButton(R.string.usar_la_del_gps, (d, w) -> {
                    // Vuelve a poner la direccion del GPS para que el cliente la revise y envie
                    direccionEditadaPorUsuario = false;
                    distritoElegidoPorUsuario = false;
                    aplicarDireccion(direccionGps, distritoGps);
                    cargando(binding.btnEnviar, false, R.string.enviar_solicitud);
                })
                .show();
    }

    private void confirmarDistrito(Solicitud s) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.distrito_gps_titulo)
                .setMessage(getString(R.string.distrito_gps_msg, distritoGps, s.getDistrito()))
                .setCancelable(false)
                .setPositiveButton(R.string.enviar_igual, (d, w) -> crear(s))
                .setNegativeButton(getString(R.string.usar_distrito_gps, distritoGps), (d, w) -> {
                    distritoElegidoPorUsuario = false;
                    binding.actvDistrito.setText(distritoGps, false);
                    binding.tilDistrito.setError(null);
                    cargando(binding.btnEnviar, false, R.string.enviar_solicitud);
                })
                .show();
    }

    private void crear(Solicitud s) {
        solicitudes.crear(s, new Resultado<>() {
            @Override
            public void onExito(String id) {
                if (isFinishing()) return;
                Intent intent = new Intent(SolicitarAsistenciaActivity.this, EstadoSolicitudActivity.class);
                intent.putExtra(EstadoSolicitudActivity.EXTRA_SOLICITUD_ID, id);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(int mensaje) {
                if (isFinishing()) return;
                cargando(binding.btnEnviar, false, R.string.enviar_solicitud);
                Snackbar.make(binding.btnEnviar, mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
