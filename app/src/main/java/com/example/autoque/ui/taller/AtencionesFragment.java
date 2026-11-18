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
import com.example.autoque.data.Escucha;
import com.example.autoque.data.Resultado;
import com.example.autoque.data.SolicitudRepository;
import com.example.autoque.databinding.FragmentAtencionesBinding;
import com.example.autoque.model.Solicitud;
import com.example.autoque.util.Intents;
import com.example.autoque.util.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class AtencionesFragment extends Fragment implements AtencionesAdapter.Listener {

    private FragmentAtencionesBinding binding;
    private SolicitudRepository solicitudes;
    private ListenerRegistration escucha;
    private String tallerUid;
    private AtencionesAdapter adapter;
    private final List<Solicitud> enCurso = new ArrayList<>();
    private final List<Solicitud> finalizadas = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAtencionesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        solicitudes = new SolicitudRepository();
        tallerUid = new SessionManager(requireContext()).getUid();

        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AtencionesAdapter(this);
        binding.recycler.setAdapter(adapter);
        binding.toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) pintar();
        });

        escucha = solicitudes.escucharDelTaller(tallerUid, new Escucha<>() {
            @Override
            public void onCambio(List<Solicitud> lista) {
                enCurso.clear();
                finalizadas.clear();
                for (Solicitud s : lista) {
                    if (Solicitud.ACEPTADA.equals(s.getEstado())) enCurso.add(s);
                    else if (Solicitud.FINALIZADA.equals(s.getEstado())) finalizadas.add(s);
                }
                finalizadas.sort((a, b) -> Long.compare(
                        b.getFinalizadoEn() != null ? b.getFinalizadoEn() : 0,
                        a.getFinalizadoEn() != null ? a.getFinalizadoEn() : 0));
                pintar();
            }

            @Override
            public void onError(int mensaje) {
                if (binding != null) Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (escucha != null) escucha.remove();
        binding = null;
        super.onDestroyView();
    }

    private void pintar() {
        if (binding == null) return;
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
                .setPositiveButton(R.string.finalizar, (d, w) ->
                        solicitudes.finalizar(s.getId(), tallerUid, new Resultado<>() {
                            @Override
                            public void onExito(Boolean ok) {
                                if (ok && isAdded()) {
                                    Toast.makeText(requireContext(), R.string.atencion_marcada_finalizada,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onError(int mensaje) {
                                if (binding != null) {
                                    Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
                                }
                            }
                        }))
                .show();
    }
}
