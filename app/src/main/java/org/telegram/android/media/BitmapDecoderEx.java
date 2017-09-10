package org.telegram.android.media;

import android.graphics.Bitmap;
import org.telegram.android.util.NativeLibLoader;

import java.io.IOException;

/**
 * Created by ex3ndr on 07.02.14.
 */
public class BitmapDecoderEx {

    private static final String TAG = "BitmapDecoderEx";

    static {
        NativeLibLoader.loadLib();
    }

    private BitmapDecoderEx() {
    }

    public static void decodeReuseBitmapBlend(String fileName, Bitmap dest, boolean scale) throws IOException {
        nativeDecodeBitmapBlend(fileName, dest, scale);
    }

    public static void decodeReuseBitmap(String fileName, Bitmap dest) throws IOException {
        nativeDecodeBitmap(fileName, dest);
    }

    public static int decodeReuseBitmapScaled(String fileName, Bitmap dest) throws IOException {
        Optimizer.BitmapInfo bitmapInfo = Optimizer.getInfo(fileName);
        int scale = 1;
        while ((bitmapInfo.getWidth() / scale > dest.getWidth() ||
                bitmapInfo.getHeight() / scale > dest.getHeight()) && scale <= 16) {
            scale *= 2;
        }
        if (scale == 1) {
            nativeDecodeBitmap(fileName, dest);
        } else {
            nativeDecodeBitmapScaled(fileName, dest, scale);
        }
        return scale;
    }

    public static void decodeReuseBitmap(byte[] src, Bitmap dest) throws IOException {
        nativeDecodeArray(src, dest);
    }

    public static void saveBitmap(Bitmap dest, int w, int h, String fileName) throws IOException {
        nativeSaveBitmap(dest, w, h, fileName);
    }

    private static native int nativeDecodeBitmapScaled(String fileName, Bitmap bitmap, int scale) throws IOException;

    private static native void nativeDecodeBitmap(String fileName, Bitmap bitmap) throws IOException;

    private static native void nativeDecodeArray(byte[] array, Bitmap bitmap) throws IOException;

    private static native void nativeDecodeBitmapBlend(String fileName, Bitmap bitmap, boolean scale) throws IOException;

    private static native void nativeSaveBitmap(Bitmap bitmap, int w, int h, String fileName) throws IOException;
}
