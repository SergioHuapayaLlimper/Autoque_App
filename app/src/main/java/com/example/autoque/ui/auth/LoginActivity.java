package com.example.autoque.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;

import com.example.autoque.R;
import com.example.autoque.data.AuthRepository;
import com.example.autoque.databinding.ActivityLoginBinding;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.ui.MainActivity;
import com.example.autoque.util.Validaciones;
import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    private AuthRepository auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);
        auth = new AuthRepository(this);

        Validaciones.limpiarErrorAlEscribir(binding.tilEmail, binding.tilPassword);

        binding.btnIngresar.setOnClickListener(v -> ingresar());
        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                ingresar();
                return true;
            }
            return false;
        });
        binding.btnOlvidaste.setOnClickListener(v -> {
            Intent intent = new Intent(this, RecuperarPasswordActivity.class);
            intent.putExtra(RecuperarPasswordActivity.EXTRA_EMAIL,
                    Validaciones.texto(binding.tilEmail));
            startActivity(intent);
        });
        binding.btnRegistrate.setOnClickListener(v ->
                startActivity(new Intent(this, SeleccionRolActivity.class)));
    }

    private void ingresar() {
        String email = Validaciones.texto(binding.tilEmail);
        String password = binding.etPassword.getText().toString();

        boolean ok = true;
        if (!Validaciones.esEmailValido(email)) {
            binding.tilEmail.setError(getString(R.string.correo_invalido));
            ok = false;
        } else {
            binding.tilEmail.setError(null);
        }
        if (password.isEmpty()) {
            binding.tilPassword.setError(getString(R.string.campo_obligatorio));
            ok = false;
        } else {
            binding.tilPassword.setError(null);
        }
        if (!ok) return;

        cargando(binding.btnIngresar, true, R.string.ingresar);
        auth.iniciarSesion(email, password, new AuthRepository.Callback<>() {
            @Override
            public void onExito(Usuario usuario) {
                if (isFinishing()) return;
                session.iniciar(usuario);
                MainActivity.abrirMenuPrincipal(LoginActivity.this, usuario.getRol());
                finish();
            }

            @Override
            public void onError(int mensaje, AuthRepository.Campo campo) {
                if (isFinishing()) return;
                cargando(binding.btnIngresar, false, R.string.ingresar);
                if (campo == AuthRepository.Campo.EMAIL) {
                    binding.tilEmail.setError(getString(mensaje));
                } else if (campo == AuthRepository.Campo.PASSWORD) {
                    binding.tilPassword.setError(getString(mensaje));
                } else {
                    Snackbar.make(binding.btnIngresar, mensaje, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }
}
