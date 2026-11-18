package com.example.autoque.ui.cliente;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.autoque.R;
import com.example.autoque.data.Escucha;
import com.example.autoque.data.PerfilRepository;
import com.example.autoque.data.Resultado;
import com.example.autoque.data.SolicitudRepository;
import com.example.autoque.databinding.ActivityEstadoSolicitudBinding;
import com.example.autoque.model.Solicitud;
import com.example.autoque.model.Taller;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.ui.common.DetalleFilas;
import com.example.autoque.ui.common.MiniMapa;
import com.example.autoque.util.Geo;
import com.example.autoque.util.Intents;
import com.example.autoque.util.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;

public class EstadoSolicitudActivity extends BaseActivity {

    public static final String EXTRA_SOLICITUD_ID = "solicitud_id";

    private ActivityEstadoSolicitudBinding binding;
    private SolicitudRepository solicitudes;
    private MiniMapa miniMapa;
    private ListenerRegistration escuchaSolicitud;
    private ListenerRegistration escuchaTaller;
    private String solicitudId;
    private String tallerEscuchado;
    private Solicitud solicitud;
    private Taller taller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirRol(Usuario.ROL_CLIENTE)) return;
        binding = ActivityEstadoSolicitudBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);
        solicitudes = new SolicitudRepository();
        miniMapa = new MiniMapa(binding.mapa);
        solicitudId = getIntent().getStringExtra(EXTRA_SOLICITUD_ID);

        binding.btnCancelar.setOnClickListener(v -> confirmarCancelacion());
        binding.btnCalificar.setOnClickListener(v -> {
            Intent intent = new Intent(this, CalificarTallerActivity.class);
            intent.putExtra(CalificarTallerActivity.EXTRA_SOLICITUD_ID, solicitudId);
            startActivity(intent);
        });
        binding.btnLlamarTaller.setOnClickListener(v -> {
            if (solicitud != null && solicitud.getTelefonoTaller() != null) {
                Intents.llamar(this, solicitud.getTelefonoTaller());
            }
        });
        binding.btnVerPerfilTaller.setOnClickListener(v -> {
            if (solicitud == null || solicitud.getTallerUid() == null) return;
            Intent intent = new Intent(this, PerfilTallerActivity.class);
            intent.putExtra(PerfilTallerActivity.EXTRA_TALLER_UID, solicitud.getTallerUid());
            startActivity(intent);
        });

        escuchaSolicitud = solicitudes.escuchar(solicitudId, new Escucha<>() {
            @Override
            public void onCambio(Solicitud s) {
                pintar(s);
            }

            @Override
            public void onError(int mensaje) {
                Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
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

    private void confirmarCancelacion() {
        if (solicitud == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cancelar_confirmar_titulo)
                .setMessage(getString(R.string.cancelar_confirmar_msg, solicitud.getDistrito()))
                .setNegativeButton(R.string.no_cancelar, null)
                .setPositiveButton(R.string.si_cancelar, (d, w) ->
                        solicitudes.cancelar(solicitudId, session.getUid(), new Resultado<>() {
                            @Override
                            public void onExito(Boolean cancelada) {
                                if (isFinishing()) return;
                                if (cancelada) {
                                    Toast.makeText(EstadoSolicitudActivity.this,
                                            R.string.solicitud_cancelada, Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(EstadoSolicitudActivity.this,
                                            R.string.no_se_pudo_cancelar, Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onError(int mensaje) {
                                Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
                            }
                        }))
                .show();
    }

    private void pintar(Solicitud s) {
        if (s == null || !session.getUid().equals(s.getClienteUid())) {
            finish();
            return;
        }
        solicitud = s;
        boolean pendiente = Solicitud.PENDIENTE.equals(s.getEstado());
        boolean aceptada = Solicitud.ACEPTADA.equals(s.getEstado());
        boolean finalizada = Solicitud.FINALIZADA.equals(s.getEstado());
        boolean cancelada = Solicitud.CANCELADA.equals(s.getEstado());

        binding.toolbar.setTitle(aceptada ? R.string.seguimiento : R.string.estado_solicitud);
        binding.panelCancelar.setVisibility(pendiente ? View.VISIBLE : View.GONE);
        binding.btnCalificar.setVisibility(finalizada && !s.isCalificada() ? View.VISIBLE : View.GONE);
        UiUtils.pintarEstado(binding.tvBadge, s.getEstado());

        int paso;
        if (aceptada) {
            paso = 2;
            binding.tvTitulo.setText(R.string.va_en_camino);
            binding.tvSubtitulo.setText(getString(R.string.taller_acepto, s.getNombreTaller()));
            binding.ivEstado.setImageResource(R.drawable.ic_wrench);
            binding.progressBuscando.setVisibility(View.GONE);
        } else if (finalizada) {
            paso = 3;
            binding.tvTitulo.setText(R.string.atencion_finalizada);
            binding.tvSubtitulo.setText(R.string.atencion_finalizada_desc);
            binding.ivEstado.setImageResource(R.drawable.ic_check);
            binding.progressBuscando.setVisibility(View.GONE);
        } else if (cancelada) {
            paso = 0;
            binding.tvTitulo.setText(R.string.solicitud_cancelada);
            binding.tvSubtitulo.setText("");
            binding.ivEstado.setImageResource(R.drawable.ic_alert);
            binding.progressBuscando.setVisibility(View.GONE);
        } else {
            paso = 1;
            binding.tvTitulo.setText(getString(R.string.buscando_taller, s.getDistrito()));
            binding.tvSubtitulo.setText(R.string.buscando_taller_desc);
            binding.ivEstado.setImageResource(R.drawable.ic_search);
            binding.progressBuscando.setVisibility(View.VISIBLE);
        }
        pintarPasos(paso);

        DetalleFilas filas = new DetalleFilas(binding.contenedorDetalle)
                .fila(R.string.solicitud, getString(R.string.solicitud_numero_hora,
                        s.getNumero(), UiUtils.hora(s.getCreadoEn())))
                .fila(R.string.incidente, s.getTipoIncidente())
                .fila(R.string.detalle, s.getDescripcion())
                .fila(R.string.direccion, s.getDireccion())
                .fila(R.string.distrito_label, s.getDistrito());
        filas.separador().fila(R.string.costo_aproximado, s.rangoCosto());

        if (s.getTallerUid() != null && (aceptada || finalizada)) {
            binding.cardTaller.setVisibility(View.VISIBLE);
            binding.btnLlamarTaller.setVisibility(aceptada ? View.VISIBLE : View.GONE);
            binding.tvNombreTaller.setText(s.getNombreTaller());
            binding.tvInicialesTaller.setText(UiUtils.iniciales(s.getNombreTaller()));
            escucharTaller(s.getTallerUid());
        } else {
            binding.cardTaller.setVisibility(View.GONE);
        }
        pintarMapaYLlegada();
    }

    private void escucharTaller(String uid) {
        if (uid.equals(tallerEscuchado)) return;
        if (escuchaTaller != null) escuchaTaller.remove();
        tallerEscuchado = uid;
        escuchaTaller = new PerfilRepository().escucharTaller(uid, new Escucha<>() {
            @Override
            public void onCambio(Taller t) {
                taller = t;
                binding.tvCalificacionTaller.setText(t.getTotalResenas() == 0
                        ? getString(R.string.sin_calificaciones)
                        : UiUtils.estrellas(t.promedio()) + "  " + UiUtils.promedio(t.promedio())
                        + " (" + t.getTotalResenas() + ")");
                binding.tvDireccionTaller.setText(t.getDireccion() + " · " + t.getDistrito());
                pintarMapaYLlegada();
            }
        });
    }

    private void pintarMapaYLlegada() {
        if (solicitud == null) return;
        boolean aceptada = Solicitud.ACEPTADA.equals(solicitud.getEstado());
        boolean hayCliente = solicitud.tieneUbicacion() && solicitud.activa();
        binding.cardMapa.setVisibility(hayCliente ? View.VISIBLE : View.GONE);
        if (hayCliente) {
            miniMapa.limpiar();
            miniMapa.marcar(solicitud.getLatitud(), solicitud.getLongitud(), getString(R.string.tu_estas_aqui), true);
            if (aceptada && taller != null && taller.tieneUbicacion()) {
                miniMapa.marcar(taller.getLatitud(), taller.getLongitud(), taller.getNombreTaller(), false);
            }
        }
        Double km = taller == null ? null : Geo.distanciaKm(solicitud.getLatitud(), solicitud.getLongitud(),
                taller.getLatitud(), taller.getLongitud());
        if (aceptada && km != null) {
            int[] eta = Geo.llegadaEstimadaMin(km);
            binding.tvLlegada.setText(getString(R.string.llegada_estimada, eta[0], eta[1]) + "  ·  "
                    + getString(R.string.taller_en_camino_km, Geo.formatearKm(km)));
            binding.tvLlegada.setVisibility(View.VISIBLE);
        } else {
            binding.tvLlegada.setVisibility(View.GONE);
        }
    }

    private void pintarPasos(int pasoActual) {
        View[] puntos = {binding.paso1, binding.paso2, binding.paso3};
        TextView[] textos = {binding.tvPaso1, binding.tvPaso2, binding.tvPaso3};
        for (int i = 0; i < 3; i++) {
            boolean alcanzado = i < pasoActual;
            puntos[i].setBackgroundResource(
                    alcanzado ? R.drawable.bg_circle_black : R.drawable.bg_step_inactive);
            textos[i].setTextColor(ContextCompat.getColor(this,
                    alcanzado ? R.color.text_primary : R.color.text_hint));
            textos[i].setTypeface(i == pasoActual - 1 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }
    }
}
