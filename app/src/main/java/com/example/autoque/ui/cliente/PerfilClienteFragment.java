package com.example.autoque.ui.cliente;

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
import com.example.autoque.databinding.FragmentPerfilClienteBinding;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.ui.common.AyudaActivity;
import com.example.autoque.ui.common.CambiarPasswordActivity;
import com.example.autoque.ui.common.OpcionesPerfil;
import com.example.autoque.util.SessionManager;
import com.example.autoque.util.UiUtils;
import com.google.firebase.firestore.ListenerRegistration;

public class PerfilClienteFragment extends Fragment {

    private FragmentPerfilClienteBinding binding;
    private ListenerRegistration escucha;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPerfilClienteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        OpcionesPerfil.configurar(binding.opEditar, R.string.editar_mis_datos,
                () -> startActivity(new Intent(requireContext(), EditarPerfilActivity.class)));
        OpcionesPerfil.configurar(binding.opSolicitudes, R.string.mis_solicitudes, () -> {
            if (getActivity() instanceof ClienteHomeActivity) {
                ((ClienteHomeActivity) getActivity()).irASolicitudes();
            }
        });
        OpcionesPerfil.configurar(binding.opCalificarApp, R.string.calificar_la_app,
                () -> startActivity(new Intent(requireContext(), CalificarAppActivity.class)));
        OpcionesPerfil.configurar(binding.opCambiarPassword, R.string.cambiar_contrasena,
                () -> startActivity(new Intent(requireContext(), CambiarPasswordActivity.class)));
        OpcionesPerfil.configurar(binding.opAyuda, R.string.ayuda_soporte,
                () -> startActivity(new Intent(requireContext(), AyudaActivity.class)));
        binding.btnCerrarSesion.setOnClickListener(v -> ((BaseActivity) requireActivity()).cerrarSesion());

        SessionManager session = new SessionManager(requireContext());
        binding.tvNombre.setText(session.getNombre());
        binding.tvIniciales.setText(UiUtils.iniciales(session.getNombre()));
        escucha = new PerfilRepository().escucharUsuario(session.getUid(), new Escucha<>() {
            @Override
            public void onCambio(Usuario u) {
                if (binding == null) return;
                binding.tvNombre.setText(u.getNombre());
                binding.tvIniciales.setText(UiUtils.iniciales(u.getNombre()));
                binding.tvEmail.setText(u.getEmail());
                binding.tvTelefono.setText(u.getTelefono());
                session.actualizarNombre(u.getNombre());
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (escucha != null) escucha.remove();
        binding = null;
        super.onDestroyView();
    }
}
