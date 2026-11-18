package com.example.autoque.ui.cliente;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autoque.R;
import com.example.autoque.databinding.ItemMiSolicitudBinding;
import com.example.autoque.model.Solicitud;
import com.example.autoque.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

class MisSolicitudesAdapter extends RecyclerView.Adapter<MisSolicitudesAdapter.Holder> {

    interface Listener {
        void onAbrir(Solicitud s);

        void onCalificar(Solicitud s);
    }

    private final List<Solicitud> items = new ArrayList<>();
    private final Listener listener;

    MisSolicitudesAdapter(Listener listener) {
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
        return new Holder(ItemMiSolicitudBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.imprimir(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemMiSolicitudBinding binding;

        Holder(@NonNull ItemMiSolicitudBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void imprimir(Solicitud s) {
            binding.tvIncidente.setText(s.getTipoIncidente());
            UiUtils.pintarEstado(binding.tvEstado, s.getEstado());
            binding.tvNumeroFecha.setText(itemView.getContext().getString(R.string.numero_fecha,
                    s.getNumero(), UiUtils.fechaHora(s.getCreadoEn())));
            if (s.getNombreTaller() != null) {
                binding.tvTaller.setText(s.getNombreTaller());
            } else if (Solicitud.PENDIENTE.equals(s.getEstado())) {
                binding.tvTaller.setText(R.string.esperando_taller);
            } else {
                binding.tvTaller.setText(s.getDireccion());
            }
            boolean puedeCalificar = Solicitud.FINALIZADA.equals(s.getEstado()) && !s.isCalificada();
            binding.btnCalificar.setVisibility(puedeCalificar ? View.VISIBLE : View.GONE);
            binding.btnCalificar.setOnClickListener(v -> listener.onCalificar(s));
            itemView.setOnClickListener(v -> listener.onAbrir(s));
        }
    }
}
