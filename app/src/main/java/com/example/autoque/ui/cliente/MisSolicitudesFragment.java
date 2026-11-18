package com.example.autoque.ui.cliente;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.autoque.R;
import com.example.autoque.data.Escucha;
import com.example.autoque.data.SolicitudRepository;
import com.example.autoque.databinding.FragmentMisSolicitudesBinding;
import com.example.autoque.model.Solicitud;
import com.example.autoque.ui.common.Chips;
import com.example.autoque.util.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class MisSolicitudesFragment extends Fragment implements MisSolicitudesAdapter.Listener {

    private static final String TODAS = "TODAS";

    private FragmentMisSolicitudesBinding binding;
    private MisSolicitudesAdapter adapter;
    private ListenerRegistration escucha;
    private List<Solicitud> todas = new ArrayList<>();
    private String filtro = TODAS;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMisSolicitudesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        crearFiltros();
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MisSolicitudesAdapter(this);
        binding.recycler.setAdapter(adapter);

        String uid = new SessionManager(requireContext()).getUid();
        escucha = new SolicitudRepository().escucharDelCliente(uid, new Escucha<>() {
            @Override
            public void onCambio(List<Solicitud> lista) {
                todas = lista;
                pintar();
            }
        });
    }

    private void crearFiltros() {
        String[][] filtros = {
                {TODAS, getString(R.string.filtro_todas)},
                {Solicitud.PENDIENTE, getString(R.string.estado_pendiente)},
                {Solicitud.ACEPTADA, getString(R.string.estado_aceptada)},
                {Solicitud.FINALIZADA, getString(R.string.estado_finalizada)},
                {Solicitud.CANCELADA, getString(R.string.estado_cancelada)}};
        for (String[] f : filtros) {
            Chip chip = Chips.crear(requireContext(), f[1], f[0]);
            binding.chipsFiltro.addView(chip);
            if (TODAS.equals(f[0])) chip.setChecked(true);
        }
        binding.chipsFiltro.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            filtro = (String) group.findViewById(ids.get(0)).getTag();
            pintar();
        });
    }

    private void pintar() {
        if (binding == null) return;
        List<Solicitud> visibles = new ArrayList<>();
        for (Solicitud s : todas) {
            if (TODAS.equals(filtro) || filtro.equals(s.getEstado())) visibles.add(s);
        }
        adapter.setItems(visibles);
        binding.tvVacio.setText(todas.isEmpty() ? R.string.sin_mis_solicitudes : R.string.sin_solicitudes_filtro);
        binding.tvVacio.setVisibility(visibles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        if (escucha != null) escucha.remove();
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onAbrir(Solicitud s) {
        Intent intent = new Intent(requireContext(), EstadoSolicitudActivity.class);
        intent.putExtra(EstadoSolicitudActivity.EXTRA_SOLICITUD_ID, s.getId());
        startActivity(intent);
    }

    @Override
    public void onCalificar(Solicitud s) {
        Intent intent = new Intent(requireContext(), CalificarTallerActivity.class);
        intent.putExtra(CalificarTallerActivity.EXTRA_SOLICITUD_ID, s.getId());
        startActivity(intent);
    }
}
