package com.example.autoque.ui.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.autoque.R;
import com.example.autoque.data.AuthRepository;
import com.example.autoque.databinding.ActivityRecuperarPasswordBinding;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.util.Validaciones;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

public class RecuperarPasswordActivity extends BaseActivity {

    public static final String EXTRA_EMAIL = "email";
    private static final String STATE_ENVIADO = "enviado_a";
    private static final long ESPERA_REENVIO_MS = 60_000;

    private ActivityRecuperarPasswordBinding binding;
    private AuthRepository auth;
    private TextInputLayout tilEmail;
    private TextView tvErrorGeneral;
    private MaterialButton btnEnviar;
    private MaterialButton btnReenviar;
    private String emailEnviado;
    private CountDownTimer temporizador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecuperarPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);
        auth = new AuthRepository(this);

        tilEmail = binding.tilEmail;
        tvErrorGeneral = binding.tvErrorGeneral;
        btnEnviar = binding.btnEnviar;
        btnReenviar = binding.btnReenviar;
        Validaciones.limpiarErrorAlEscribir(tilEmail);

        String emailInicial = getIntent().getStringExtra(EXTRA_EMAIL);
        if (emailInicial != null) tilEmail.getEditText().setText(emailInicial);

        btnEnviar.setOnClickListener(v -> enviar());
        tilEmail.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                enviar();
                return true;
            }
            return false;
        });
        binding.btnVolverLogin.setOnClickListener(v -> finish());
        binding.btnIrLogin.setOnClickListener(v -> finish());
        btnReenviar.setOnClickListener(v -> reenviar());

        if (savedInstanceState != null && savedInstanceState.getString(STATE_ENVIADO) != null) {
            mostrarEnviado(savedInstanceState.getString(STATE_ENVIADO));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_ENVIADO, emailEnviado);
    }

    @Override
    protected void onDestroy() {
        if (temporizador != null) temporizador.cancel();
        super.onDestroy();
    }

    private void enviar() {
        String email = Validaciones.texto(tilEmail);
        tvErrorGeneral.setVisibility(View.GONE);
        if (!Validaciones.esEmailValido(email)) {
            tilEmail.setError(getString(R.string.correo_invalido));
            return;
        }
        cargando(btnEnviar, true, R.string.enviar_enlace);
        auth.enviarCorreoRecuperacion(email, new AuthRepository.Callback<>() {
            @Override
            public void onExito(Void r) {
                if (isFinishing()) return;
                cargando(btnEnviar, false, R.string.enviar_enlace);
                mostrarEnviado(email);
            }

            @Override
            public void onError(int mensaje, AuthRepository.Campo campo) {
                if (isFinishing()) return;
                cargando(btnEnviar, false, R.string.enviar_enlace);
                if (campo == AuthRepository.Campo.EMAIL) {
                    tilEmail.setError(getString(mensaje));
                } else {
                    tvErrorGeneral.setText(mensaje);
                    tvErrorGeneral.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void reenviar() {
        cargando(btnReenviar, true, R.string.reenviar_enlace);
        auth.enviarCorreoRecuperacion(emailEnviado, new AuthRepository.Callback<>() {
            @Override
            public void onExito(Void r) {
                if (isFinishing()) return;
                Toast.makeText(RecuperarPasswordActivity.this, R.string.enlace_reenviado,
                        Toast.LENGTH_SHORT).show();
                iniciarEsperaReenvio();
            }

            @Override
            public void onError(int mensaje, AuthRepository.Campo campo) {
                if (isFinishing()) return;
                cargando(btnReenviar, false, R.string.reenviar_enlace);
                Toast.makeText(RecuperarPasswordActivity.this, mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarEnviado(String email) {
        emailEnviado = email;
        binding.tvEnviadoDetalle
                .setText(getString(R.string.enlace_enviado_detalle, email));
        binding.pasoCorreo.setVisibility(View.GONE);
        binding.pasoEnviado.setVisibility(View.VISIBLE);
        iniciarEsperaReenvio();
    }

    private void iniciarEsperaReenvio() {
        if (temporizador != null) temporizador.cancel();
        btnReenviar.setEnabled(false);
        temporizador = new CountDownTimer(ESPERA_REENVIO_MS, 1_000) {
            @Override
            public void onTick(long restante) {
                btnReenviar.setText(getString(R.string.reenviar_en, (int) (restante / 1_000) + 1));
            }

            @Override
            public void onFinish() {
                btnReenviar.setEnabled(true);
                btnReenviar.setText(R.string.reenviar_enlace);
            }
        }.start();
    }
}
