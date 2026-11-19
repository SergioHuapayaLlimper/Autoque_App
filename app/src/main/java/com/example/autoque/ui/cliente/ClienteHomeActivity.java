package com.example.autoque.ui.cliente;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.autoque.R;
import com.example.autoque.databinding.ActivityClienteHomeBinding;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.util.Localizador;
import com.example.autoque.util.Notificaciones;
import com.example.autoque.util.SeguimientoSolicitudes;

public class ClienteHomeActivity extends BaseActivity {

    public static final String EXTRA_TAB = "tab";
    public static final String TAB_SOLICITUDES = "solicitudes";

    private ActivityClienteHomeBinding binding;

    private final ActivityResultLauncher<String> permisoNotificaciones = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), concedido -> { });
    private final ActivityResultLauncher<String[]> permisoUbicacion = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), r -> {
                Localizador.marcarPermisoPedido(this);
                pedirNotificaciones();
            });

    public static void abrirSolicitudes(Context context) {
        Intent intent = new Intent(context, ClienteHomeActivity.class);
        intent.putExtra(EXTRA_TAB, TAB_SOLICITUDES);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirRol(Usuario.ROL_CLIENTE)) return;
        binding = ActivityClienteHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Notificaciones.crearCanal(this);
        // RF09: al abrir el menu principal se pide la ubicacion y, despues, las notificaciones
        // (en orden, porque Android solo muestra un dialogo de permiso a la vez)
        if (savedInstanceState == null && !Localizador.tienePermiso(this)
                && !Localizador.permisoBloqueado(this)) {
            permisoUbicacion.launch(Localizador.PERMISOS);
        } else if (savedInstanceState == null) {
            pedirNotificaciones();
        }
        SeguimientoSolicitudes.iniciar(this, session.getUid());

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

    private void pedirNotificaciones() {
        if (!Notificaciones.tienePermiso(this)) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        seleccionarTab(tabInicial(intent));
    }

    private int tabInicial(Intent intent) {
        return TAB_SOLICITUDES.equals(intent.getStringExtra(EXTRA_TAB))
                ? R.id.nav_mis_solicitudes : R.id.nav_inicio;
    }

    private void seleccionarTab(int itemId) {
        if (binding.bottomNav.getSelectedItemId() == itemId) {
            mostrar(itemId);
        } else {
            binding.bottomNav.setSelectedItemId(itemId);
        }
    }

    private void mostrar(int itemId) {
        Fragment fragment;
        if (itemId == R.id.nav_mis_solicitudes) {
            fragment = new MisSolicitudesFragment();
        } else if (itemId == R.id.nav_perfil) {
            fragment = new PerfilClienteFragment();
        } else {
            fragment = new InicioClienteFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contenedorFragment, fragment)
                .commit();
        actualizarTitulo(itemId);
    }

    private void actualizarTitulo(int itemId) {
        if (itemId == R.id.nav_mis_solicitudes) {
            binding.tvTitulo.setText(R.string.mis_solicitudes);
        } else if (itemId == R.id.nav_perfil) {
            binding.tvTitulo.setText(R.string.mi_perfil);
        } else {
            binding.tvTitulo.setText(R.string.app_name);
        }
    }

    void irASolicitudes() {
        seleccionarTab(R.id.nav_mis_solicitudes);
    }
}
