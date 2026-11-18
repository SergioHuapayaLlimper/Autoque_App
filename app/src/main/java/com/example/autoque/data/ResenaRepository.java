package com.example.autoque.data;

import com.example.autoque.model.Resena;
import com.example.autoque.model.Solicitud;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResenaRepository {

    private static final String RESENAS_TALLER = "resenas_taller";
    private static final String RESENAS_APP = "resenas_app";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void calificarTaller(String solicitudId, Resena resena, Resultado<Boolean> cb) {
        DocumentReference solicitudRef = db.collection(SolicitudRepository.SOLICITUDES).document(solicitudId);
        DocumentReference resenaRef = db.collection(RESENAS_TALLER).document(solicitudId);
        db.runTransaction((Transaction.Function<Void>) tx -> {
            DocumentSnapshot doc = tx.get(solicitudRef);
            Solicitud s = doc.toObject(Solicitud.class);
            if (s == null || !Solicitud.FINALIZADA.equals(s.getEstado()) || s.isCalificada()
                    || !resena.getAutorUid().equals(s.getClienteUid()) || s.getTallerUid() == null) {
                throw new FirebaseFirestoreException("NO_DISPONIBLE",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            resena.setTallerUid(s.getTallerUid());
            resena.setSolicitudId(solicitudId);
            tx.set(resenaRef, resena);

            Map<String, Object> taller = new HashMap<>();
            taller.put("sumaEstrellas", FieldValue.increment(resena.getEstrellas()));
            taller.put("totalResenas", FieldValue.increment(1));
            tx.update(db.collection(PerfilRepository.TALLERES).document(s.getTallerUid()), taller);
            tx.update(solicitudRef, "calificada", true);
            return null;
        }).addOnSuccessListener(v -> cb.onExito(true)).addOnFailureListener(e -> {
            if (e instanceof FirebaseFirestoreException && ((FirebaseFirestoreException) e).getCode()
                    == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                cb.onExito(false);
            } else {
                cb.onError(Errores.mensaje(e));
            }
        });
    }

    public ListenerRegistration escucharResenasTaller(String tallerUid, Escucha<List<Resena>> escucha) {
        return db.collection(RESENAS_TALLER).whereEqualTo("tallerUid", tallerUid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        escucha.onError(Errores.mensaje(e));
                    } else if (snap != null) {
                        escucha.onCambio(ordenarRecientes(snap.toObjects(Resena.class)));
                    }
                });
    }

    public ListenerRegistration escucharResenasApp(Escucha<List<Resena>> escucha) {
        return db.collection(RESENAS_APP).addSnapshotListener((snap, e) -> {
            if (e != null) {
                escucha.onError(Errores.mensaje(e));
            } else if (snap != null) {
                escucha.onCambio(ordenarRecientes(snap.toObjects(Resena.class)));
            }
        });
    }

    public void guardarResenaApp(Resena resena, Resultado<Void> cb) {
        db.collection(RESENAS_APP).document(resena.getAutorUid()).set(resena)
                .addOnSuccessListener(v -> cb.onExito(null))
                .addOnFailureListener(e -> cb.onError(Errores.mensaje(e)));
    }

    private static List<Resena> ordenarRecientes(List<Resena> lista) {
        lista.sort((a, b) -> Long.compare(b.getCreadoEn(), a.getCreadoEn()));
        return lista;
    }

    public static double promedio(List<Resena> lista) {
        if (lista.isEmpty()) return 0;
        double suma = 0;
        for (Resena r : lista) suma += r.getEstrellas();
        return suma / lista.size();
    }

    public static int[] distribucion(List<Resena> lista) {
        int[] conteo = new int[6];
        for (Resena r : lista) {
            if (r.getEstrellas() >= 1 && r.getEstrellas() <= 5) conteo[r.getEstrellas()]++;
        }
        return conteo;
    }
}
