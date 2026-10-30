package com.example.autoque.ui;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.autoque.R;
import com.example.autoque.databinding.ActivitySplashBinding;

public class Splash extends AppCompatActivity {

    private static final long DURACION_MS = 3800;

    private ActivitySplashBinding binding;
    private MediaPlayer mediaPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable irAlInicio = this::mostrarIniciar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ocultarBarras();
        reproducirSonido();
        rotarImagen();
        handler.postDelayed(irAlInicio, DURACION_MS);
    }

    private void ocultarBarras() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private void reproducirSonido() {
        mediaPlayer = MediaPlayer.create(this, R.raw.carro);
        if (mediaPlayer != null) mediaPlayer.start();
    }

    private void rotarImagen() {
        RotateAnimation animation = new RotateAnimation(
                0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setInterpolator(new LinearInterpolator());
        animation.setDuration(4000);
        binding.imgLogo.startAnimation(animation);
    }

    private void mostrarIniciar() {
        detenerSonido();
        startActivity(new Intent(Splash.this, MainActivity.class));
        finish();
    }

    private void detenerSonido() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(irAlInicio);
        detenerSonido();
        super.onDestroy();
    }
}
