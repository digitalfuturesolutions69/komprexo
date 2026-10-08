package com.komprexo.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;

/** Test APK only. Java keeps the separate provider process independent of
 * Kotlin classes supplied by the target APK during instrumentation. */
public class GalleryFixtureProvider extends ContentProvider {
    public boolean streamViaPipe = true;
    public interface FixtureLoader { byte[] load(String name) throws IOException; }
    public FixtureLoader fixtureLoader;
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) {
        String name = uri.getLastPathSegment();
        if ("metadata-fails".equals(name)) throw new UnsupportedOperationException("private document metadata");
        if ("misdeclared".equals(name)) return "image/heic";
        if ("png".equals(name)) return "image/png";
        if ("webp".equals(name)) return "image/webp";
        if ("heif".equals(name)) return "image/heif";
        return "image/jpeg";
    }
    @Override public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if ("revoked".equals(name)) throw new SecurityException("private path and permission token");
        if ("missing".equals(name)) throw new FileNotFoundException("private document");
        try {
            String asset = "png".equals(name) ? "alpha.png" : "webp".equals(name) ? "noise.webp" : "noise.jpg";
            final byte[] payload;
            if ("heif".equals(name)) payload = new byte[]{0,0,0,24,102,116,121,112,104,101,105,99};
            else if (fixtureLoader != null) payload = fixtureLoader.load(asset);
            else try (InputStream input = getContext().getAssets().open(asset); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096]; int count;
                while ((count = input.read(buffer)) != -1) bytes.write(buffer,0,count);
                payload = bytes.toByteArray();
            }
            if ("offset".equals(name) || !streamViaPipe) {
                boolean offset = "offset".equals(name);
                File file = new File(getContext().getCacheDir(), offset ? "gallery-offset-fixture" : "gallery-stream-fixture");
                try (OutputStream output = new FileOutputStream(file)) {
                    if (offset) output.write(new byte[37]);
                    output.write(payload);
                    if (offset) output.write(new byte[]{5,6,7});
                }
                return new AssetFileDescriptor(ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY), offset ? 37 : 0,
                    offset ? payload.length : AssetFileDescriptor.UNKNOWN_LENGTH);
            }
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            new Thread(() -> {
                try (OutputStream output = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])) { output.write(payload); }
                catch (IOException ignored) { /* Reader can cancel a fixture. */ }
            }).start();
            return new AssetFileDescriptor(pipe[0],0,AssetFileDescriptor.UNKNOWN_LENGTH);
        } catch (IOException error) { throw new FileNotFoundException("Fixture unavailable"); }
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order) { throw new UnsupportedOperationException("No _data path or size metadata"); }
    @Override public Uri insert(Uri uri,ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri,String selection,String[] args) { return 0; }
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args) { return 0; }
}
