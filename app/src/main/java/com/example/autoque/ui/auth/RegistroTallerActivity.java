package com.example.autoque.ui.auth;

import android.os.Bundle;
import android.view.View;

import com.example.autoque.R;
import com.example.autoque.data.UsuarioDao;
import com.example.autoque.databinding.ActivityRegistroTallerBinding;
import com.example.autoque.model.Catalogos;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.ui.MainActivity;
import com.example.autoque.util.Validaciones;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

public class RegistroTallerActivity extends BaseActivity {

    private ActivityRegistroTallerBinding binding;
    private final List<MaterialCheckBox> checksServicios = new ArrayList<>();
    private UsuarioDao usuarioDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistroTallerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);
        usuarioDao = new UsuarioDao(this);

        Validaciones.limpiarErrorAlEscribir(binding.tilNombreTaller, binding.tilEncargado,
                binding.tilTelefono, binding.tilDireccion, binding.tilOtros, binding.tilEmail,
                binding.tilPassword);

        binding.actvDistrito.setSimpleItems(Catalogos.DISTRITOS_LIMA.toArray(new String[0]));
        binding.actvDistrito.setOnItemClickListener((p, v, pos, id) ->
                binding.tilDistrito.setError(null));

        crearChecksServicios();

        binding.tilEmail.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validarEmail();
        });
        binding.btnRegistrar.setOnClickListener(v -> registrar());
    }

    private void crearChecksServicios() {
        for (String servicio : Catalogos.SERVICIOS_TALLER) {
            MaterialCheckBox check = new MaterialCheckBox(this);
            check.setText(servicio);
            check.setTextSize(16);
            check.setMinHeight(getResources().getDimensionPixelSize(R.dimen.min_touch));
            check.setOnCheckedChangeListener((b, marcado) -> {
                binding.tvErrorServicios.setVisibility(View.GONE);
                if (Catalogos.SERVICIO_OTROS.equals(servicio)) {
                    binding.grupoOtros.setVisibility(marcado ? View.VISIBLE : View.GONE);
                    if (!marcado) binding.tilOtros.setError(null);
                }
            });
            binding.contenedorServicios.addView(check);
            checksServicios.add(check);
        }
    }

    private List<String> serviciosSeleccionados() {
        List<String> lista = new ArrayList<>();
        for (MaterialCheckBox c : checksServicios) {
            if (c.isChecked()) lista.add(c.getText().toString());
        }
        return lista;
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
        String obligatorio = getString(R.string.campo_obligatorio);
        boolean ok = Validaciones.requerido(binding.tilNombreTaller, obligatorio);
        ok &= Validaciones.requerido(binding.tilEncargado, obligatorio);

        String telefono = Validaciones.texto(binding.tilTelefono);
        if (!Validaciones.esCelularValido(telefono)) {
            binding.tilTelefono.setError(getString(R.string.telefono_invalido));
            ok = false;
        } else {
            binding.tilTelefono.setError(null);
        }

        String distrito = binding.actvDistrito.getText().toString();
        if (!Catalogos.DISTRITOS_LIMA.contains(distrito)) {
            binding.tilDistrito.setError(getString(R.string.distrito_invalido));
            ok = false;
        } else {
            binding.tilDistrito.setError(null);
        }

        ok &= Validaciones.requerido(binding.tilDireccion, obligatorio);

        List<String> servicios = serviciosSeleccionados();
        if (servicios.isEmpty()) {
            binding.tvErrorServicios.setVisibility(View.VISIBLE);
            ok = false;
        }
        if (servicios.contains(Catalogos.SERVICIO_OTROS)) {
            ok &= Validaciones.requerido(binding.tilOtros, obligatorio);
        }

        ok &= validarEmail();

        String password = binding.tilPassword.getEditText().getText().toString();
        if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.password_corta));
            ok = false;
        } else {
            binding.tilPassword.setError(null);
        }
        if (!ok) return;

        String nombreTaller = Validaciones.texto(binding.tilNombreTaller);
        String encargado = Validaciones.texto(binding.tilEncargado);
        String direccion = Validaciones.texto(binding.tilDireccion);
        String otros = Validaciones.texto(binding.tilOtros);
        String email = Validaciones.texto(binding.tilEmail);
        RegistroConFirebase.registrar(this, binding.btnRegistrar, R.string.registrar_taller,
                binding.tilEmail, binding.tilPassword, email, password,
                uid -> usuarioDao.registrarTaller(nombreTaller, encargado,
                        Validaciones.formatearCelular(telefono), distrito, direccion,
                        servicios, otros, email, password, uid),
                id -> {
                    RegistroConFirebase.bienvenida(this, R.string.taller_registrado);
                    session.iniciar(id, Usuario.ROL_TALLER);
                    MainActivity.abrirMenuPrincipal(this, Usuario.ROL_TALLER);
                    finish();
                });
    }
}
