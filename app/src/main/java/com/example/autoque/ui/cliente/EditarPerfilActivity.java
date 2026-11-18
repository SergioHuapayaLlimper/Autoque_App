package com.example.autoque.ui.cliente;

import android.os.Bundle;
import android.widget.Toast;

import com.example.autoque.R;
import com.example.autoque.data.PerfilRepository;
import com.example.autoque.data.Resultado;
import com.example.autoque.databinding.ActivityEditarPerfilBinding;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.util.Validaciones;
import com.google.android.material.snackbar.Snackbar;

public class EditarPerfilActivity extends BaseActivity {

    private ActivityEditarPerfilBinding binding;
    private PerfilRepository perfiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirRol(Usuario.ROL_CLIENTE)) return;
        binding = ActivityEditarPerfilBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);
        perfiles = new PerfilRepository();
        Validaciones.limpiarErrorAlEscribir(binding.tilNombre, binding.tilTelefono);
        binding.btnGuardar.setOnClickListener(v -> guardar());

        perfiles.obtenerUsuario(session.getUid(), new Resultado<>() {
            @Override
            public void onExito(Usuario u) {
                if (u == null || isFinishing()) return;
                binding.tilNombre.getEditText().setText(u.getNombre());
                binding.tilTelefono.getEditText().setText(Validaciones.ultimos9Digitos(u.getTelefono()));
                binding.tilEmail.getEditText().setText(u.getEmail());
            }

            @Override
            public void onError(int mensaje) {
                Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void guardar() {
        boolean ok = Validaciones.requerido(binding.tilNombre, getString(R.string.campo_obligatorio));
        String telefono = Validaciones.texto(binding.tilTelefono);
        if (!Validaciones.esCelularValido(telefono)) {
            binding.tilTelefono.setError(getString(R.string.telefono_invalido));
            ok = false;
        }
        if (!ok) return;

        String nombre = Validaciones.texto(binding.tilNombre);
        cargando(binding.btnGuardar, true, R.string.guardar_cambios);
        perfiles.actualizarCliente(session.getUid(), nombre, Validaciones.formatearCelular(telefono),
                new Resultado<>() {
                    @Override
                    public void onExito(Void v) {
                        if (isFinishing()) return;
                        session.actualizarNombre(nombre);
                        Toast.makeText(EditarPerfilActivity.this, R.string.datos_guardados,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(int mensaje) {
                        if (isFinishing()) return;
                        cargando(binding.btnGuardar, false, R.string.guardar_cambios);
                        Snackbar.make(binding.btnGuardar, mensaje, Snackbar.LENGTH_LONG).show();
                    }
                });
    }
}
