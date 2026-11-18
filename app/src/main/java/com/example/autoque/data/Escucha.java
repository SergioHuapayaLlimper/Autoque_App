package com.example.autoque.data;

import androidx.annotation.StringRes;

public interface Escucha<T> {

    void onCambio(T valor);

    default void onError(@StringRes int mensaje) {
    }
}
