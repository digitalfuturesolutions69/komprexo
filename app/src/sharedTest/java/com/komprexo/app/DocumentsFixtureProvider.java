package com.komprexo.app;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import java.io.*;
import java.util.*;

/** Test-only double for DocumentsContract create/delete calls and file streams.
 * No real storage permission policy is simulated or certified. */
public class DocumentsFixtureProvider extends ContentProvider {
    public static final String AUTHORITY = "com.komprexo.app.test.exports";
    private final Map<String,Integer> creates = new HashMap<>();
    @Override public boolean onCreate() { return true; }
    @Override public Bundle call(String method,String argument,Bundle extras) {
        Uri uri = extras.getParcelable("uri");
        if ("android:createDocument".equals(method)) {
            String root=DocumentsContract.getDocumentId(uri);
            int count=creates.containsKey(root) ? creates.get(root) : 0;
            creates.put(root,count+1);
            if (root.startsWith("revoked") || (root.startsWith("partial") && count>0)) throw new SecurityException("Fixture permission revoked");
            String name=extras.getString("_display_name");
            try { file(name).createNewFile(); }
            catch(IOException e) { throw new IllegalStateException("Fixture storage unavailable"); }
            Bundle reply=new Bundle();reply.putParcelable("uri",DocumentsContract.buildDocumentUri(AUTHORITY,name));return reply;
        }
        if ("android:deleteDocument".equals(method)) {
            try { file(DocumentsContract.getDocumentId(uri)).delete(); } catch(FileNotFoundException ignored) { }
            return new Bundle();
        }
        return super.call(method,argument,extras);
    }
    private File file(String id) throws FileNotFoundException {
        if(id==null || id.length()>200 || id.equals(".") || id.equals("..") || !id.matches("[A-Za-z0-9._-]+")) throw new FileNotFoundException("Invalid fixture identifier");
        File directory=new File(getContext().getCacheDir(),"document-fixtures");directory.mkdirs();return new File(directory,id);
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(file(DocumentsContract.getDocumentId(uri)),ParcelFileDescriptor.parseMode(mode));
    }
    @Override public String getType(Uri uri) { String id=DocumentsContract.getDocumentId(uri);return id.endsWith(".png") ? "image/png" : id.endsWith(".webp") ? "image/webp" : "image/jpeg"; }
    @Override public Cursor query(Uri uri,String[] columns,String selection,String[] args,String order) { return null; }
    @Override public Uri insert(Uri uri,ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri,String selection,String[] args) { return 0; }
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args) { return 0; }
}
