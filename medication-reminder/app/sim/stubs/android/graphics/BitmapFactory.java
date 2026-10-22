package android.graphics; public class BitmapFactory { public static Bitmap decodeByteArray(byte[] b, int off, int len) { return len > 0 ? new Bitmap(len) : null; } }
