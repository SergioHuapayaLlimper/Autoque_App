package com.example.autoque.ui.cliente;

import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.autoque.R;
import com.example.autoque.data.Escucha;
import com.example.autoque.data.PerfilRepository;
import com.example.autoque.data.ResenaRepository;
import com.example.autoque.databinding.ActivityPerfilTallerBinding;
import com.example.autoque.model.FotoTaller;
import com.example.autoque.model.Resena;
import com.example.autoque.model.Taller;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.ui.common.Chips;
import com.example.autoque.ui.common.DetalleFilas;
import com.example.autoque.ui.common.FotosAdapter;
import com.example.autoque.ui.common.ResenasAdapter;
import com.example.autoque.util.ImagenUtils;
import com.example.autoque.util.Intents;
import com.example.autoque.util.UiUtils;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class PerfilTallerActivity extends BaseActivity implements FotosAdapter.Listener {

    public static final String EXTRA_TALLER_UID = "taller_uid";

    private ActivityPerfilTallerBinding binding;
    private FotosAdapter fotosAdapter;
    private ResenasAdapter resenasAdapter;
    private ListenerRegistration escuchaTaller;
    private ListenerRegistration escuchaFotos;
    private ListenerRegistration escuchaResenas;
    private Taller taller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!session.haySesion()) {
            cerrarSesion();
            return;
        }
        binding = ActivityPerfilTallerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);
        String uid = getIntent().getStringExtra(EXTRA_TALLER_UID);

        fotosAdapter = new FotosAdapter(false, this);
        binding.recyclerFotos.setLayoutManager(new GridLayoutManager(this, 3));
        binding.recyclerFotos.setAdapter(fotosAdapter);
        resenasAdapter = new ResenasAdapter();
        binding.recyclerResenas.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerResenas.setAdapter(resenasAdapter);
        binding.btnLlamar.setOnClickListener(v -> {
            if (taller != null && taller.getTelefono() != null) Intents.llamar(this, taller.getTelefono());
        });

        PerfilRepository perfiles = new PerfilRepository();
        escuchaTaller = perfiles.escucharTaller(uid, new Escucha<>() {
            @Override
            public void onCambio(Taller t) {
                pintarTaller(t);
            }
        });
        escuchaFotos = perfiles.escucharFotos(uid, new Escucha<>() {
            @Override
            public void onCambio(List<FotoTaller> fotos) {
                pintarFotos(fotos);
            }
        });
        escuchaResenas = new ResenaRepository().escucharResenasTaller(uid, new Escucha<>() {
            @Override
            public void onCambio(List<Resena> lista) {
                resenasAdapter.setItems(lista);
                binding.tvSinResenas.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (escuchaTaller != null) escuchaTaller.remove();
        if (escuchaFotos != null) escuchaFotos.remove();
        if (escuchaResenas != null) escuchaResenas.remove();
        super.onDestroy();
    }

    private void pintarTaller(Taller t) {
        taller = t;
        binding.tvNombre.setText(t.getNombreTaller());
        binding.tvCalificacion.setText(t.getTotalResenas() == 0
                ? getString(R.string.sin_calificaciones)
                : getString(R.string.calif_opiniones,
                UiUtils.estrellas(t.promedio()) + " " + UiUtils.promedio(t.promedio()),
                getResources().getQuantityString(R.plurals.n_opiniones,
                        (int) t.getTotalResenas(), (int) t.getTotalResenas())));
        new DetalleFilas(binding.contenedorDetalle)
                .fila(R.string.direccion, t.getDireccion())
                .fila(R.string.distrito_label, t.getDistrito())
                .fila(R.string.telefono_label, t.getTelefono())
                .fila(R.string.encargado_label, t.getEncargado());
        binding.chipsServicios.removeAllViews();
        for (String servicio : t.serviciosVisibles()) {
            binding.chipsServicios.addView(Chips.etiqueta(this, servicio));
        }
    }

    private void pintarFotos(List<FotoTaller> fotos) {
        boolean hay = !fotos.isEmpty();
        binding.tvSinFotos.setVisibility(hay ? View.GONE : View.VISIBLE);
        binding.imgPrincipal.setImageBitmap(hay ? ImagenUtils.desdeBase64(fotos.get(0).getImagenBase64()) : null);
        fotosAdapter.setItems(fotos.size() > 1 ? fotos.subList(1, fotos.size()) : fotos.subList(0, 0));
    }

    @Override
    public void onSeleccionar(FotoTaller foto) {
        binding.imgPrincipal.setImageBitmap(ImagenUtils.desdeBase64(foto.getImagenBase64()));
    }

    @Override
    public void onEliminar(FotoTaller foto) {
    }
}
