package com.example.autoque.ui.auth;

import android.os.Bundle;
import android.view.View;

import com.example.autoque.R;
import com.example.autoque.data.UsuarioDao;
import com.example.autoque.databinding.ActivityRegistroClienteBinding;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.ui.MainActivity;
import com.example.autoque.util.Validaciones;

public class RegistroClienteActivity extends BaseActivity {

    private ActivityRegistroClienteBinding binding;
    private UsuarioDao usuarioDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistroClienteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);
        usuarioDao = new UsuarioDao(this);

        Validaciones.limpiarErrorAlEscribir(binding.tilNombre, binding.tilTelefono,
                binding.tilEmail, binding.tilPassword);

        binding.tilEmail.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validarEmail();
        });
        binding.cbTerminos.setOnCheckedChangeListener((b, checked) -> {
            if (checked) binding.tvErrorTerminos.setVisibility(View.GONE);
        });
        binding.btnCrearCuenta.setOnClickListener(v -> registrar());
    }

    private boolean validarEmail() {
        String email = Validaciones.texto(binding.tilEmail);
        if (!Validaciones.esEmailValido(email)) {
            binding.tilEmail.setError(getString(R.string.correo_invalido));
            return false;
        }
        if (usuarioDao.existeEmail(email)) {
            binding.tilEmail.setError(getString(R.string.correo_registrado));
            return false;
        }
        binding.tilEmail.setError(null);
        return true;
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

        ok &= validarEmail();

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

        String nombre = Validaciones.texto(binding.tilNombre);
        String email = Validaciones.texto(binding.tilEmail);
        RegistroConFirebase.registrar(this, binding.btnCrearCuenta, R.string.crear_cuenta,
                binding.tilEmail, binding.tilPassword, email, password,
                uid -> usuarioDao.registrarCliente(nombre, Validaciones.formatearCelular(telefono),
                        email, password, uid),
                id -> {
                    RegistroConFirebase.bienvenida(this, R.string.cuenta_creada);
                    session.iniciar(id, Usuario.ROL_CLIENTE);
                    MainActivity.abrirMenuPrincipal(this, Usuario.ROL_CLIENTE);
                    finish();
                });
    }
}
