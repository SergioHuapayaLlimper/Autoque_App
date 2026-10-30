package com.example.autoque.ui.taller;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autoque.R;
import com.example.autoque.databinding.ItemAtencionEnCursoBinding;
import com.example.autoque.databinding.ItemAtencionFinalizadaBinding;
import com.example.autoque.model.Solicitud;
import com.example.autoque.ui.common.DetalleFilas;
import com.example.autoque.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

class AtencionesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    interface Listener {
        void onLlamar(Solicitud s);

        void onComoLlegar(Solicitud s);

        void onFinalizar(Solicitud s);
    }

    private static final int TIPO_EN_CURSO = 0;
    private static final int TIPO_FINALIZADA = 1;

    private final List<Solicitud> items = new ArrayList<>();
    private final Listener listener;

    AtencionesAdapter(Listener listener) {
        this.listener = listener;
    }

    void setItems(List<Solicitud> nuevas) {
        items.clear();
        items.addAll(nuevas);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return Solicitud.ACEPTADA.equals(items.get(position).getEstado())
                ? TIPO_EN_CURSO : TIPO_FINALIZADA;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TIPO_EN_CURSO) {
            return new EnCursoHolder(ItemAtencionEnCursoBinding.inflate(inflater, parent, false));
        }
        return new FinalizadaHolder(ItemAtencionFinalizadaBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Solicitud s = items.get(position);
        if (holder instanceof EnCursoHolder) {
            ((EnCursoHolder) holder).imprimir(s);
        } else {
            ((FinalizadaHolder) holder).imprimir(s);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class EnCursoHolder extends RecyclerView.ViewHolder {
        private final ItemAtencionEnCursoBinding binding;

        EnCursoHolder(@NonNull ItemAtencionEnCursoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void imprimir(Solicitud s) {
            binding.tvIncidente.setText(s.getTipoIncidente());
            UiUtils.pintarEstado(binding.tvEstado, s.getEstado());
            binding.tvIniciales.setText(UiUtils.iniciales(s.getNombreContacto()));
            binding.tvCliente.setText(s.getNombreContacto());
            binding.tvTelefono.setText(s.getTelefonoContacto());
            new DetalleFilas(binding.contenedorDetalle)
                    .fila(R.string.detalle, s.getDescripcion())
                    .fila(R.string.direccion, s.getDireccion())
                    .fila(R.string.distrito_label, s.getDistrito())
                    .fila(R.string.aceptada_label, s.getAceptadoEn() != null
                            ? UiUtils.fechaHora(s.getAceptadoEn()) : null)
                    .fila(R.string.costo_aprox_label, s.rangoCosto());
            binding.btnLlamar.setOnClickListener(v -> listener.onLlamar(s));
            binding.btnComoLlegar.setOnClickListener(v -> listener.onComoLlegar(s));
            binding.btnFinalizar.setOnClickListener(v -> listener.onFinalizar(s));
        }
    }

    static class FinalizadaHolder extends RecyclerView.ViewHolder {
        private final ItemAtencionFinalizadaBinding binding;

        FinalizadaHolder(@NonNull ItemAtencionFinalizadaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void imprimir(Solicitud s) {
            binding.tvIncidente.setText(s.getTipoIncidente());
            long cuando = s.getFinalizadoEn() != null ? s.getFinalizadoEn() : s.getCreadoEn();
            binding.tvResumen.setText(UiUtils.fechaHora(cuando) + " · " + s.getNombreContacto());
            UiUtils.pintarEstado(binding.tvEstado, s.getEstado());
        }
    }
}
