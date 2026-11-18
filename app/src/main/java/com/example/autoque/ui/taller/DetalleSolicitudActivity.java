package com.example.autoque.ui.taller;

import android.os.Bundle;
import android.view.View;

import com.example.autoque.R;
import com.example.autoque.data.Escucha;
import com.example.autoque.data.PerfilRepository;
import com.example.autoque.data.SolicitudRepository;
import com.example.autoque.databinding.ActivityDetalleSolicitudBinding;
import com.example.autoque.model.Solicitud;
import com.example.autoque.model.Taller;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.ui.common.DetalleFilas;
import com.example.autoque.ui.common.MiniMapa;
import com.example.autoque.util.Geo;
import com.example.autoque.util.Intents;
import com.example.autoque.util.UiUtils;
import com.google.firebase.firestore.ListenerRegistration;

public class DetalleSolicitudActivity extends BaseActivity implements AceptacionSolicitud.Callback {

    public static final String EXTRA_SOLICITUD_ID = "solicitud_id";

    private ActivityDetalleSolicitudBinding binding;
    private MiniMapa miniMapa;
    private ListenerRegistration escuchaSolicitud;
    private ListenerRegistration escuchaTaller;
    private Solicitud solicitud;
    private Taller taller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirRol(Usuario.ROL_TALLER)) return;
        binding = ActivityDetalleSolicitudBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);
        miniMapa = new MiniMapa(binding.mapa);
        String solicitudId = getIntent().getStringExtra(EXTRA_SOLICITUD_ID);

        binding.btnAceptar.setOnClickListener(v -> {
            if (solicitud != null) AceptacionSolicitud.aceptar(this, solicitud, taller, this);
        });
        binding.btnMapa.setOnClickListener(v -> {
            if (solicitud != null) Intents.abrirMapa(this, solicitud);
        });

        escuchaTaller = new PerfilRepository().escucharTaller(session.getUid(), new Escucha<>() {
            @Override
            public void onCambio(Taller t) {
                taller = t;
                pintar();
            }
        });
        escuchaSolicitud = new SolicitudRepository().escuchar(solicitudId, new Escucha<>() {
            @Override
            public void onCambio(Solicitud s) {
                solicitud = s;
                pintar();
            }
        });
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
    protected void onDestroy() {
        if (escuchaSolicitud != null) escuchaSolicitud.remove();
        if (escuchaTaller != null) escuchaTaller.remove();
        if (miniMapa != null) miniMapa.onDestroy();
        super.onDestroy();
    }

    private void pintar() {
        if (solicitud == null) return;
        String uid = session.getUid();
        boolean pendiente = Solicitud.PENDIENTE.equals(solicitud.getEstado());
        if (!pendiente && !solicitud.asignadaA(uid)) {
            finish();
            return;
        }

        binding.tvIniciales.setText(UiUtils.iniciales(solicitud.getNombreContacto()));
        binding.tvCliente.setText(solicitud.getNombreContacto());
        binding.tvNumero.setText(getString(R.string.solicitud_numero_hora, solicitud.getNumero(),
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

        binding.cardMapa.setVisibility(solicitud.tieneUbicacion() ? View.VISIBLE : View.GONE);
        if (solicitud.tieneUbicacion()) {
            miniMapa.limpiar();
            miniMapa.marcar(solicitud.getLatitud(), solicitud.getLongitud(),
                    getString(R.string.cliente_label), true);
            if (taller != null && taller.tieneUbicacion()) {
                miniMapa.marcar(taller.getLatitud(), taller.getLongitud(), taller.getNombreTaller(), false);
            }
        }
        Double km = taller == null ? null : Geo.distanciaKm(solicitud.getLatitud(), solicitud.getLongitud(),
                taller.getLatitud(), taller.getLongitud());
        binding.tvDistancia.setVisibility(km != null ? View.VISIBLE : View.GONE);
        if (km != null) binding.tvDistancia.setText(getString(R.string.a_distancia_taller, Geo.formatearKm(km)));

        binding.btnMapa.setVisibility(View.VISIBLE);
        binding.panelAceptar.setVisibility(pendiente ? View.VISIBLE : View.GONE);
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
