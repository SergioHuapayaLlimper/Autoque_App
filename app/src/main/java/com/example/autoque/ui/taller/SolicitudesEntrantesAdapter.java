package com.example.autoque.ui.taller;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autoque.R;
import com.example.autoque.databinding.ItemSolicitudEntranteBinding;
import com.example.autoque.model.Solicitud;
import com.example.autoque.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

class SolicitudesEntrantesAdapter
        extends RecyclerView.Adapter<SolicitudesEntrantesAdapter.Holder> {

    interface Listener {
        void onVer(Solicitud s);

        void onAceptar(Solicitud s);
    }

    private final List<Solicitud> items = new ArrayList<>();
    private final Listener listener;

    SolicitudesEntrantesAdapter(Listener listener) {
        this.listener = listener;
    }

    void setItems(List<Solicitud> nuevas) {
        items.clear();
        items.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemSolicitudEntranteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        h.imprimir(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemSolicitudEntranteBinding binding;

        Holder(@NonNull ItemSolicitudEntranteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void imprimir(Solicitud s) {
            binding.tvIncidente.setText(s.getTipoIncidente());
            UiUtils.pintarEstado(binding.tvEstado, s.getEstado());
            binding.tvDireccion.setText(s.getDireccion() + " · " + s.getDistrito());
            String tiempo = UiUtils.haceCuanto(s.getCreadoEn());
            binding.tvTiempo.setText(Character.toUpperCase(tiempo.charAt(0)) + tiempo.substring(1));
            binding.tvCosto.setText(itemView.getContext()
                    .getString(R.string.aprox_costo, s.rangoCosto()));
            itemView.setOnClickListener(v -> listener.onVer(s));
            binding.btnVer.setOnClickListener(v -> listener.onVer(s));
            binding.btnAceptar.setOnClickListener(v -> listener.onAceptar(s));
        }
    }
}
