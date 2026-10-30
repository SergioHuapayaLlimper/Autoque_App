package com.example.autoque.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.example.autoque.R;
import com.example.autoque.model.Usuario;
import com.example.autoque.util.PasswordUtils;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class AuthRepository {

    private static final String TAG = "AuthRepository";

    public enum Campo { EMAIL, PASSWORD, GENERAL }

    public interface Callback<T> {
        void onExito(@Nullable T resultado);

        void onError(@StringRes int mensaje, Campo campo);
    }

    private final Context context;
    private final UsuarioDao usuarioDao;

    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        this.usuarioDao = new UsuarioDao(this.context);
    }

    public boolean firebaseConfigurado() {
        return !FirebaseApp.getApps(context).isEmpty();
    }

    private FirebaseAuth auth() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.setLanguageCode("es");
        return auth;
    }

    public void crearCredenciales(String email, String password, Callback<String> cb) {
        if (!firebaseConfigurado()) {
            cb.onExito(null);
            return;
        }
        auth().createUserWithEmailAndPassword(email, password).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                cb.onExito(t.getResult().getUser().getUid());
            } else if (t.getException() instanceof FirebaseAuthUserCollisionException) {
                auth().signInWithEmailAndPassword(email, password).addOnCompleteListener(t2 -> {
                    if (t2.isSuccessful()) cb.onExito(t2.getResult().getUser().getUid());
                    else cb.onError(R.string.correo_registrado, Campo.EMAIL);
                });
            } else {
                notificarError(t.getException(), cb);
            }
        });
    }

    public void deshacerCredenciales() {
        if (firebaseConfigurado() && auth().getCurrentUser() != null) {
            auth().getCurrentUser().delete();
        }
    }

    public void iniciarSesion(String email, String password, Callback<Usuario> cb) {
        if (!firebaseConfigurado()) {
            Usuario local = usuarioDao.autenticar(email, password);
            if (local != null) cb.onExito(local);
            else cb.onError(R.string.credenciales_invalidas, Campo.PASSWORD);
            return;
        }
        auth().signInWithEmailAndPassword(email, password).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                Usuario local = usuarioDao.buscarPorEmail(email);
                if (local == null) {
                    auth().signOut();
                    cb.onError(R.string.sin_perfil_local, Campo.GENERAL);
                    return;
                }
                if (local.getFirebaseUid() == null) {
                    usuarioDao.vincularFirebase(local.getId(), t.getResult().getUser().getUid());
                }
                usuarioDao.actualizarPasswordLocal(local.getId(), password);
                cb.onExito(local);
                return;
            }
            Exception e = t.getException();
            if (e instanceof FirebaseAuthInvalidCredentialsException
                    || e instanceof FirebaseAuthInvalidUserException) {
                migrarCuentaLocal(email, password, cb);
            } else {
                notificarError(e, cb);
            }
        });
    }

    private void migrarCuentaLocal(String email, String password, Callback<Usuario> cb) {
        Usuario local = usuarioDao.autenticar(email, password);
        if (local == null || local.getFirebaseUid() != null) {
            cb.onError(R.string.credenciales_invalidas, Campo.PASSWORD);
            return;
        }
        auth().createUserWithEmailAndPassword(email, password).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                usuarioDao.vincularFirebase(local.getId(), t.getResult().getUser().getUid());
                local.setFirebaseUid(t.getResult().getUser().getUid());
                cb.onExito(local);
            } else if (t.getException() instanceof FirebaseAuthWeakPasswordException) {
                cb.onError(R.string.password_corta, Campo.PASSWORD);
            } else {
                cb.onError(R.string.credenciales_invalidas, Campo.PASSWORD);
            }
        });
    }

    public void enviarCorreoRecuperacion(String email, Callback<Void> cb) {
        if (!firebaseConfigurado()) {
            cb.onError(R.string.firebase_no_configurado, Campo.GENERAL);
            return;
        }
        Usuario local = usuarioDao.buscarPorEmail(email);
        if (local == null) {
            cb.onError(R.string.correo_no_registrado, Campo.EMAIL);
            return;
        }
        if (local.getFirebaseUid() != null) {
            enviarEnlace(email, cb);
            return;
        }
        auth().createUserWithEmailAndPassword(email, PasswordUtils.generarClaveAleatoria())
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        usuarioDao.vincularFirebase(local.getId(), t.getResult().getUser().getUid());
                        auth().signOut();
                        enviarEnlace(email, cb);
                    } else if (t.getException() instanceof FirebaseAuthUserCollisionException) {
                        enviarEnlace(email, cb);
                    } else {
                        notificarError(t.getException(), cb);
                    }
                });
    }

    private void enviarEnlace(String email, Callback<Void> cb) {
        auth().sendPasswordResetEmail(email).addOnCompleteListener(t -> {
            if (t.isSuccessful()) cb.onExito(null);
            else notificarError(t.getException(), cb);
        });
    }

    public void cerrarSesion() {
        if (firebaseConfigurado()) auth().signOut();
    }

    private static <T> void notificarError(Exception e, Callback<T> cb) {
        if (e instanceof FirebaseNetworkException) {
            cb.onError(R.string.sin_conexion, Campo.GENERAL);
        } else if (e instanceof FirebaseTooManyRequestsException) {
            cb.onError(R.string.demasiados_intentos, Campo.GENERAL);
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            cb.onError(R.string.password_corta, Campo.PASSWORD);
        } else if (e instanceof FirebaseAuthUserCollisionException) {
            cb.onError(R.string.correo_registrado, Campo.EMAIL);
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            cb.onError(R.string.correo_invalido, Campo.EMAIL);
        } else {
            Log.e(TAG, "Error de Firebase Authentication", e);
            cb.onError(R.string.error_firebase, Campo.GENERAL);
        }
    }
}
