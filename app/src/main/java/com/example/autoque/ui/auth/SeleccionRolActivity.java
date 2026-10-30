package com.example.autoque.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.example.autoque.databinding.ActivitySeleccionRolBinding;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;

public class SeleccionRolActivity extends BaseActivity {

    private static final String STATE_ROL = "rol";

    private ActivitySeleccionRolBinding binding;
    private String rolSeleccionado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySeleccionRolBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);

        binding.cardCliente.setOnClickListener(v -> seleccionar(Usuario.ROL_CLIENTE));
        binding.cardTaller.setOnClickListener(v -> seleccionar(Usuario.ROL_TALLER));
        binding.btnContinuar.setOnClickListener(v -> continuar());

        if (savedInstanceState != null) {
            String rol = savedInstanceState.getString(STATE_ROL);
            if (rol != null) seleccionar(rol);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_ROL, rolSeleccionado);
    }

    private void seleccionar(String rol) {
        rolSeleccionado = rol;
        boolean cliente = Usuario.ROL_CLIENTE.equals(rol);
        binding.cardCliente.setChecked(cliente);
        binding.rbCliente.setChecked(cliente);
        binding.cardTaller.setChecked(!cliente);
        binding.rbTaller.setChecked(!cliente);
        binding.btnContinuar.setEnabled(true);
        binding.tvAyudaRol.setVisibility(View.INVISIBLE);
    }

    private void continuar() {
        if (rolSeleccionado == null) return;
        Class<?> destino = Usuario.ROL_CLIENTE.equals(rolSeleccionado)
                ? RegistroClienteActivity.class : RegistroTallerActivity.class;
        startActivity(new Intent(this, destino));
    }
}
