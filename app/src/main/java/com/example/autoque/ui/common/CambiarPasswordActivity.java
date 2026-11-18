package com.example.autoque.ui.common;

import android.os.Bundle;
import android.widget.Toast;

import com.example.autoque.R;
import com.example.autoque.data.AuthRepository;
import com.example.autoque.databinding.ActivityCambiarPasswordBinding;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.util.Validaciones;
import com.google.android.material.snackbar.Snackbar;

public class CambiarPasswordActivity extends BaseActivity {

    private ActivityCambiarPasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!session.haySesion() || !AuthRepository.haySesionFirebase()) {
            cerrarSesion();
            return;
        }
        binding = ActivityCambiarPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);
        Validaciones.limpiarErrorAlEscribir(binding.tilActual, binding.tilNueva, binding.tilConfirmar);
        binding.btnGuardar.setOnClickListener(v -> guardar());
    }

    private void guardar() {
        String actual = binding.tilActual.getEditText().getText().toString();
        String nueva = binding.tilNueva.getEditText().getText().toString();
        String confirmar = binding.tilConfirmar.getEditText().getText().toString();

        boolean ok = true;
        if (actual.isEmpty()) {
            binding.tilActual.setError(getString(R.string.campo_obligatorio));
            ok = false;
        }
        if (nueva.length() < 6) {
            binding.tilNueva.setError(getString(R.string.password_corta));
            ok = false;
        }
        if (!nueva.equals(confirmar)) {
            binding.tilConfirmar.setError(getString(R.string.contrasenas_no_coinciden));
            ok = false;
        }
        if (!ok) return;

        cargando(binding.btnGuardar, true, R.string.cambiar_contrasena);
        new AuthRepository(this).cambiarPassword(actual, nueva, new AuthRepository.Callback<>() {
            @Override
            public void onExito(Void v) {
                if (isFinishing()) return;
                Toast.makeText(CambiarPasswordActivity.this, R.string.contrasena_actualizada,
                        Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(int mensaje, AuthRepository.Campo campo) {
                if (isFinishing()) return;
                cargando(binding.btnGuardar, false, R.string.cambiar_contrasena);
                if (mensaje == R.string.password_actual_incorrecta) {
                    binding.tilActual.setError(getString(mensaje));
                } else if (campo == AuthRepository.Campo.PASSWORD) {
                    binding.tilNueva.setError(getString(mensaje));
                } else {
                    Snackbar.make(binding.btnGuardar, mensaje, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }
}
