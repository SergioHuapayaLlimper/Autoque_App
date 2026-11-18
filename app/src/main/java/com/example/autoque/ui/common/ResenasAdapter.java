package com.example.autoque.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autoque.databinding.ItemResenaBinding;
import com.example.autoque.model.Resena;
import com.example.autoque.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class ResenasAdapter extends RecyclerView.Adapter<ResenasAdapter.Holder> {

    private final List<Resena> items = new ArrayList<>();

    public void setItems(List<Resena> nuevas) {
        items.clear();
        items.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemResenaBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.imprimir(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        private final ItemResenaBinding binding;

        Holder(@NonNull ItemResenaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void imprimir(Resena r) {
            binding.tvIniciales.setText(UiUtils.iniciales(r.getAutorNombre()));
            binding.tvAutor.setText(r.getAutorNombre());
            binding.tvFecha.setText(UiUtils.fecha(r.getCreadoEn()));
            binding.tvEstrellas.setText(UiUtils.estrellas(r.getEstrellas()));
            boolean conTexto = r.getComentario() != null && !r.getComentario().isEmpty();
            binding.tvComentario.setVisibility(conTexto ? View.VISIBLE : View.GONE);
            binding.tvComentario.setText(r.getComentario());
        }
    }
}
