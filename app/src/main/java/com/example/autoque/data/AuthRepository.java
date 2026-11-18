package com.example.autoque.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.example.autoque.R;
import com.example.autoque.model.Taller;
import com.example.autoque.model.Usuario;
import com.example.autoque.util.PasswordUtils;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

public class AuthRepository {

    private static final String TAG = "AuthRepository";

    public enum Campo { EMAIL, PASSWORD, GENERAL }

    public interface Callback<T> {
        void onExito(@Nullable T resultado);

        void onError(@StringRes int mensaje, Campo campo);
    }

    private final Context context;
    private final PerfilRepository perfiles = new PerfilRepository();
    private final PerfilLocalDao perfilLocal;
    private final CuentaLegacyDao legacy;

    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        this.perfilLocal = new PerfilLocalDao(this.context);
        this.legacy = new CuentaLegacyDao(this.context);
    }

    public boolean firebaseConfigurado() {
        return !FirebaseApp.getApps(context).isEmpty();
    }

    public static boolean haySesionFirebase() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    private FirebaseAuth auth() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.setLanguageCode("es");
        return auth;
    }

    public void registrar(Usuario usuario, Taller taller, String password, Callback<Usuario> cb) {
        if (!firebaseConfigurado()) {
            cb.onError(R.string.firebase_no_configurado, Campo.GENERAL);
            return;
        }
        auth().createUserWithEmailAndPassword(usuario.getEmail(), password).addOnCompleteListener(t -> {
            if (!t.isSuccessful()) {
                notificarError(t.getException(), cb);
                return;
            }
            usuario.setUid(t.getResult().getUser().getUid());
            guardarPerfilNuevo(usuario, taller, true, cb);
        });
    }

    private void guardarPerfilNuevo(Usuario usuario, Taller taller, boolean borrarSiFalla,
                                    Callback<Usuario> cb) {
        perfiles.crearPerfil(usuario, taller, new Resultado<>() {
            @Override
            public void onExito(Void v) {
                perfilLocal.guardar(usuario, taller);
                cb.onExito(usuario);
            }

            @Override
            public void onError(int mensaje) {
                FirebaseUser actual = auth().getCurrentUser();
                if (borrarSiFalla && actual != null) actual.delete();
                else auth().signOut();
                cb.onError(mensaje, Campo.GENERAL);
            }
        });
    }

    public void iniciarSesion(String email, String password, Callback<Usuario> cb) {
        if (!firebaseConfigurado()) {
            cb.onError(R.string.firebase_no_configurado, Campo.GENERAL);
            return;
        }
        auth().signInWithEmailAndPassword(email, password).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                cargarPerfil(t.getResult().getUser().getUid(), email, cb);
                return;
            }
            Exception e = t.getException();
            if (e instanceof FirebaseAuthInvalidCredentialsException
                    || e instanceof FirebaseAuthInvalidUserException) {
                migrarCuentaLegacy(email, password, cb);
            } else {
                notificarError(e, cb);
            }
        });
    }

    private void cargarPerfil(String uid, String email, Callback<Usuario> cb) {
        perfiles.obtenerUsuario(uid, new Resultado<>() {
            @Override
            public void onExito(Usuario usuario) {
                if (usuario == null) {
                    subirPerfilLegacy(uid, email, cb);
                } else if (usuario.esTaller()) {
                    perfiles.obtenerTaller(uid, new Resultado<>() {
                        @Override
                        public void onExito(Taller taller) {
                            perfilLocal.guardar(usuario, taller);
                            cb.onExito(usuario);
                        }

                        @Override
                        public void onError(int mensaje) {
                            perfilLocal.guardar(usuario, null);
                            cb.onExito(usuario);
                        }
                    });
                } else {
                    perfilLocal.guardar(usuario, null);
                    cb.onExito(usuario);
                }
            }

            @Override
            public void onError(int mensaje) {
                Usuario enCache = perfilLocal.obtener(uid);
                if (enCache != null) {
                    cb.onExito(enCache);
                } else {
                    auth().signOut();
                    cb.onError(mensaje, Campo.GENERAL);
                }
            }
        });
    }

    private void subirPerfilLegacy(String uid, String email, Callback<Usuario> cb) {
        CuentaLegacyDao.CuentaLegacy cuenta = legacy.buscarPorEmail(email);
        if (cuenta == null) {
            auth().signOut();
            cb.onError(R.string.sin_perfil, Campo.GENERAL);
            return;
        }
        Usuario usuario = cuenta.getUsuario();
        usuario.setUid(uid);
        usuario.setCreadoEn(System.currentTimeMillis());
        Taller taller = usuario.esTaller() ? legacy.obtenerTaller(cuenta.getId()) : null;
        guardarPerfilNuevo(usuario, taller, false, cb);
    }

    private void migrarCuentaLegacy(String email, String password, Callback<Usuario> cb) {
        CuentaLegacyDao.CuentaLegacy cuenta = legacy.buscarPorEmail(email);
        if (cuenta == null || !cuenta.passwordCorrecta(password)) {
            cb.onError(R.string.credenciales_invalidas, Campo.PASSWORD);
            return;
        }
        auth().createUserWithEmailAndPassword(email, password).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                subirPerfilLegacy(t.getResult().getUser().getUid(), email, cb);
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
        CuentaLegacyDao.CuentaLegacy cuenta = legacy.buscarPorEmail(email);
        if (cuenta == null) {
            enviarEnlace(email, cb);
            return;
        }
        auth().createUserWithEmailAndPassword(email, PasswordUtils.generarClaveAleatoria())
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        subirPerfilLegacy(t.getResult().getUser().getUid(), email, new Callback<>() {
                            @Override
                            public void onExito(Usuario u) {
                                auth().signOut();
                                enviarEnlace(email, cb);
                            }

                            @Override
                            public void onError(int mensaje, Campo campo) {
                                enviarEnlace(email, cb);
                            }
                        });
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

    public void cambiarPassword(String actual, String nueva, Callback<Void> cb) {
        FirebaseUser usuario = auth().getCurrentUser();
        if (usuario == null || usuario.getEmail() == null) {
            cb.onError(R.string.sesion_expirada, Campo.GENERAL);
            return;
        }
        AuthCredential credencial = EmailAuthProvider.getCredential(usuario.getEmail(), actual);
        usuario.reauthenticate(credencial).addOnCompleteListener(t -> {
            if (!t.isSuccessful()) {
                if (t.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                    cb.onError(R.string.password_actual_incorrecta, Campo.PASSWORD);
                } else {
                    notificarError(t.getException(), cb);
                }
                return;
            }
            usuario.updatePassword(nueva).addOnCompleteListener(t2 -> {
                if (t2.isSuccessful()) cb.onExito(null);
                else notificarError(t2.getException(), cb);
            });
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
