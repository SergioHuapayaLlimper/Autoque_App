package com.example.autoque.ui.cliente;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.autoque.R;
import com.example.autoque.data.SolicitudDao;
import com.example.autoque.databinding.ActivityEstadoSolicitudBinding;
import com.example.autoque.model.Solicitud;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.ui.common.DetalleFilas;
import com.example.autoque.util.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class EstadoSolicitudActivity extends BaseActivity {

    public static final String EXTRA_SOLICITUD_ID = "solicitud_id";
    private static final long INTERVALO_REFRESCO_MS = 5_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresco = new Runnable() {
        @Override
        public void run() {
            if (cargar()) handler.postDelayed(this, INTERVALO_REFRESCO_MS);
        }
    };

    private ActivityEstadoSolicitudBinding binding;
    private SolicitudDao solicitudDao;
    private long solicitudId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirRol(Usuario.ROL_CLIENTE)) return;
        binding = ActivityEstadoSolicitudBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);
        solicitudDao = new SolicitudDao(this);
        solicitudId = getIntent().getLongExtra(EXTRA_SOLICITUD_ID, -1);
        binding.btnCancelar.setOnClickListener(v -> confirmarCancelacion());
    }

    private void confirmarCancelacion() {
        Solicitud s = solicitudDao.obtener(solicitudId);
        if (s == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cancelar_confirmar_titulo)
                .setMessage(getString(R.string.cancelar_confirmar_msg, s.getDistrito()))
                .setNegativeButton(R.string.no_cancelar, null)
                .setPositiveButton(R.string.si_cancelar, (d, w) -> {
                    if (solicitudDao.cancelar(solicitudId, session.getUsuarioId())) {
                        Toast.makeText(this, R.string.solicitud_cancelada, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, R.string.no_se_pudo_cancelar, Toast.LENGTH_LONG).show();
                        cargar();
                    }
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (solicitudDao != null) handler.post(refresco);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refresco);
        super.onPause();
    }

    private boolean cargar() {
        Solicitud s = solicitudDao.obtener(solicitudId);
        if (s == null || s.getClienteId() != session.getUsuarioId()
                || Solicitud.CANCELADA.equals(s.getEstado())) {
            finish();
            return false;
        }
        binding.panelCancelar.setVisibility(
                Solicitud.PENDIENTE.equals(s.getEstado()) ? View.VISIBLE : View.GONE);
        UiUtils.pintarEstado(binding.tvBadge, s.getEstado());

        int pasoActual;
        switch (s.getEstado()) {
            case Solicitud.ACEPTADA:
                pasoActual = 2;
                binding.tvTitulo.setText(getString(R.string.taller_acepto, s.getNombreTaller()));
                binding.tvSubtitulo.setText(R.string.taller_acepto_desc);
                binding.ivEstado.setImageResource(R.drawable.ic_wrench);
                binding.progressBuscando.setVisibility(View.GONE);
                break;
            case Solicitud.FINALIZADA:
                pasoActual = 3;
                binding.tvTitulo.setText(R.string.atencion_finalizada);
                binding.tvSubtitulo.setText(R.string.atencion_finalizada_desc);
                binding.ivEstado.setImageResource(R.drawable.ic_check);
                binding.progressBuscando.setVisibility(View.GONE);
                break;
            default:
                pasoActual = 1;
                binding.tvTitulo.setText(getString(R.string.buscando_taller, s.getDistrito()));
                binding.tvSubtitulo.setText(R.string.buscando_taller_desc);
                binding.ivEstado.setImageResource(R.drawable.ic_search);
                binding.progressBuscando.setVisibility(View.VISIBLE);
        }
        pintarPasos(pasoActual);

        DetalleFilas filas = new DetalleFilas(binding.contenedorDetalle)
                .fila(R.string.solicitud, getString(R.string.solicitud_numero_hora,
                        s.getId(), UiUtils.hora(s.getCreadoEn())))
                .fila(R.string.incidente, s.getTipoIncidente())
                .fila(R.string.detalle, s.getDescripcion())
                .fila(R.string.direccion, s.getDireccion())
                .fila(R.string.distrito_label, s.getDistrito());
        if (s.getNombreTaller() != null) filas.fila(R.string.taller_label, s.getNombreTaller());
        filas.separador().fila(R.string.costo_aproximado, s.rangoCosto());
        return !Solicitud.FINALIZADA.equals(s.getEstado());
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
