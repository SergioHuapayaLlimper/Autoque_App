package com.example.autoque.ui.taller;

import android.os.Bundle;
import android.view.View;

import com.example.autoque.R;
import com.example.autoque.data.SolicitudDao;
import com.example.autoque.databinding.ActivityDetalleSolicitudBinding;
import com.example.autoque.model.Solicitud;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.ui.common.DetalleFilas;
import com.example.autoque.util.Intents;
import com.example.autoque.util.UiUtils;

public class DetalleSolicitudActivity extends BaseActivity implements AceptacionSolicitud.Callback {

    public static final String EXTRA_SOLICITUD_ID = "solicitud_id";

    private ActivityDetalleSolicitudBinding binding;
    private SolicitudDao solicitudDao;
    private long solicitudId;
    private Solicitud solicitud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirRol(Usuario.ROL_TALLER)) return;
        binding = ActivityDetalleSolicitudBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);
        solicitudDao = new SolicitudDao(this);
        solicitudId = getIntent().getLongExtra(EXTRA_SOLICITUD_ID, -1);

        binding.btnAceptar.setOnClickListener(v -> {
            if (solicitud != null) {
                AceptacionSolicitud.aceptar(this, solicitud, session.getUsuarioId(), this);
            }
        });
        binding.btnMapa.setOnClickListener(v -> {
            if (solicitud != null) Intents.abrirMapa(this, solicitud);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (solicitudDao != null) cargar();
    }

    private void cargar() {
        solicitud = solicitudDao.obtener(solicitudId);
        long tallerId = session.getUsuarioId();
        if (solicitud == null || (!Solicitud.PENDIENTE.equals(solicitud.getEstado())
                && !solicitud.asignadaA(tallerId))) {
            finish();
            return;
        }
        boolean pendiente = Solicitud.PENDIENTE.equals(solicitud.getEstado());

        binding.tvIniciales.setText(UiUtils.iniciales(solicitud.getNombreContacto()));
        binding.tvCliente.setText(solicitud.getNombreContacto());
        binding.tvNumero.setText(getString(R.string.solicitud_numero_hora, solicitud.getId(),
                UiUtils.haceCuanto(solicitud.getCreadoEn())));
        UiUtils.pintarEstado(binding.tvEstado, solicitud.getEstado());

        new DetalleFilas(binding.contenedorDetalle)
                .fila(R.string.incidente, solicitud.getTipoIncidente())
                .fila(R.string.detalle, solicitud.getDescripcion())
                .fila(R.string.direccion, solicitud.getDireccion())
                .fila(R.string.distrito_label, solicitud.getDistrito())
                .fila(R.string.telefono_label, pendiente
                        ? getString(R.string.visible_al_aceptar) : solicitud.getTelefonoContacto())
                .separador()
                .fila(R.string.costo_aproximado, solicitud.rangoCosto(), true);

        binding.btnMapa.setVisibility(solicitud.tieneUbicacion() ? View.VISIBLE : View.GONE);
        binding.panelAceptar.setVisibility(pendiente ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onListaCambio() {
        solicitud = solicitudDao.obtener(solicitudId);
        if (solicitud != null && solicitud.asignadaA(session.getUsuarioId())) {
            cargar();
        }
    }

    @Override
    public void onNoDisponible() {
        finish();
    }

    @Override
    public void onVerAtencion() {
        TallerHomeActivity.abrirAtenciones(this);
        finish();
    }
}
