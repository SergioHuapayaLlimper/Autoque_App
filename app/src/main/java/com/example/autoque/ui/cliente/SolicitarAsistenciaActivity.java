package com.example.autoque.ui.cliente;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.autoque.R;
import com.example.autoque.data.SolicitudDao;
import com.example.autoque.data.UsuarioDao;
import com.example.autoque.databinding.ActivitySolicitarAsistenciaBinding;
import com.example.autoque.model.Catalogos;
import com.example.autoque.model.Incidente;
import com.example.autoque.model.Solicitud;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.util.Validaciones;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SolicitarAsistenciaActivity extends BaseActivity {

    private static final String STATE_LAT = "lat";
    private static final String STATE_LON = "lon";

    private ActivitySolicitarAsistenciaBinding binding;
    private TextInputLayout tilNombre;
    private TextInputLayout tilTelefono;
    private TextInputLayout tilDireccion;
    private TextInputLayout tilDistrito;
    private MaterialAutoCompleteTextView actvDistrito;
    private TextInputLayout tilDescripcion;
    private ChipGroup chipsIncidente;
    private TextView tvErrorIncidente;
    private TextView tvCosto;
    private TextView tvCostoSegun;
    private TextView tvGpsEstado;
    private View progressGps;

    private Double latitud;
    private Double longitud;
    private boolean direccionEditadaPorUsuario;
    private boolean distritoElegidoPorUsuario;
    private boolean autollenando;
    private CancellationSignal cancelarUbicacion;
    private SolicitudDao solicitudDao;

    private final ActivityResultLauncher<String[]> permisoUbicacion = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), this::onResultadoPermiso);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirRol(Usuario.ROL_CLIENTE)) return;
        binding = ActivitySolicitarAsistenciaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);
        solicitudDao = new SolicitudDao(this);

        tilNombre = binding.tilNombre;
        tilTelefono = binding.tilTelefono;
        tilDireccion = binding.tilDireccion;
        tilDistrito = binding.tilDistrito;
        actvDistrito = binding.actvDistrito;
        tilDescripcion = binding.tilDescripcion;
        chipsIncidente = binding.chipsIncidente;
        tvErrorIncidente = binding.tvErrorIncidente;
        tvCosto = binding.tvCosto;
        tvCostoSegun = binding.tvCostoSegun;
        tvGpsEstado = binding.tvGpsEstado;
        progressGps = binding.progressGps;

        actvDistrito.setSimpleItems(Catalogos.DISTRITOS_LIMA.toArray(new String[0]));
        actvDistrito.setOnItemClickListener((p, v, pos, id) -> {
            distritoElegidoPorUsuario = true;
            tilDistrito.setError(null);
        });
        tilDireccion.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!autollenando) direccionEditadaPorUsuario = true;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        Validaciones.limpiarErrorAlEscribir(tilNombre, tilTelefono, tilDireccion, tilDescripcion);

        crearChipsIncidente();
        autocompletarContacto();

        binding.btnUsarGps.setOnClickListener(v -> pedirUbicacion());
        binding.btnEnviar.setOnClickListener(v -> enviar());

        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_LAT)) {
            latitud = savedInstanceState.getDouble(STATE_LAT);
            longitud = savedInstanceState.getDouble(STATE_LON);
            tvGpsEstado.setText(getString(R.string.gps_ok, latitud, longitud));
        } else {
            pedirUbicacion();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (latitud != null && longitud != null) {
            outState.putDouble(STATE_LAT, latitud);
            outState.putDouble(STATE_LON, longitud);
        }
    }

    @Override
    protected void onDestroy() {
        if (cancelarUbicacion != null) cancelarUbicacion.cancel();
        super.onDestroy();
    }

    private void autocompletarContacto() {
        Usuario usuario = new UsuarioDao(this).obtenerUsuario(session.getUsuarioId());
        if (usuario == null) return;
        tilNombre.getEditText().setText(usuario.getNombre());
        String digitos = Validaciones.soloDigitos(usuario.getTelefono());
        if (digitos.length() > 9) digitos = digitos.substring(digitos.length() - 9);
        tilTelefono.getEditText().setText(digitos);
    }

    private void crearChipsIncidente() {
        for (Incidente incidente : Incidente.values()) {
            Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
            chip.setId(View.generateViewId());
            chip.setTag(incidente);
            chip.setText(incidente.etiqueta);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            chip.setChipBackgroundColorResource(R.color.chip_bg);
            chip.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_text));
            chip.setChipStrokeColorResource(R.color.outline);
            chip.setChipStrokeWidth(getResources().getDisplayMetrics().density);
            chip.setCheckedIconResource(R.drawable.ic_check);
            chip.setCheckedIconTintResource(R.color.autoque_yellow);
            chip.setChipMinHeight(44 * getResources().getDisplayMetrics().density);
            chip.setShapeAppearanceModel(chip.getShapeAppearanceModel()
                    .withCornerSize(22 * getResources().getDisplayMetrics().density));
            chipsIncidente.addView(chip);
        }
        chipsIncidente.setOnCheckedStateChangeListener((group, ids) -> {
            tvErrorIncidente.setVisibility(View.GONE);
            Incidente incidente = incidenteSeleccionado();
            if (incidente == null) {
                tvCosto.setText("S/ —");
                tvCostoSegun.setText(R.string.costo_sin_seleccion);
            } else {
                tvCosto.setText(incidente.rangoCosto());
                tvCostoSegun.setText(getString(R.string.costo_segun, incidente.etiqueta));
                if (incidente != Incidente.OTRO) tilDescripcion.setError(null);
            }
        });
    }

    private Incidente incidenteSeleccionado() {
        int id = chipsIncidente.getCheckedChipId();
        if (id == View.NO_ID) return null;
        return (Incidente) chipsIncidente.findViewById(id).getTag();
    }

    private boolean tienePermisoUbicacion() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void pedirUbicacion() {
        if (tienePermisoUbicacion()) {
            obtenerUbicacion();
        } else {
            permisoUbicacion.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private void onResultadoPermiso(Map<String, Boolean> resultado) {
        if (tienePermisoUbicacion()) {
            obtenerUbicacion();
        } else {
            tvGpsEstado.setText(R.string.gps_sin_permiso);
        }
    }

    @SuppressLint("MissingPermission")
    private void obtenerUbicacion() {
        if (!tienePermisoUbicacion()) return;
        LocationManager lm = getSystemService(LocationManager.class);
        if (lm == null || !lm.isLocationEnabled()) {
            tvGpsEstado.setText(R.string.gps_desactivado);
            return;
        }
        String proveedor;
        if (lm.hasProvider(LocationManager.FUSED_PROVIDER)
                && lm.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
            proveedor = LocationManager.FUSED_PROVIDER;
        } else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            proveedor = LocationManager.GPS_PROVIDER;
        } else {
            proveedor = LocationManager.NETWORK_PROVIDER;
        }

        progressGps.setVisibility(View.VISIBLE);
        tvGpsEstado.setText(R.string.gps_buscando);
        if (cancelarUbicacion != null) cancelarUbicacion.cancel();
        cancelarUbicacion = new CancellationSignal();
        lm.getCurrentLocation(proveedor, cancelarUbicacion, ContextCompat.getMainExecutor(this),
                location -> {
                    if (location == null) location = lm.getLastKnownLocation(proveedor);
                    onUbicacion(location);
                });
    }

    private void onUbicacion(Location location) {
        if (isFinishing() || isDestroyed()) return;
        progressGps.setVisibility(View.GONE);
        if (location == null) {
            tvGpsEstado.setText(R.string.gps_error);
            return;
        }
        latitud = location.getLatitude();
        longitud = location.getLongitude();
        tvGpsEstado.setText(getString(R.string.gps_ok, latitud, longitud));
        buscarDireccion(latitud, longitud);
    }

    private void buscarDireccion(double lat, double lon) {
        if (!Geocoder.isPresent()) return;
        Geocoder geocoder = new Geocoder(this, Locale.forLanguageTag("es-PE"));
        geocoder.getFromLocation(lat, lon, 1, new Geocoder.GeocodeListener() {
            @Override
            public void onGeocode(@NonNull List<Address> direcciones) {
                if (direcciones.isEmpty()) return;
                runOnUiThread(() -> aplicarDireccion(direcciones.get(0)));
            }

            @Override
            public void onError(String mensaje) {
            }
        });
    }

    private void aplicarDireccion(Address a) {
        if (isFinishing() || isDestroyed()) return;
        String calle = a.getThoroughfare();
        String direccion;
        if (calle != null) {
            direccion = a.getSubThoroughfare() != null ? calle + " " + a.getSubThoroughfare() : calle;
        } else if (a.getAddressLine(0) != null) {
            direccion = a.getAddressLine(0).split(",")[0];
        } else {
            direccion = null;
        }
        if (direccion != null && !direccionEditadaPorUsuario) {
            autollenando = true;
            tilDireccion.getEditText().setText(direccion);
            autollenando = false;
        }

        String distrito = Catalogos.buscarDistrito(a.getSubLocality());
        if (distrito == null) distrito = Catalogos.buscarDistrito(a.getLocality());
        if (distrito == null) distrito = Catalogos.buscarDistrito(a.getSubAdminArea());
        if (distrito != null && !distritoElegidoPorUsuario) {
            actvDistrito.setText(distrito, false);
            tilDistrito.setError(null);
        }
    }

    private void enviar() {
        String obligatorio = getString(R.string.campo_obligatorio);
        boolean ok = Validaciones.requerido(tilNombre, obligatorio);

        String telefono = Validaciones.texto(tilTelefono);
        if (!Validaciones.esCelularValido(telefono)) {
            tilTelefono.setError(getString(R.string.telefono_invalido));
            ok = false;
        } else {
            tilTelefono.setError(null);
        }

        ok &= Validaciones.requerido(tilDireccion, obligatorio);

        String distrito = actvDistrito.getText().toString();
        if (!Catalogos.DISTRITOS_LIMA.contains(distrito)) {
            tilDistrito.setError(getString(R.string.distrito_invalido));
            ok = false;
        } else {
            tilDistrito.setError(null);
        }

        Incidente incidente = incidenteSeleccionado();
        if (incidente == null) {
            tvErrorIncidente.setVisibility(View.VISIBLE);
            ok = false;
        }
        String descripcion = Validaciones.texto(tilDescripcion);
        if (incidente == Incidente.OTRO && descripcion.isEmpty()) {
            tilDescripcion.setError(getString(R.string.describe_obligatorio));
            ok = false;
        } else {
            tilDescripcion.setError(null);
        }
        if (!ok) return;

        Solicitud activa = solicitudDao.solicitudActivaDelCliente(session.getUsuarioId());
        if (activa != null) {
            abrirEstado(activa.getId());
            return;
        }

        Solicitud s = new Solicitud();
        s.setClienteId(session.getUsuarioId());
        s.setNombreContacto(Validaciones.texto(tilNombre));
        s.setTelefonoContacto(Validaciones.formatearCelular(telefono));
        s.setDireccion(Validaciones.texto(tilDireccion));
        s.setDistrito(distrito);
        s.setLatitud(latitud);
        s.setLongitud(longitud);
        s.setTipoIncidente(incidente.etiqueta);
        s.setDescripcion(descripcion.isEmpty() ? null : descripcion);
        s.setCostoMin(incidente.costoMin);
        s.setCostoMax(incidente.costoMax);

        long id = solicitudDao.crear(s);
        if (id != -1) abrirEstado(id);
    }

    private void abrirEstado(long solicitudId) {
        Intent intent = new Intent(this, EstadoSolicitudActivity.class);
        intent.putExtra(EstadoSolicitudActivity.EXTRA_SOLICITUD_ID, solicitudId);
        startActivity(intent);
        finish();
    }
}
