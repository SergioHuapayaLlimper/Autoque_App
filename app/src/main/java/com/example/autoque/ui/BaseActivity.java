package com.example.autoque.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.autoque.R;
import com.example.autoque.data.AuthRepository;
import com.example.autoque.util.SeguimientoSolicitudes;
import com.example.autoque.util.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public abstract class BaseActivity extends AppCompatActivity {

    protected SessionManager session;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (usaFondoOscuro()) {
            EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT),
                    SystemBarStyle.dark(Color.TRANSPARENT));
        } else {
            EdgeToEdge.enable(this);
        }
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
    }

    protected boolean usaFondoOscuro() {
        return false;
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    protected void setupToolbarConBack(MaterialToolbar toolbar) {
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    public void cerrarSesion() {
        session.cerrar();
        SeguimientoSolicitudes.detener();
        new AuthRepository(this).cerrarSesion();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    protected void cargando(MaterialButton boton, boolean cargando, int textoNormal) {
        boton.setEnabled(!cargando);
        boton.setText(cargando ? R.string.procesando : textoNormal);
    }

    protected boolean exigirRol(String rol) {
        if (!session.haySesion() || !rol.equals(session.getRol())
                || !AuthRepository.haySesionFirebase()) {
            cerrarSesion();
            return false;
        }
        return true;
    }
}
