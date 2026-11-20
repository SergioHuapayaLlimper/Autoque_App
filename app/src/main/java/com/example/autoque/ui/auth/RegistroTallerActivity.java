package com.example.autoque.ui.auth;

import android.os.Bundle;

import com.example.autoque.R;
import com.example.autoque.databinding.ActivityRegistroTallerBinding;
import com.example.autoque.model.Taller;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.ui.MainActivity;
import com.example.autoque.ui.common.FormularioTaller;
import com.example.autoque.util.Validaciones;

public class RegistroTallerActivity extends BaseActivity {

    private ActivityRegistroTallerBinding binding;
    private FormularioTaller formulario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistroTallerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);

        formulario = new FormularioTaller(this, binding.tilNombreTaller, binding.tilEncargado,
                binding.tilTelefono, binding.tilDistrito, binding.actvDistrito, binding.tilDireccion,
                binding.contenedorServicios, binding.grupoOtros, binding.tilOtros,
                binding.tvErrorServicios);
        Validaciones.limpiarErrorAlEscribir(binding.tilEmail, binding.tilPassword);
        binding.btnRegistrar.setOnClickListener(v -> registrar());
    }

    private void registrar() {
        Taller taller = formulario.leerValidado();
        boolean ok = taller != null;

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
        if (!ok) return;

        taller.setEmail(email.toLowerCase());
        Usuario usuario = new Usuario(null, Usuario.ROL_TALLER, taller.getEncargado(),
                taller.getTelefono(), taller.getEmail());

        cargando(binding.btnRegistrar, true, R.string.registrar_taller);
        formulario.validarUbicacion(taller, new FormularioTaller.AlValidarUbicacion() {
            @Override
            public void onValida(Taller valido, boolean sinServicio) {
                if (isFinishing()) return;
                RegistroConFirebase.registrar(RegistroTallerActivity.this, binding.btnRegistrar,
                        R.string.registrar_taller, binding.tilEmail, binding.tilPassword, usuario,
                        valido, password, registrado -> {
                            RegistroConFirebase.bienvenida(RegistroTallerActivity.this,
                                    R.string.taller_registrado);
                            session.iniciar(registrado);
                            MainActivity.abrirMenuPrincipal(RegistroTallerActivity.this,
                                    registrado.getRol());
                            finish();
                        });
            }

            @Override
            public void onInvalida() {
                if (isFinishing()) return;
                cargando(binding.btnRegistrar, false, R.string.registrar_taller);
            }
        });
    }
}
