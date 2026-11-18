package com.example.autoque.ui.taller;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.autoque.R;
import com.example.autoque.databinding.ActivityTallerHomeBinding;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;

public class TallerHomeActivity extends BaseActivity {

    public static final String EXTRA_TAB = "tab";
    public static final String TAB_ATENCIONES = "atenciones";

    private ActivityTallerHomeBinding binding;

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
