package com.example.autoque.ui.taller;

import android.os.Bundle;
import android.widget.Toast;

import com.example.autoque.R;
import com.example.autoque.data.PerfilRepository;
import com.example.autoque.data.Resultado;
import com.example.autoque.databinding.ActivityEditarTallerBinding;
import com.example.autoque.model.Taller;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.ui.common.FormularioTaller;
import com.google.android.material.snackbar.Snackbar;

public class EditarTallerActivity extends BaseActivity {

    private ActivityEditarTallerBinding binding;
    private FormularioTaller formulario;
    private PerfilRepository perfiles;
    private Taller original;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirRol(Usuario.ROL_TALLER)) return;
        binding = ActivityEditarTallerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);
        perfiles = new PerfilRepository();
        formulario = new FormularioTaller(this, binding.tilNombreTaller, binding.tilEncargado,
                binding.tilTelefono, binding.tilDistrito, binding.actvDistrito, binding.tilDireccion,
                binding.contenedorServicios, binding.grupoOtros, binding.tilOtros,
                binding.tvErrorServicios);
        binding.btnGuardar.setEnabled(false);
        binding.btnGuardar.setOnClickListener(v -> guardar());

        perfiles.obtenerTaller(session.getUid(), new Resultado<>() {
            @Override
            public void onExito(Taller t) {
                if (t == null || isFinishing()) return;
                original = t;
                formulario.cargar(t);
                binding.btnGuardar.setEnabled(true);
            }

            @Override
            public void onError(int mensaje) {
                Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void guardar() {
        Taller editado = formulario.leerValidado();
        if (editado == null || original == null) return;
        editado.setUid(original.getUid());
        editado.setEmail(original.getEmail());
        cargando(binding.btnGuardar, true, R.string.guardar_cambios);

        boolean cambioUbicacion = !editado.getDireccion().equals(original.getDireccion())
                || !editado.getDistrito().equals(original.getDistrito());
        if (cambioUbicacion || !original.tieneUbicacion()) {
            formulario.validarUbicacion(editado, new FormularioTaller.AlValidarUbicacion() {
                @Override
                public void onValida(Taller valido, boolean sinServicio) {
                    if (isFinishing()) return;
                    if (sinServicio && !cambioUbicacion) {
                        valido.setLatitud(original.getLatitud());
                        valido.setLongitud(original.getLongitud());
                    }
                    enviar(valido);
                }

                @Override
                public void onInvalida() {
                    if (isFinishing()) return;
                    cargando(binding.btnGuardar, false, R.string.guardar_cambios);
                }
            });
        } else {
            editado.setLatitud(original.getLatitud());
            editado.setLongitud(original.getLongitud());
            enviar(editado);
        }
    }

    private void enviar(Taller editado) {
        perfiles.actualizarTaller(editado, new Resultado<>() {
            @Override
            public void onExito(Void v) {
                if (isFinishing()) return;
                session.actualizarNombre(editado.getEncargado());
                Toast.makeText(EditarTallerActivity.this, R.string.datos_guardados, Toast.LENGTH_SHORT).show();
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
