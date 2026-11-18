package com.example.autoque.data;

import androidx.annotation.StringRes;

public interface Resultado<T> {

    void onExito(T valor);

    void onError(@StringRes int mensaje);
}
