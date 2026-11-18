package com.example.autoque.ui.cliente;

import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.autoque.R;
import com.example.autoque.data.Escucha;
import com.example.autoque.data.ResenaRepository;
import com.example.autoque.data.Resultado;
import com.example.autoque.databinding.ActivityCalificarAppBinding;
import com.example.autoque.model.Resena;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.ui.common.ResenasAdapter;
import com.example.autoque.util.UiUtils;
import com.example.autoque.util.Validaciones;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class CalificarAppActivity extends BaseActivity {

    private ActivityCalificarAppBinding binding;
    private ResenaRepository resenas;
    private ResenasAdapter adapter;
    private ListenerRegistration escucha;
    private boolean miResenaCargada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirRol(Usuario.ROL_CLIENTE)) return;
        binding = ActivityCalificarAppBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);
        resenas = new ResenaRepository();

        adapter = new ResenasAdapter();
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);
        binding.btnPublicar.setOnClickListener(v -> publicar());

        escucha = resenas.escucharResenasApp(new Escucha<>() {
            @Override
            public void onCambio(List<Resena> lista) {
                pintar(lista);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (escucha != null) escucha.remove();
        super.onDestroy();
    }

    private void pintar(List<Resena> lista) {
        double promedio = ResenaRepository.promedio(lista);
        binding.tvPromedio.setText(UiUtils.promedio(promedio));
        binding.tvEstrellas.setText(UiUtils.estrellas(promedio));
        binding.tvTotal.setText(getResources().getQuantityString(R.plurals.promedio_de,
                lista.size(), lista.size()));
        adapter.setItems(lista);
        binding.tvSinResenas.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);

        if (miResenaCargada) return;
        for (Resena r : lista) {
            if (session.getUid().equals(r.getAutorUid())) {
                binding.ratingBar.setRating(r.getEstrellas());
                binding.tilComentario.getEditText().setText(r.getComentario());
                binding.btnPublicar.setText(R.string.actualizar_resena);
                break;
            }
        }
        miResenaCargada = true;
    }

    private void publicar() {
        int estrellas = Math.round(binding.ratingBar.getRating());
        if (estrellas < 1) {
            Snackbar.make(binding.btnPublicar, R.string.elige_estrellas, Snackbar.LENGTH_SHORT).show();
            return;
        }
        String comentario = Validaciones.texto(binding.tilComentario);
        Resena resena = new Resena(session.getUid(), session.getNombre(), estrellas,
                comentario.isEmpty() ? null : comentario);
        cargando(binding.btnPublicar, true, R.string.publicar_resena);
        resenas.guardarResenaApp(resena, new Resultado<>() {
            @Override
            public void onExito(Void v) {
                if (isFinishing()) return;
                cargando(binding.btnPublicar, false, R.string.actualizar_resena);
                Snackbar.make(binding.btnPublicar, R.string.resena_publicada, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int mensaje) {
                if (isFinishing()) return;
                cargando(binding.btnPublicar, false, R.string.publicar_resena);
                Snackbar.make(binding.btnPublicar, mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
