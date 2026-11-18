package com.example.autoque.data;

import com.example.autoque.model.Solicitud;
import com.example.autoque.model.Taller;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolicitudRepository {

    static final String SOLICITUDES = "solicitudes";
    private static final String CONTADORES = "contadores";
    private static final long NUMERO_INICIAL = 1000;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private DocumentReference ref(String id) {
        return db.collection(SOLICITUDES).document(id);
    }

    public void crear(Solicitud s, Resultado<String> cb) {
        DocumentReference contador = db.collection(CONTADORES).document(SOLICITUDES);
        DocumentReference nueva = db.collection(SOLICITUDES).document();
        db.runTransaction((Transaction.Function<String>) tx -> {
            DocumentSnapshot c = tx.get(contador);
            Long actual = c.exists() ? c.getLong("valor") : null;
            long numero = (actual != null ? actual : NUMERO_INICIAL) + 1;
            tx.set(contador, Collections.singletonMap("valor", numero));
            s.setNumero(numero);
            s.setEstado(Solicitud.PENDIENTE);
            s.setCreadoEn(System.currentTimeMillis());
            tx.set(nueva, s);
            return nueva.getId();
        }).addOnSuccessListener(cb::onExito).addOnFailureListener(e -> cb.onError(Errores.mensaje(e)));
    }

    public ListenerRegistration escuchar(String id, Escucha<Solicitud> escucha) {
        return ref(id).addSnapshotListener((doc, e) -> {
            if (e != null) {
                escucha.onError(Errores.mensaje(e));
            } else if (doc != null) {
                escucha.onCambio(doc.exists() ? doc.toObject(Solicitud.class) : null);
            }
        });
    }

    public void obtener(String id, Resultado<Solicitud> cb) {
        ref(id).get()
                .addOnSuccessListener(doc -> cb.onExito(doc.exists() ? doc.toObject(Solicitud.class) : null))
                .addOnFailureListener(e -> cb.onError(Errores.mensaje(e)));
    }

    public ListenerRegistration escucharDelCliente(String clienteUid, Escucha<List<Solicitud>> escucha) {
        return escucharLista(db.collection(SOLICITUDES).whereEqualTo("clienteUid", clienteUid),
                false, escucha);
    }

    public ListenerRegistration escucharPendientesDelDistrito(String distrito,
                                                              Escucha<List<Solicitud>> escucha) {
        return escucharLista(db.collection(SOLICITUDES)
                .whereEqualTo("distrito", distrito)
                .whereEqualTo("estado", Solicitud.PENDIENTE), true, escucha);
    }

    public ListenerRegistration escucharDelTaller(String tallerUid, Escucha<List<Solicitud>> escucha) {
        return escucharLista(db.collection(SOLICITUDES).whereEqualTo("tallerUid", tallerUid),
                false, escucha);
    }

    private ListenerRegistration escucharLista(Query query, boolean ascendente,
                                               Escucha<List<Solicitud>> escucha) {
        return query.addSnapshotListener((snap, e) -> {
            if (e != null) {
                escucha.onError(Errores.mensaje(e));
                return;
            }
            if (snap == null) return;
            List<Solicitud> lista = snap.toObjects(Solicitud.class);
            lista.sort((a, b) -> ascendente
                    ? Long.compare(a.getCreadoEn(), b.getCreadoEn())
                    : Long.compare(b.getCreadoEn(), a.getCreadoEn()));
            escucha.onCambio(lista);
        });
    }

    public void aceptar(String id, Taller taller, Resultado<Boolean> cb) {
        db.runTransaction((Transaction.Function<Void>) tx -> {
            DocumentSnapshot doc = tx.get(ref(id));
            Solicitud s = doc.toObject(Solicitud.class);
            if (s == null || !Solicitud.PENDIENTE.equals(s.getEstado())
                    || !s.getDistrito().equals(taller.getDistrito())) {
                throw new FirebaseFirestoreException("NO_DISPONIBLE",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            Map<String, Object> cambios = new HashMap<>();
            cambios.put("estado", Solicitud.ACEPTADA);
            cambios.put("tallerUid", taller.getUid());
            cambios.put("nombreTaller", taller.getNombreTaller());
            cambios.put("telefonoTaller", taller.getTelefono());
            cambios.put("aceptadoEn", System.currentTimeMillis());
            tx.update(ref(id), cambios);
            return null;
        }).addOnSuccessListener(v -> cb.onExito(true)).addOnFailureListener(e -> {
            if (esNoDisponible(e)) cb.onExito(false);
            else cb.onError(Errores.mensaje(e));
        });
    }

    public void cancelar(String id, String clienteUid, Resultado<Boolean> cb) {
        cambiarEstado(id, Solicitud.PENDIENTE, Solicitud.CANCELADA, "clienteUid", clienteUid,
                null, cb);
    }

    public void finalizar(String id, String tallerUid, Resultado<Boolean> cb) {
        cambiarEstado(id, Solicitud.ACEPTADA, Solicitud.FINALIZADA, "tallerUid", tallerUid,
                "finalizadoEn", cb);
    }

    private void cambiarEstado(String id, String desde, String hacia, String campoDueno,
                               String uidDueno, String campoFecha, Resultado<Boolean> cb) {
        db.runTransaction((Transaction.Function<Void>) tx -> {
            DocumentSnapshot doc = tx.get(ref(id));
            if (!desde.equals(doc.getString("estado")) || !uidDueno.equals(doc.getString(campoDueno))) {
                throw new FirebaseFirestoreException("NO_DISPONIBLE",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            Map<String, Object> cambios = new HashMap<>();
            cambios.put("estado", hacia);
            if (campoFecha != null) cambios.put(campoFecha, System.currentTimeMillis());
            tx.update(ref(id), cambios);
            return null;
        }).addOnSuccessListener(v -> cb.onExito(true)).addOnFailureListener(e -> {
            if (esNoDisponible(e)) cb.onExito(false);
            else cb.onError(Errores.mensaje(e));
        });
    }

    private static boolean esNoDisponible(Exception e) {
        return e instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) e).getCode()
                == FirebaseFirestoreException.Code.FAILED_PRECONDITION;
    }
}
