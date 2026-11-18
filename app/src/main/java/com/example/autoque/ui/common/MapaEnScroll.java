package com.example.autoque.ui.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewParent;

import com.google.android.gms.maps.MapView;

/**
 * MapView de Google Maps que se puede mover con el dedo aunque este dentro de un ScrollView:
 * mientras el usuario toca el mapa, el ScrollView padre no intercepta el gesto.
 */
public class MapaEnScroll extends MapView {

    public MapaEnScroll(Context context) {
        super(context);
    }

    public MapaEnScroll(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MapaEnScroll(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        ViewParent padre = getParent();
        if (padre != null) {
            int accion = ev.getActionMasked();
            if (accion == MotionEvent.ACTION_DOWN) {
                padre.requestDisallowInterceptTouchEvent(true);
            } else if (accion == MotionEvent.ACTION_UP || accion == MotionEvent.ACTION_CANCEL) {
                padre.requestDisallowInterceptTouchEvent(false);
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}
