package com.example.autoque.ui.taller;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.autoque.R;
import com.example.autoque.data.Escucha;
import com.example.autoque.data.PerfilRepository;
import com.example.autoque.databinding.FragmentPerfilTallerBinding;
import com.example.autoque.model.Taller;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.ui.common.AyudaActivity;
import com.example.autoque.ui.common.CambiarPasswordActivity;
import com.example.autoque.ui.common.OpcionesPerfil;
import com.example.autoque.util.SessionManager;
import com.example.autoque.util.UiUtils;
import com.google.firebase.firestore.ListenerRegistration;

public class PerfilTallerFragment extends Fragment {

    private FragmentPerfilTallerBinding binding;
    private ListenerRegistration escucha;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPerfilTallerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        OpcionesPerfil.configurar(binding.opDatos, R.string.datos_del_taller_op, this::abrirEditar);
        OpcionesPerfil.configurar(binding.opServicios, R.string.servicios_op, this::abrirEditar);
        OpcionesPerfil.configurar(binding.opDistrito, R.string.distrito_atencion, this::abrirEditar);
        OpcionesPerfil.configurar(binding.opGaleria, R.string.galeria_fotos,
                () -> abrirMiTaller(MiTallerActivity.TAB_GALERIA));
        OpcionesPerfil.configurar(binding.opOpiniones, R.string.opiniones_recibidas,
                () -> abrirMiTaller(MiTallerActivity.TAB_OPINIONES));
        OpcionesPerfil.configurar(binding.opCambiarPassword, R.string.cambiar_contrasena,
                () -> startActivity(new Intent(requireContext(), CambiarPasswordActivity.class)));
        OpcionesPerfil.configurar(binding.opAyuda, R.string.ayuda_soporte,
                () -> startActivity(new Intent(requireContext(), AyudaActivity.class)));
        binding.btnCerrarSesion.setOnClickListener(v -> ((BaseActivity) requireActivity()).cerrarSesion());

        String uid = new SessionManager(requireContext()).getUid();
        escucha = new PerfilRepository().escucharTaller(uid, new Escucha<>() {
            @Override
            public void onCambio(Taller t) {
                pintar(t);
            }
        });
    }

    private void pintar(Taller t) {
        if (binding == null) return;
        binding.tvIniciales.setText(UiUtils.iniciales(t.getNombreTaller()));
        binding.tvNombreTaller.setText(t.getNombreTaller());
        binding.tvEncargado.setText(getString(R.string.encargado_x, t.getEncargado()));
        binding.tvEmail.setText(t.getEmail());
        binding.tvCalificacion.setText(t.getTotalResenas() == 0
                ? getString(R.string.sin_calificaciones)
                : UiUtils.estrellas(t.promedio()) + " " + UiUtils.promedio(t.promedio()) + " · "
                + getResources().getQuantityString(R.plurals.n_opiniones,
                (int) t.getTotalResenas(), (int) t.getTotalResenas()));
        int servicios = t.serviciosVisibles().size();
        OpcionesPerfil.valor(binding.opServicios,
                getResources().getQuantityString(R.plurals.n_servicios, servicios, servicios));
        OpcionesPerfil.valor(binding.opDistrito, t.getDistrito());
    }

    private void abrirEditar() {
        startActivity(new Intent(requireContext(), EditarTallerActivity.class));
    }

    private void abrirMiTaller(String tab) {
        Intent intent = new Intent(requireContext(), MiTallerActivity.class);
        intent.putExtra(MiTallerActivity.EXTRA_TAB, tab);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        if (escucha != null) escucha.remove();
        binding = null;
        super.onDestroyView();
    }
}
