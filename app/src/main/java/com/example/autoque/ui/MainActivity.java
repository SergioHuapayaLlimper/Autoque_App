package com.example.autoque.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.autoque.databinding.ActivityMainBinding;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.auth.LoginActivity;
import com.example.autoque.ui.auth.SeleccionRolActivity;
import com.example.autoque.ui.cliente.ClienteHomeActivity;
import com.example.autoque.ui.taller.TallerHomeActivity;

public class MainActivity extends BaseActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (session.haySesion()) {
            abrirMenuPrincipal(this, session.getRol());
            finish();
            return;
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnIniciarSesion.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
        binding.btnCrearCuenta.setOnClickListener(v ->
                startActivity(new Intent(this, SeleccionRolActivity.class)));
    }

    @Override
    protected boolean usaFondoOscuro() {
        return true;
    }

    public static void abrirMenuPrincipal(Context context, String rol) {
        Class<?> destino = Usuario.ROL_TALLER.equals(rol)
                ? TallerHomeActivity.class : ClienteHomeActivity.class;
        Intent intent = new Intent(context, destino);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
