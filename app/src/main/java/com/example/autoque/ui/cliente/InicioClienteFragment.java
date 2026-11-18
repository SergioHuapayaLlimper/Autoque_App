package com.example.autoque.ui.cliente;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.autoque.R;
import com.example.autoque.data.Escucha;
import com.example.autoque.data.ResenaRepository;
import com.example.autoque.data.SolicitudRepository;
import com.example.autoque.databinding.FragmentInicioClienteBinding;
import com.example.autoque.model.Resena;
import com.example.autoque.model.Solicitud;
import com.example.autoque.ui.common.MiniMapa;
import com.example.autoque.util.Localizador;
import com.example.autoque.util.SessionManager;
import com.example.autoque.util.UiUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.Locale;

public class InicioClienteFragment extends Fragment {

    private FragmentInicioClienteBinding binding;
    private MiniMapa miniMapa;
    private SessionManager session;
    private ListenerRegistration escuchaSolicitudes;
    private ListenerRegistration escuchaResenas;
    private CancellationSignal cancelarUbicacion;
    private Solicitud solicitudActiva;
    private Localizador.Estado ultimoEstado;
    private Double latitud;
    private Double longitud;
    private String direccion;
    private String distrito;

    private final ActivityResultLauncher<String[]> permisoUbicacion = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), r -> actualizarUbicacion());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentInicioClienteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        session = new SessionManager(requireContext());
        miniMapa = new MiniMapa(binding.mapa);
        String nombre = session.getNombre();
        int espacio = nombre.indexOf(' ');
        binding.tvSaludo.setText(getString(R.string.hola_nombre,
                espacio > 0 ? nombre.substring(0, espacio) : nombre));

        binding.btnRefrescarUbicacion.setOnClickListener(v -> actualizarUbicacion());
        binding.btnActivarUbicacion.setOnClickListener(v -> {
            if (Localizador.estado(requireContext()) == Localizador.Estado.SIN_PERMISO) {
                permisoUbicacion.launch(Localizador.PERMISOS);
            } else {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        binding.btnSolicitar.setOnClickListener(v -> solicitar());
        binding.cardActiva.setOnClickListener(v -> abrirActiva());
        binding.btnVerResenas.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CalificarAppActivity.class)));
        binding.btnCalificarApp.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CalificarAppActivity.class)));

        escuchaSolicitudes = new SolicitudRepository().escucharDelCliente(session.getUid(),
                new Escucha<>() {
                    @Override
                    public void onCambio(List<Solicitud> lista) {
                        mostrarActiva(lista);
                    }
                });
        escuchaResenas = new ResenaRepository().escucharResenasApp(new Escucha<>() {
            @Override
            public void onCambio(List<Resena> lista) {
                mostrarResenas(lista);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        miniMapa.onResume();
        if (Localizador.estado(requireContext()) != ultimoEstado || latitud == null) {
            actualizarUbicacion();
        }
    }

    @Override
    public void onPause() {
        miniMapa.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (escuchaSolicitudes != null) escuchaSolicitudes.remove();
        if (escuchaResenas != null) escuchaResenas.remove();
        if (cancelarUbicacion != null) cancelarUbicacion.cancel();
        miniMapa.onDestroy();
        binding = null;
        super.onDestroyView();
    }

    private void actualizarUbicacion() {
        if (binding == null) return;
        ultimoEstado = Localizador.estado(requireContext());
        if (ultimoEstado == Localizador.Estado.SIN_PERMISO) {
            mostrarPanel(R.string.sin_permiso_titulo, R.string.sin_permiso_desc, R.string.permitir_ubicacion);
            binding.tvGpsChip.setText(R.string.ubicacion_no_disponible);
            binding.tvDireccionActual.setText(R.string.ubicacion_no_disponible);
            return;
        }
        if (ultimoEstado == Localizador.Estado.GPS_APAGADO) {
            mostrarPanel(R.string.gps_apagado_titulo, R.string.gps_apagado_desc, R.string.activar_gps);
            binding.tvGpsChip.setText(R.string.gps_apagado_chip);
            binding.tvDireccionActual.setText(R.string.ubicacion_no_disponible);
            return;
        }
        binding.panelSinUbicacion.setVisibility(View.GONE);
        binding.tvGpsChip.setText(R.string.gps_buscando);
        if (cancelarUbicacion != null) cancelarUbicacion.cancel();
        cancelarUbicacion = Localizador.obtener(requireContext(), this::onUbicacion);
    }

    private void mostrarPanel(int titulo, int descripcion, int boton) {
        binding.panelSinUbicacion.setVisibility(View.VISIBLE);
        binding.tvSinUbicacionTitulo.setText(titulo);
        binding.tvSinUbicacionDesc.setText(descripcion);
        binding.btnActivarUbicacion.setText(boton);
    }

    private void onUbicacion(Location location) {
        if (binding == null) return;
        if (location == null) {
            binding.tvGpsChip.setText(R.string.gps_error);
            return;
        }
        latitud = location.getLatitude();
        longitud = location.getLongitude();
        int precision = Math.round(location.getAccuracy());
        binding.tvGpsChip.setText(getString(R.string.gps_activo) + " · ±" + precision + " m");
        miniMapa.limpiar();
        miniMapa.marcar(latitud, longitud, getString(R.string.tu_estas_aqui), true);
        binding.tvDireccionActual.setText(String.format(Locale.US, "%.5f, %.5f", latitud, longitud));
        Localizador.direccion(requireContext(), latitud, longitud, (dir, dist) -> {
            if (binding == null) return;
            direccion = dir;
            distrito = dist;
            StringBuilder texto = new StringBuilder(dir != null ? dir : "");
            if (dist != null) texto.append(texto.length() > 0 ? ", " : "").append(dist);
            if (texto.length() > 0) binding.tvDireccionActual.setText(texto);
        });
    }

    private void mostrarActiva(List<Solicitud> lista) {
        if (binding == null) return;
        solicitudActiva = null;
        for (Solicitud s : lista) {
            if (s.activa()) {
                solicitudActiva = s;
                break;
            }
        }
        if (solicitudActiva == null) {
            binding.cardActiva.setVisibility(View.GONE);
            return;
        }
        binding.cardActiva.setVisibility(View.VISIBLE);
        UiUtils.pintarEstado(binding.tvActivaEstado, solicitudActiva.getEstado());
        String detalle = solicitudActiva.getTipoIncidente() + " · " + solicitudActiva.getDireccion();
        if (solicitudActiva.getNombreTaller() != null) {
            detalle += "\n" + solicitudActiva.getNombreTaller();
        }
        binding.tvActivaDetalle.setText(detalle);
    }

    private void mostrarResenas(List<Resena> lista) {
        if (binding == null) return;
        double promedio = ResenaRepository.promedio(lista);
        binding.tvPromedioApp.setText(UiUtils.promedio(promedio));
        binding.tvEstrellasApp.setText(UiUtils.estrellas(promedio));
        binding.tvTotalResenasApp.setText(getResources().getQuantityString(
                R.plurals.n_resenas, lista.size(), lista.size()));
        if (lista.isEmpty()) {
            binding.tvUltimaResenaAutor.setText(R.string.sin_resenas_app);
            binding.tvUltimaResenaTexto.setVisibility(View.GONE);
        } else {
            Resena r = lista.get(0);
            binding.tvUltimaResenaAutor.setText(r.getAutorNombre() + " · " + UiUtils.estrellas(r.getEstrellas()));
            boolean conTexto = r.getComentario() != null && !r.getComentario().isEmpty();
            binding.tvUltimaResenaTexto.setVisibility(conTexto ? View.VISIBLE : View.GONE);
            if (conTexto) binding.tvUltimaResenaTexto.setText("\"" + r.getComentario() + "\"");
        }
    }

    private void solicitar() {
        if (solicitudActiva != null) {
            Snackbar.make(binding.getRoot(), R.string.ya_tienes_solicitud, Snackbar.LENGTH_LONG)
                    .setAction(R.string.ver_estado, v -> abrirActiva())
                    .show();
            return;
        }
        Intent intent = new Intent(requireContext(), SolicitarAsistenciaActivity.class);
        if (latitud != null) {
            intent.putExtra(SolicitarAsistenciaActivity.EXTRA_LAT, latitud);
            intent.putExtra(SolicitarAsistenciaActivity.EXTRA_LON, longitud);
            intent.putExtra(SolicitarAsistenciaActivity.EXTRA_DIRECCION, direccion);
            intent.putExtra(SolicitarAsistenciaActivity.EXTRA_DISTRITO, distrito);
        }
        startActivity(intent);
    }

    private void abrirActiva() {
        if (solicitudActiva == null) return;
        Intent intent = new Intent(requireContext(), EstadoSolicitudActivity.class);
        intent.putExtra(EstadoSolicitudActivity.EXTRA_SOLICITUD_ID, solicitudActiva.getId());
        startActivity(intent);
    }
}
