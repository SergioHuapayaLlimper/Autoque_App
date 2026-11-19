package com.example.autoque.ui.taller;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.autoque.R;
import com.example.autoque.databinding.ActivityTallerHomeBinding;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.util.AvisoSolicitudesTaller;
import com.example.autoque.util.Notificaciones;

public class TallerHomeActivity extends BaseActivity {

    public static final String EXTRA_TAB = "tab";
    public static final String TAB_ATENCIONES = "atenciones";

    private ActivityTallerHomeBinding binding;

    private final ActivityResultLauncher<String> permisoNotificaciones = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), concedido -> { });

    public static void abrirAtenciones(Context context) {
        Intent intent = new Intent(context, TallerHomeActivity.class);
        intent.putExtra(EXTRA_TAB, TAB_ATENCIONES);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirRol(Usuario.ROL_TALLER)) return;
        binding = ActivityTallerHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // HU05: el taller recibe un aviso cuando llega una solicitud nueva de su distrito
        Notificaciones.crearCanalTalleres(this);
        if (savedInstanceState == null && !Notificaciones.tienePermiso(this)) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        AvisoSolicitudesTaller.iniciar(this, session.getUid());

        binding.bottomNav.setOnItemSelectedListener(item -> {
            mostrar(item.getItemId());
            return true;
        });
        binding.bottomNav.setOnItemReselectedListener(item -> { });

        if (savedInstanceState == null) {
            seleccionarTab(tabInicial(getIntent()));
        } else {
            actualizarTitulo(binding.bottomNav.getSelectedItemId());
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        seleccionarTab(tabInicial(intent));
    }

    private void seleccionarTab(int itemId) {
        if (binding.bottomNav.getSelectedItemId() == itemId) {
            mostrar(itemId);
        } else {
            binding.bottomNav.setSelectedItemId(itemId);
        }
    }

    private int tabInicial(Intent intent) {
        return TAB_ATENCIONES.equals(intent.getStringExtra(EXTRA_TAB))
                ? R.id.nav_atenciones : R.id.nav_solicitudes;
    }

    private void mostrar(int itemId) {
        Fragment fragment;
        if (itemId == R.id.nav_atenciones) {
            fragment = new AtencionesFragment();
        } else if (itemId == R.id.nav_perfil) {
            fragment = new PerfilTallerFragment();
        } else {
            fragment = new SolicitudesEntrantesFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contenedorFragment, fragment)
                .commit();
        actualizarTitulo(itemId);
    }

    private void actualizarTitulo(int itemId) {
        if (itemId == R.id.nav_atenciones) {
            binding.toolbar.setTitle(R.string.mis_atenciones);
        } else if (itemId == R.id.nav_perfil) {
            binding.toolbar.setTitle(R.string.mi_cuenta);
        } else {
            binding.toolbar.setTitle(R.string.solicitudes_entrantes);
        }
    }

    void irAAtenciones() {
        seleccionarTab(R.id.nav_atenciones);
    }
}
