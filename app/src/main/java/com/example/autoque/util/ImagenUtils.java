package com.example.autoque.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class ImagenUtils {

    private static final int LADO_MAXIMO = 800;
    private static final int CALIDAD_JPEG = 70;

    private ImagenUtils() {}

    public static String comprimirABase64(Context context, Uri uri) throws IOException {
        ImageDecoder.Source fuente = ImageDecoder.createSource(context.getContentResolver(), uri);
        Bitmap bitmap = ImageDecoder.decodeBitmap(fuente, (decoder, info, src) -> {
            int ancho = info.getSize().getWidth();
            int alto = info.getSize().getHeight();
            float escala = Math.min(1f, (float) LADO_MAXIMO / Math.max(ancho, alto));
            decoder.setTargetSize(Math.round(ancho * escala), Math.round(alto * escala));
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida);
        return Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP);
    }

    public static Bitmap desdeBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
