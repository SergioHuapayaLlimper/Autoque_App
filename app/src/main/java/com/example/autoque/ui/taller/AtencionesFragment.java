package com.example.autoque.ui.taller;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.autoque.R;
import com.example.autoque.data.SolicitudDao;
import com.example.autoque.databinding.FragmentAtencionesBinding;
import com.example.autoque.model.Solicitud;
import com.example.autoque.util.Intents;
import com.example.autoque.util.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class AtencionesFragment extends Fragment implements AtencionesAdapter.Listener {

    private FragmentAtencionesBinding binding;
    private SolicitudDao solicitudDao;
    private long tallerId;
    private AtencionesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAtencionesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        solicitudDao = new SolicitudDao(requireContext());
        tallerId = new SessionManager(requireContext()).getUsuarioId();

        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AtencionesAdapter(this);
        binding.recycler.setAdapter(adapter);

        binding.toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) cargar();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void cargar() {
        if (binding == null) return;
        List<Solicitud> enCurso = solicitudDao.atencionesEnCurso(tallerId);
        List<Solicitud> finalizadas = solicitudDao.atencionesFinalizadas(tallerId);
        binding.btnEnCurso.setText(getString(R.string.en_curso_n, enCurso.size()));
        binding.btnFinalizadas.setText(getString(R.string.finalizadas_n, finalizadas.size()));

        boolean verEnCurso = binding.toggle.getCheckedButtonId() == R.id.btnEnCurso;
        List<Solicitud> lista = verEnCurso ? enCurso : finalizadas;
        adapter.setItems(lista);
        binding.tvVacio.setText(verEnCurso ? R.string.sin_en_curso : R.string.sin_finalizadas);
        binding.tvVacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onLlamar(Solicitud s) {
        Intents.llamar(requireContext(), s.getTelefonoContacto());
    }

    @Override
    public void onComoLlegar(Solicitud s) {
        Intents.abrirMapa(requireContext(), s);
    }

    @Override
    public void onFinalizar(Solicitud s) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.finalizar_confirmar_titulo)
                .setMessage(getString(R.string.finalizar_confirmar_msg, s.getNombreContacto()))
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.finalizar, (d, w) -> {
                    if (solicitudDao.finalizar(s.getId(), tallerId)) {
                        Toast.makeText(requireContext(), R.string.atencion_marcada_finalizada,
                                Toast.LENGTH_SHORT).show();
                    }
                    cargar();
                })
                .show();
    }
}
