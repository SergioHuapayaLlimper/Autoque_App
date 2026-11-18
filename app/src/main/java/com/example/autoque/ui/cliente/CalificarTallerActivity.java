package com.example.autoque.ui.cliente;

import android.os.Bundle;
import android.widget.Toast;

import com.example.autoque.R;
import com.example.autoque.data.ResenaRepository;
import com.example.autoque.data.Resultado;
import com.example.autoque.data.SolicitudRepository;
import com.example.autoque.databinding.ActivityCalificarTallerBinding;
import com.example.autoque.model.Resena;
import com.example.autoque.model.Solicitud;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.util.UiUtils;
import com.example.autoque.util.Validaciones;
import com.google.android.material.snackbar.Snackbar;

public class CalificarTallerActivity extends BaseActivity {

    public static final String EXTRA_SOLICITUD_ID = "solicitud_id";

    private static final int[] ETIQUETAS = {R.string.elige_estrellas, R.string.estrellas_1,
            R.string.estrellas_2, R.string.estrellas_3, R.string.estrellas_4, R.string.estrellas_5};

    private ActivityCalificarTallerBinding binding;
    private String solicitudId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirRol(Usuario.ROL_CLIENTE)) return;
        binding = ActivityCalificarTallerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);
        solicitudId = getIntent().getStringExtra(EXTRA_SOLICITUD_ID);

        binding.ratingBar.setOnRatingBarChangeListener((bar, valor, usuario) ->
                binding.tvEtiquetaEstrellas.setText(ETIQUETAS[Math.round(valor)]));
        binding.btnEnviar.setOnClickListener(v -> enviar());

        new SolicitudRepository().obtener(solicitudId, new Resultado<>() {
            @Override
            public void onExito(Solicitud s) {
                if (s == null || isFinishing()) return;
                binding.tvTaller.setText(s.getNombreTaller());
                binding.tvIniciales.setText(UiUtils.iniciales(s.getNombreTaller()));
                long fecha = s.getFinalizadoEn() != null ? s.getFinalizadoEn() : s.getCreadoEn();
                binding.tvResumen.setText(getString(R.string.finalizada_resumen,
                        s.getTipoIncidente(), UiUtils.fecha(fecha)));
                if (s.isCalificada()) {
                    binding.btnEnviar.setEnabled(false);
                    binding.btnEnviar.setText(R.string.ya_calificada);
                }
            }

            @Override
            public void onError(int mensaje) {
                Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void enviar() {
        int estrellas = Math.round(binding.ratingBar.getRating());
        if (estrellas < 1) {
            Snackbar.make(binding.btnEnviar, R.string.elige_estrellas, Snackbar.LENGTH_SHORT).show();
            return;
        }
        String comentario = Validaciones.texto(binding.tilComentario);
        Resena resena = new Resena(session.getUid(), session.getNombre(), estrellas,
                comentario.isEmpty() ? null : comentario);

        cargando(binding.btnEnviar, true, R.string.enviar_calificacion);
        new ResenaRepository().calificarTaller(solicitudId, resena, new Resultado<>() {
            @Override
            public void onExito(Boolean ok) {
                if (isFinishing()) return;
                Toast.makeText(CalificarTallerActivity.this,
                        ok ? R.string.calificacion_enviada : R.string.ya_calificada, Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(int mensaje) {
                if (isFinishing()) return;
                cargando(binding.btnEnviar, false, R.string.enviar_calificacion);
                Snackbar.make(binding.btnEnviar, mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
