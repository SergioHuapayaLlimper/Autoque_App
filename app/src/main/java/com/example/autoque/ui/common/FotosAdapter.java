package com.example.autoque.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autoque.databinding.ItemFotoBinding;
import com.example.autoque.model.FotoTaller;
import com.example.autoque.util.ImagenUtils;

import java.util.ArrayList;
import java.util.List;

public class FotosAdapter extends RecyclerView.Adapter<FotosAdapter.Holder> {

    public interface Listener {
        void onSeleccionar(FotoTaller foto);

        void onEliminar(FotoTaller foto);
    }

    private final List<FotoTaller> items = new ArrayList<>();
    private final boolean editable;
    private final Listener listener;

    public FotosAdapter(boolean editable, Listener listener) {
        this.editable = editable;
        this.listener = listener;
    }

    public void setItems(List<FotoTaller> nuevas) {
        items.clear();
        items.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemFotoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
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
        private final ItemFotoBinding binding;

        Holder(@NonNull ItemFotoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void imprimir(FotoTaller foto) {
            binding.imgFoto.setImageBitmap(ImagenUtils.desdeBase64(foto.getImagenBase64()));
            binding.tvTitulo.setText(foto.getTitulo());
            binding.btnEliminar.setVisibility(editable ? View.VISIBLE : View.GONE);
            binding.btnEliminar.setOnClickListener(v -> listener.onEliminar(foto));
            itemView.setOnClickListener(v -> listener.onSeleccionar(foto));
        }
    }
}
