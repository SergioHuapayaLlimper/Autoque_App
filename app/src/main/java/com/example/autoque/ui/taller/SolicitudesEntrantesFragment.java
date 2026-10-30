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
import com.example.autoque.data.SolicitudDao;
import com.example.autoque.data.UsuarioDao;
import com.example.autoque.databinding.FragmentSolicitudesEntrantesBinding;
import com.example.autoque.model.Solicitud;
import com.example.autoque.model.Taller;
import com.example.autoque.util.SessionManager;
import com.example.autoque.util.UiUtils;

import java.util.List;

public class SolicitudesEntrantesFragment extends Fragment
        implements SolicitudesEntrantesAdapter.Listener, AceptacionSolicitud.Callback {

    private static final long INTERVALO_REFRESCO_MS = 15_000;
    private static final long INTERVALO_RELOJ_MS = 30_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresco = new Runnable() {
        @Override
        public void run() {
            cargar();
            handler.postDelayed(this, INTERVALO_REFRESCO_MS);
        }
    };
    private final Runnable relojUltimaActualizacion = new Runnable() {
        @Override
        public void run() {
            pintarUltimaActualizacion();
            handler.postDelayed(this, INTERVALO_RELOJ_MS);
        }
    };

    private FragmentSolicitudesEntrantesBinding binding;
    private SolicitudDao solicitudDao;
    private SolicitudesEntrantesAdapter adapter;
    private long tallerId;
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
        solicitudDao = new SolicitudDao(requireContext());
        tallerId = new SessionManager(requireContext()).getUsuarioId();
        taller = new UsuarioDao(requireContext()).obtenerTaller(tallerId);

        String distrito = taller != null ? taller.getDistrito() : "—";
        binding.tvDistrito.setText(getString(R.string.distrito_chip, distrito));
        binding.tvVacioDesc.setText(getString(R.string.sin_solicitudes_desc, distrito));

        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SolicitudesEntrantesAdapter(this);
        binding.recycler.setAdapter(adapter);

        binding.btnActualizar.setOnClickListener(v -> cargar());
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(refresco);
        handler.postDelayed(relojUltimaActualizacion, INTERVALO_RELOJ_MS);
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(refresco);
        handler.removeCallbacks(relojUltimaActualizacion);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void cargar() {
        if (taller == null || binding == null) return;
        List<Solicitud> lista = solicitudDao.pendientesDelDistritoDelTaller(tallerId);
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
        AceptacionSolicitud.aceptar(requireContext(), s, tallerId, this);
    }

    @Override
    public void onListaCambio() {
        cargar();
    }

    @Override
    public void onVerAtencion() {
        if (getActivity() instanceof TallerHomeActivity) {
            ((TallerHomeActivity) getActivity()).irAAtenciones();
        }
    }
}
