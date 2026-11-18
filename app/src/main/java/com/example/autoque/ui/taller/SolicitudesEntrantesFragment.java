package com.example.autoque.ui.taller;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.autoque.R;
import com.example.autoque.data.Escucha;
import com.example.autoque.data.PerfilRepository;
import com.example.autoque.data.SolicitudRepository;
import com.example.autoque.databinding.FragmentSolicitudesEntrantesBinding;
import com.example.autoque.model.Solicitud;
import com.example.autoque.model.Taller;
import com.example.autoque.util.SessionManager;
import com.example.autoque.util.UiUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class SolicitudesEntrantesFragment extends Fragment
        implements SolicitudesEntrantesAdapter.Listener, AceptacionSolicitud.Callback {

    private static final long INTERVALO_RELOJ_MS = 30_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable reloj = new Runnable() {
        @Override
        public void run() {
            pintarUltimaActualizacion();
            adapter.notifyDataSetChanged();
            handler.postDelayed(this, INTERVALO_RELOJ_MS);
        }
    };

    private FragmentSolicitudesEntrantesBinding binding;
    private SolicitudesEntrantesAdapter adapter;
    private ListenerRegistration escuchaTaller;
    private ListenerRegistration escuchaSolicitudes;
    private String distritoEscuchado;
    private Taller taller;
    private long ultimaActualizacion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSolicitudesEntrantesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SolicitudesEntrantesAdapter(this);
        binding.recycler.setAdapter(adapter);
        binding.btnActualizar.setOnClickListener(v -> escucharSolicitudes(true));

        String uid = new SessionManager(requireContext()).getUid();
        escuchaTaller = new PerfilRepository().escucharTaller(uid, new Escucha<>() {
            @Override
            public void onCambio(Taller t) {
                taller = t;
                adapter.setTaller(t);
                if (binding == null) return;
                binding.tvDistrito.setText(getString(R.string.distrito_chip, t.getDistrito()));
                binding.tvVacioDesc.setText(getString(R.string.sin_solicitudes_desc, t.getDistrito()));
                escucharSolicitudes(false);
            }

            @Override
            public void onError(int mensaje) {
                if (binding != null) Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void escucharSolicitudes(boolean forzar) {
        if (taller == null) return;
        if (!forzar && taller.getDistrito().equals(distritoEscuchado)) return;
        if (escuchaSolicitudes != null) escuchaSolicitudes.remove();
        distritoEscuchado = taller.getDistrito();
        escuchaSolicitudes = new SolicitudRepository().escucharPendientesDelDistrito(distritoEscuchado,
                new Escucha<>() {
                    @Override
                    public void onCambio(List<Solicitud> lista) {
                        pintar(lista);
                    }

                    @Override
                    public void onError(int mensaje) {
                        if (binding != null) Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.postDelayed(reloj, INTERVALO_RELOJ_MS);
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(reloj);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (escuchaTaller != null) escuchaTaller.remove();
        if (escuchaSolicitudes != null) escuchaSolicitudes.remove();
        binding = null;
        super.onDestroyView();
    }

    private void pintar(List<Solicitud> lista) {
        if (binding == null) return;
        adapter.setItems(lista);
        binding.tvConteo.setText(getResources().getQuantityString(
                R.plurals.solicitudes_pendientes, lista.size(), lista.size()));
        binding.vacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
        ultimaActualizacion = System.currentTimeMillis();
        pintarUltimaActualizacion();
    }

    private void pintarUltimaActualizacion() {
        if (ultimaActualizacion == 0 || binding == null) return;
        binding.tvUltimaActualizacion.setText(getString(R.string.ultima_actualizacion,
                UiUtils.haceCuanto(ultimaActualizacion)));
    }

    @Override
    public void onVer(Solicitud s) {
        Intent intent = new Intent(requireContext(), DetalleSolicitudActivity.class);
        intent.putExtra(DetalleSolicitudActivity.EXTRA_SOLICITUD_ID, s.getId());
        startActivity(intent);
    }

    @Override
    public void onAceptar(Solicitud s) {
        AceptacionSolicitud.aceptar(requireActivity(), s, taller, this);
    }

    @Override
    public void onVerAtencion() {
        if (getActivity() instanceof TallerHomeActivity) {
            ((TallerHomeActivity) getActivity()).irAAtenciones();
        }
    }
}
