package com.example.autoque.ui.taller;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.autoque.R;
import com.example.autoque.data.Escucha;
import com.example.autoque.data.PerfilRepository;
import com.example.autoque.data.ResenaRepository;
import com.example.autoque.data.Resultado;
import com.example.autoque.databinding.ActivityMiTallerBinding;
import com.example.autoque.model.FotoTaller;
import com.example.autoque.model.Resena;
import com.example.autoque.model.Taller;
import com.example.autoque.model.Usuario;
import com.example.autoque.ui.BaseActivity;
import com.example.autoque.ui.common.Chips;
import com.example.autoque.ui.common.DetalleFilas;
import com.example.autoque.ui.common.FotosAdapter;
import com.example.autoque.ui.common.ResenasAdapter;
import com.example.autoque.util.ImagenUtils;
import com.example.autoque.util.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MiTallerActivity extends BaseActivity implements FotosAdapter.Listener {

    public static final String EXTRA_TAB = "tab";
    public static final String TAB_GALERIA = "galeria";
    public static final String TAB_OPINIONES = "opiniones";

    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor();

    private ActivityMiTallerBinding binding;
    private PerfilRepository perfiles;
    private FotosAdapter fotosAdapter;
    private ResenasAdapter resenasAdapter;
    private ListenerRegistration escuchaTaller;
    private ListenerRegistration escuchaFotos;
    private ListenerRegistration escuchaResenas;
    private int totalFotos;

    private final ActivityResultLauncher<PickVisualMediaRequest> elegirFoto = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) procesarFoto(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirRol(Usuario.ROL_TALLER)) return;
        binding = ActivityMiTallerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbarConBack(binding.toolbar);
        perfiles = new PerfilRepository();
        String uid = session.getUid();

        fotosAdapter = new FotosAdapter(true, this);
        binding.recyclerFotos.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerFotos.setAdapter(fotosAdapter);
        resenasAdapter = new ResenasAdapter();
        binding.recyclerResenas.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerResenas.setAdapter(resenasAdapter);

        binding.btnSubirFoto.setOnClickListener(v -> {
            if (totalFotos >= FotoTaller.MAXIMO_POR_TALLER) {
                Snackbar.make(binding.getRoot(), R.string.limite_fotos, Snackbar.LENGTH_LONG).show();
                return;
            }
            elegirFoto.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
        binding.btnEditar.setOnClickListener(v ->
                startActivity(new Intent(this, EditarTallerActivity.class)));
        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mostrarPanel(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        int tabInicial = TAB_OPINIONES.equals(getIntent().getStringExtra(EXTRA_TAB)) ? 1 : 0;
        binding.tabs.selectTab(binding.tabs.getTabAt(tabInicial));
        mostrarPanel(tabInicial);

        escuchaTaller = perfiles.escucharTaller(uid, new Escucha<>() {
            @Override
            public void onCambio(Taller t) {
                pintarTaller(t);
            }
        });
        escuchaFotos = perfiles.escucharFotos(uid, new Escucha<>() {
            @Override
            public void onCambio(List<FotoTaller> fotos) {
                totalFotos = fotos.size();
                fotosAdapter.setItems(fotos);
                binding.tvContadorFotos.setText(getString(R.string.fotos_del_local,
                        totalFotos, FotoTaller.MAXIMO_POR_TALLER));
                binding.tvSinFotos.setVisibility(fotos.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
        escuchaResenas = new ResenaRepository().escucharResenasTaller(uid, new Escucha<>() {
            @Override
            public void onCambio(List<Resena> lista) {
                pintarOpiniones(lista);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (escuchaTaller != null) escuchaTaller.remove();
        if (escuchaFotos != null) escuchaFotos.remove();
        if (escuchaResenas != null) escuchaResenas.remove();
        ejecutor.shutdown();
        super.onDestroy();
    }

    private void mostrarPanel(int posicion) {
        binding.panelGaleria.setVisibility(posicion == 0 ? View.VISIBLE : View.GONE);
        binding.panelOpiniones.setVisibility(posicion == 1 ? View.VISIBLE : View.GONE);
        binding.panelDatos.setVisibility(posicion == 2 ? View.VISIBLE : View.GONE);
    }

    private void pintarTaller(Taller t) {
        binding.tvIniciales.setText(UiUtils.iniciales(t.getNombreTaller()));
        binding.tvNombre.setText(t.getNombreTaller());
        binding.tvDireccion.setText(t.getDireccion() + " · " + t.getDistrito());
        binding.tvCalificacion.setText(t.getTotalResenas() == 0
                ? getString(R.string.sin_calificaciones)
                : UiUtils.estrellas(t.promedio()) + " " + UiUtils.promedio(t.promedio()) + " · "
                + getResources().getQuantityString(R.plurals.n_opiniones,
                (int) t.getTotalResenas(), (int) t.getTotalResenas()));
        new DetalleFilas(binding.contenedorDatos)
                .fila(R.string.taller_label, t.getNombreTaller())
                .fila(R.string.encargado_label, t.getEncargado())
                .fila(R.string.telefono_label, t.getTelefono())
                .fila(R.string.distrito_label, t.getDistrito())
                .fila(R.string.direccion, t.getDireccion())
                .fila(R.string.correo_electronico, t.getEmail());
        binding.chipsServicios.removeAllViews();
        for (String servicio : t.serviciosVisibles()) {
            binding.chipsServicios.addView(Chips.etiqueta(this, servicio));
        }
    }

    private void pintarOpiniones(List<Resena> lista) {
        double promedio = ResenaRepository.promedio(lista);
        binding.tvPromedio.setText(UiUtils.promedio(promedio));
        binding.tvEstrellas.setText(UiUtils.estrellas(promedio));
        binding.tvTotalOpiniones.setText(getResources().getQuantityString(
                R.plurals.n_opiniones, lista.size(), lista.size()));
        resenasAdapter.setItems(lista);
        binding.tvSinOpiniones.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);

        int[] conteo = ResenaRepository.distribucion(lista);
        binding.contenedorDistribucion.removeAllViews();
        float densidad = getResources().getDisplayMetrics().density;
        for (int estrellas = 5; estrellas >= 1; estrellas--) {
            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(android.view.Gravity.CENTER_VERTICAL);
            fila.setPadding(0, (int) (3 * densidad), 0, (int) (3 * densidad));

            TextView etiqueta = new TextView(this);
            etiqueta.setText(estrellas + " ★");
            etiqueta.setMinWidth((int) (36 * densidad));
            LinearProgressIndicator barra = new LinearProgressIndicator(this);
            barra.setMax(Math.max(1, lista.size()));
            barra.setProgress(conteo[estrellas]);
            barra.setIndicatorColor(ContextCompat.getColor(this, R.color.autoque_yellow));
            barra.setTrackColor(ContextCompat.getColor(this, R.color.surface_muted));
            barra.setTrackCornerRadius((int) (4 * densidad));
            barra.setTrackThickness((int) (8 * densidad));
            barra.setTrackStopIndicatorSize(0);
            barra.setIndicatorTrackGapSize(0);
            TextView cantidad = new TextView(this);
            cantidad.setText(String.valueOf(conteo[estrellas]));
            cantidad.setMinWidth((int) (32 * densidad));
            cantidad.setGravity(android.view.Gravity.END);

            fila.addView(etiqueta);
            fila.addView(barra, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            fila.addView(cantidad);
            binding.contenedorDistribucion.addView(fila);
        }
    }

    private void procesarFoto(Uri uri) {
        binding.btnSubirFoto.setEnabled(false);
        ejecutor.execute(() -> {
            String base64;
            try {
                base64 = ImagenUtils.comprimirABase64(this, uri);
            } catch (Exception e) {
                base64 = null;
            }
            String resultado = base64;
            runOnUiThread(() -> {
                if (isFinishing()) return;
                binding.btnSubirFoto.setEnabled(true);
                if (resultado == null) {
                    Snackbar.make(binding.getRoot(), R.string.error_foto, Snackbar.LENGTH_LONG).show();
                } else {
                    pedirTitulo(resultado);
                }
            });
        });
    }

    private void pedirTitulo(String base64) {
        View vista = LayoutInflater.from(this).inflate(R.layout.dialog_titulo_foto, null);
        TextInputLayout til = vista.findViewById(R.id.tilTitulo);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.titulo_foto)
                .setView(vista)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.subir_foto, (d, w) -> {
                    TextInputEditText et = (TextInputEditText) til.getEditText();
                    String titulo = et == null ? "" : et.getText().toString().trim();
                    subirFoto(new FotoTaller(base64, titulo.isEmpty() ? getString(R.string.tab_galeria) : titulo));
                })
                .show();
    }

    private void subirFoto(FotoTaller foto) {
        perfiles.agregarFoto(session.getUid(), foto, new Resultado<>() {
            @Override
            public void onExito(Void v) {
                if (!isFinishing()) {
                    Toast.makeText(MiTallerActivity.this, R.string.foto_subida, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(int mensaje) {
                if (!isFinishing()) Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onSeleccionar(FotoTaller foto) {
    }

    @Override
    public void onEliminar(FotoTaller foto) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.eliminar_foto_titulo)
                .setMessage(foto.getTitulo())
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.eliminar, (d, w) ->
                        perfiles.eliminarFoto(session.getUid(), foto.getId(), new Resultado<>() {
                            @Override
                            public void onExito(Void v) {
                            }

                            @Override
                            public void onError(int mensaje) {
                                Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
                            }
                        }))
                .show();
    }
}
