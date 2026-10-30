package com.example.autoque.ui.cliente;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.autoque.R;
import com.example.autoque.data.SolicitudDao;
import com.example.autoque.data.UsuarioDao;
import com.example.autoque.databinding.ActivityClienteHomeBinding;
import com.example.autoque.model.Solicitud;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.util.UiUtils;
import com.google.android.material.snackbar.Snackbar;

public class ClienteHomeActivity extends BaseActivity {

    private ActivityClienteHomeBinding binding;
    private SolicitudDao solicitudDao;
    private Solicitud solicitudActiva;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirRol(Usuario.ROL_CLIENTE)) return;
        binding = ActivityClienteHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        solicitudDao = new SolicitudDao(this);

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                cerrarSesion();
                return true;
            }
            return false;
        });

        Usuario usuario = new UsuarioDao(this).obtenerUsuario(session.getUsuarioId());
        if (usuario == null) {
            cerrarSesion();
            return;
        }
        binding.tvSaludo.setText(getString(R.string.hola_nombre, usuario.primerNombre()));

        binding.cardActiva.setOnClickListener(v -> abrirEstado());
        binding.btnVerEstado.setOnClickListener(v -> abrirEstado());
        binding.btnSolicitar.setOnClickListener(v -> solicitar());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (solicitudDao == null) return;
        solicitudActiva = solicitudDao.solicitudActivaDelCliente(session.getUsuarioId());
        if (solicitudActiva == null) {
            binding.cardActiva.setVisibility(View.GONE);
            return;
        }
        binding.cardActiva.setVisibility(View.VISIBLE);
        UiUtils.pintarEstado(binding.tvActivaEstado, solicitudActiva.getEstado());
        binding.tvActivaDetalle.setText(solicitudActiva.getTipoIncidente() + " · "
                + solicitudActiva.getDireccion() + ", " + solicitudActiva.getDistrito());
    }

    private void solicitar() {
        if (solicitudActiva != null) {
            Snackbar.make(binding.cardActiva, R.string.ya_tienes_solicitud, Snackbar.LENGTH_LONG)
                    .setAction(R.string.ver_estado, v -> abrirEstado())
                    .show();
            return;
        }
        startActivity(new Intent(this, SolicitarAsistenciaActivity.class));
    }

    private void abrirEstado() {
        if (solicitudActiva == null) return;
        Intent intent = new Intent(this, EstadoSolicitudActivity.class);
        intent.putExtra(EstadoSolicitudActivity.EXTRA_SOLICITUD_ID, solicitudActiva.getId());
        startActivity(intent);
    }
}
