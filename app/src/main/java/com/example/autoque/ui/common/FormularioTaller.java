package com.example.autoque.ui.common;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.autoque.R;
import com.example.autoque.model.Catalogos;
import com.example.autoque.model.Taller;
import com.example.autoque.util.Ubicaciones;
import com.example.autoque.util.Validaciones;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class FormularioTaller {

    public interface AlValidarUbicacion {
        /** La direccion es valida: el taller ya tiene sus coordenadas (o null si no hubo servicio). */
        void onValida(Taller taller, boolean sinServicio);

        /** La direccion no existe o es de otro distrito: el error ya se muestra en el campo. */
        void onInvalida();
    }

    private final Context context;
    private final TextInputLayout tilNombreTaller;
    private final TextInputLayout tilEncargado;
    private final TextInputLayout tilTelefono;
    private final TextInputLayout tilDistrito;
    private final MaterialAutoCompleteTextView actvDistrito;
    private final TextInputLayout tilDireccion;
    private final TextInputLayout tilOtros;
    private final View grupoOtros;
    private final TextView tvErrorServicios;
    private final List<MaterialCheckBox> checks = new ArrayList<>();

    public FormularioTaller(Context context, TextInputLayout tilNombreTaller,
                            TextInputLayout tilEncargado, TextInputLayout tilTelefono,
                            TextInputLayout tilDistrito, MaterialAutoCompleteTextView actvDistrito,
                            TextInputLayout tilDireccion, LinearLayout contenedorServicios,
                            View grupoOtros, TextInputLayout tilOtros, TextView tvErrorServicios) {
        this.context = context;
        this.tilNombreTaller = tilNombreTaller;
        this.tilEncargado = tilEncargado;
        this.tilTelefono = tilTelefono;
        this.tilDistrito = tilDistrito;
        this.actvDistrito = actvDistrito;
        this.tilDireccion = tilDireccion;
        this.tilOtros = tilOtros;
        this.grupoOtros = grupoOtros;
        this.tvErrorServicios = tvErrorServicios;

        Validaciones.limpiarErrorAlEscribir(tilNombreTaller, tilEncargado, tilTelefono,
                tilDireccion, tilOtros);
        actvDistrito.setSimpleItems(Catalogos.DISTRITOS_LIMA.toArray(new String[0]));
        actvDistrito.setOnItemClickListener((p, v, pos, id) -> tilDistrito.setError(null));
        crearChecks(contenedorServicios);
    }

    private void crearChecks(LinearLayout contenedor) {
        for (String servicio : Catalogos.SERVICIOS_TALLER) {
            MaterialCheckBox check = new MaterialCheckBox(context);
            check.setText(servicio);
            check.setTextSize(16);
            check.setMinHeight(context.getResources().getDimensionPixelSize(R.dimen.min_touch));
            check.setOnCheckedChangeListener((b, marcado) -> {
                tvErrorServicios.setVisibility(View.GONE);
                if (Catalogos.SERVICIO_OTROS.equals(servicio)) {
                    grupoOtros.setVisibility(marcado ? View.VISIBLE : View.GONE);
                    if (!marcado) tilOtros.setError(null);
                }
            });
            contenedor.addView(check);
            checks.add(check);
        }
    }

    public void cargar(Taller t) {
        tilNombreTaller.getEditText().setText(t.getNombreTaller());
        tilEncargado.getEditText().setText(t.getEncargado());
        tilTelefono.getEditText().setText(Validaciones.ultimos9Digitos(t.getTelefono()));
        actvDistrito.setText(t.getDistrito(), false);
        tilDireccion.getEditText().setText(t.getDireccion());
        tilOtros.getEditText().setText(t.getOtrosServicios());
        for (MaterialCheckBox c : checks) {
            c.setChecked(t.getServicios() != null && t.getServicios().contains(c.getText().toString()));
        }
    }

    /**
     * Valida de forma estricta que la direccion del taller exista y pertenezca a su distrito.
     * Si es valida, guarda sus coordenadas en el taller para mostrarlo en el mapa del cliente.
     */
    public void validarUbicacion(Taller taller, AlValidarUbicacion cb) {
        Ubicaciones.validar(context, taller.getDireccion(), taller.getDistrito(), v -> {
            switch (v.estado) {
                case VALIDA:
                    taller.setLatitud(v.latitud);
                    taller.setLongitud(v.longitud);
                    cb.onValida(taller, false);
                    break;
                case OTRO_DISTRITO:
                    tilDistrito.setError(context.getString(R.string.dir_otro_distrito,
                            v.distritoEncontrado, taller.getDistrito()));
                    cb.onInvalida();
                    break;
                case NO_ENCONTRADA:
                    tilDireccion.setError(context.getString(R.string.dir_no_encontrada,
                            taller.getDistrito()));
                    cb.onInvalida();
                    break;
                default:
                    // Sin internet o sin servicio de direcciones: no se bloquea el registro
                    taller.setLatitud(null);
                    taller.setLongitud(null);
                    cb.onValida(taller, true);
            }
        });
    }

    public Taller leerValidado() {
        String obligatorio = context.getString(R.string.campo_obligatorio);
        boolean ok = Validaciones.requerido(tilNombreTaller, obligatorio);
        ok &= Validaciones.requerido(tilEncargado, obligatorio);

        String telefono = Validaciones.texto(tilTelefono);
        if (!Validaciones.esCelularValido(telefono)) {
            tilTelefono.setError(context.getString(R.string.telefono_invalido));
            ok = false;
        } else {
            tilTelefono.setError(null);
        }

        String distrito = actvDistrito.getText().toString();
        if (!Catalogos.DISTRITOS_LIMA.contains(distrito)) {
            tilDistrito.setError(context.getString(R.string.distrito_invalido));
            ok = false;
        } else {
            tilDistrito.setError(null);
        }

        ok &= Validaciones.requerido(tilDireccion, obligatorio);

        List<String> servicios = new ArrayList<>();
        for (MaterialCheckBox c : checks) {
            if (c.isChecked()) servicios.add(c.getText().toString());
        }
        if (servicios.isEmpty()) {
            tvErrorServicios.setVisibility(View.VISIBLE);
            ok = false;
        }
        boolean conOtros = servicios.contains(Catalogos.SERVICIO_OTROS);
        if (conOtros) ok &= Validaciones.requerido(tilOtros, obligatorio);
        if (!ok) return null;

        Taller t = new Taller();
        t.setNombreTaller(Validaciones.texto(tilNombreTaller));
        t.setEncargado(Validaciones.texto(tilEncargado));
        t.setTelefono(Validaciones.formatearCelular(telefono));
        t.setDistrito(distrito);
        t.setDireccion(Validaciones.texto(tilDireccion));
        t.setServicios(servicios);
        t.setOtrosServicios(conOtros ? Validaciones.texto(tilOtros) : null);
        return t;
    }
}
