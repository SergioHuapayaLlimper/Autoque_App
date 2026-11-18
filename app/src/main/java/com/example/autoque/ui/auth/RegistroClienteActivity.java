package com.example.autoque.ui.auth;

import android.os.Bundle;
import android.view.View;

import com.example.autoque.R;
import com.example.autoque.databinding.ActivityRegistroClienteBinding;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.ui.MainActivity;
import com.example.autoque.util.Validaciones;

public class RegistroClienteActivity extends BaseActivity {

    private ActivityRegistroClienteBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistroClienteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);

        Validaciones.limpiarErrorAlEscribir(binding.tilNombre, binding.tilTelefono,
                binding.tilEmail, binding.tilPassword);

        binding.cbTerminos.setOnCheckedChangeListener((b, checked) -> {
            if (checked) binding.tvErrorTerminos.setVisibility(View.GONE);
        });
        binding.btnCrearCuenta.setOnClickListener(v -> registrar());
    }

    private void registrar() {
        boolean ok = Validaciones.requerido(binding.tilNombre, getString(R.string.campo_obligatorio));

        String telefono = Validaciones.texto(binding.tilTelefono);
        if (!Validaciones.esCelularValido(telefono)) {
            binding.tilTelefono.setError(getString(R.string.telefono_invalido));
            ok = false;
        } else {
            binding.tilTelefono.setError(null);
        }

        String email = Validaciones.texto(binding.tilEmail);
        if (!Validaciones.esEmailValido(email)) {
            binding.tilEmail.setError(getString(R.string.correo_invalido));
            ok = false;
        } else {
            binding.tilEmail.setError(null);
        }

        String password = binding.tilPassword.getEditText().getText().toString();
        if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.password_corta));
            ok = false;
        } else {
            binding.tilPassword.setError(null);
        }

        if (!binding.cbTerminos.isChecked()) {
            binding.tvErrorTerminos.setVisibility(View.VISIBLE);
            ok = false;
        }
        if (!ok) return;

        Usuario usuario = new Usuario(null, Usuario.ROL_CLIENTE, Validaciones.texto(binding.tilNombre),
                Validaciones.formatearCelular(telefono), email.toLowerCase());
        RegistroConFirebase.registrar(this, binding.btnCrearCuenta, R.string.crear_cuenta,
                binding.tilEmail, binding.tilPassword, usuario, null, password, registrado -> {
                    RegistroConFirebase.bienvenida(this, R.string.cuenta_creada);
                    session.iniciar(registrado);
                    MainActivity.abrirMenuPrincipal(this, registrado.getRol());
                    finish();
                });
    }
}
