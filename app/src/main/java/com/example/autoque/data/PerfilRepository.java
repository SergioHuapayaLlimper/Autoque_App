package com.example.autoque.data;

import com.example.autoque.model.FotoTaller;
import com.example.autoque.model.Taller;
import com.example.autoque.model.Usuario;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerfilRepository {

    static final String USUARIOS = "usuarios";
    static final String TALLERES = "talleres";
    static final String FOTOS = "fotos";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private DocumentReference usuarioRef(String uid) {
        return db.collection(USUARIOS).document(uid);
    }

    private DocumentReference tallerRef(String uid) {
        return db.collection(TALLERES).document(uid);
    }

    private CollectionReference fotosRef(String tallerUid) {
        return tallerRef(tallerUid).collection(FOTOS);
    }

    public void crearPerfil(Usuario usuario, Taller taller, Resultado<Void> cb) {
        WriteBatch batch = db.batch();
        batch.set(usuarioRef(usuario.getUid()), usuario);
        if (taller != null) {
            taller.setCreadoEn(System.currentTimeMillis());
            batch.set(tallerRef(usuario.getUid()), taller);
        }
        batch.commit()
                .addOnSuccessListener(v -> cb.onExito(null))
                .addOnFailureListener(e -> cb.onError(Errores.mensaje(e)));
    }

    public void obtenerUsuario(String uid, Resultado<Usuario> cb) {
        usuarioRef(uid).get()
                .addOnSuccessListener(doc -> cb.onExito(doc.exists() ? doc.toObject(Usuario.class) : null))
                .addOnFailureListener(e -> cb.onError(Errores.mensaje(e)));
    }

    public void obtenerTaller(String uid, Resultado<Taller> cb) {
        tallerRef(uid).get()
                .addOnSuccessListener(doc -> cb.onExito(doc.exists() ? doc.toObject(Taller.class) : null))
                .addOnFailureListener(e -> cb.onError(Errores.mensaje(e)));
    }

    public ListenerRegistration escucharUsuario(String uid, Escucha<Usuario> escucha) {
        return usuarioRef(uid).addSnapshotListener((doc, e) -> {
            if (e != null) {
                escucha.onError(Errores.mensaje(e));
            } else if (doc != null && doc.exists()) {
                escucha.onCambio(doc.toObject(Usuario.class));
            }
        });
    }

    public ListenerRegistration escucharTaller(String uid, Escucha<Taller> escucha) {
        return tallerRef(uid).addSnapshotListener((doc, e) -> {
            if (e != null) {
                escucha.onError(Errores.mensaje(e));
            } else if (doc != null && doc.exists()) {
                escucha.onCambio(doc.toObject(Taller.class));
            }
        });
    }

    public void actualizarCliente(String uid, String nombre, String telefono, Resultado<Void> cb) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", nombre);
        datos.put("telefono", telefono);
        usuarioRef(uid).update(datos)
                .addOnSuccessListener(v -> cb.onExito(null))
                .addOnFailureListener(e -> cb.onError(Errores.mensaje(e)));
    }

    public void actualizarTaller(Taller taller, Resultado<Void> cb) {
        Map<String, Object> datosTaller = new HashMap<>();
        datosTaller.put("nombreTaller", taller.getNombreTaller());
        datosTaller.put("encargado", taller.getEncargado());
        datosTaller.put("telefono", taller.getTelefono());
        datosTaller.put("distrito", taller.getDistrito());
        datosTaller.put("direccion", taller.getDireccion());
        datosTaller.put("servicios", taller.getServicios());
        datosTaller.put("otrosServicios", taller.getOtrosServicios());
        datosTaller.put("latitud", taller.getLatitud());
        datosTaller.put("longitud", taller.getLongitud());

        Map<String, Object> datosUsuario = new HashMap<>();
        datosUsuario.put("nombre", taller.getEncargado());
        datosUsuario.put("telefono", taller.getTelefono());

        WriteBatch batch = db.batch();
        batch.update(tallerRef(taller.getUid()), datosTaller);
        batch.update(usuarioRef(taller.getUid()), datosUsuario);
        batch.commit()
                .addOnSuccessListener(v -> cb.onExito(null))
                .addOnFailureListener(e -> cb.onError(Errores.mensaje(e)));
    }

    public ListenerRegistration escucharFotos(String tallerUid, Escucha<List<FotoTaller>> escucha) {
        return fotosRef(tallerUid).orderBy("creadoEn", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        escucha.onError(Errores.mensaje(e));
                    } else if (snap != null) {
                        escucha.onCambio(snap.toObjects(FotoTaller.class));
                    }
                });
    }

    public void agregarFoto(String tallerUid, FotoTaller foto, Resultado<Void> cb) {
        fotosRef(tallerUid).add(foto)
                .addOnSuccessListener(r -> cb.onExito(null))
                .addOnFailureListener(e -> cb.onError(Errores.mensaje(e)));
    }

    public void eliminarFoto(String tallerUid, String fotoId, Resultado<Void> cb) {
        fotosRef(tallerUid).document(fotoId).delete()
                .addOnSuccessListener(v -> cb.onExito(null))
                .addOnFailureListener(e -> cb.onError(Errores.mensaje(e)));
    }
}
