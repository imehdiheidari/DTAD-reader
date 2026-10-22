package ir.mehdi.doseclock;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Gives the camera app one file to write the photo into (cache/camera.jpg). Not exported; access is granted per photo. */
public class PhotoProvider extends ContentProvider {
    static final String AUTHORITY = "ir.mehdi.doseclock.photos";
    static final Uri CAMERA_URI = Uri.parse("content://" + AUTHORITY + "/camera.jpg");

    static File cameraFile(android.content.Context c) { return new File(c.getCacheDir(), "camera.jpg"); }

    @Override public boolean onCreate() { return true; }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"camera.jpg".equals(uri.getLastPathSegment())) throw new FileNotFoundException(uri.toString());
        return ParcelFileDescriptor.open(cameraFile(getContext()), ParcelFileDescriptor.parseMode(mode));
    }

    @Override public String getType(Uri uri) { return "image/jpeg"; }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        MatrixCursor c = new MatrixCursor(new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[] {"camera.jpg", cameraFile(getContext()).length()});
        return c;
    }

    @Override public Uri insert(Uri uri, ContentValues v) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
