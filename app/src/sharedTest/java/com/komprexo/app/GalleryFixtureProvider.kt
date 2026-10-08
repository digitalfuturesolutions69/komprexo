package com.komprexo.app

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

/** Test APK only: streaming/offset/metadata behaviors found in gallery providers.
 * This is not a Samsung implementation and does not certify an OEM provider.
 */
open class GalleryFixtureProvider : ContentProvider() {
    var streamViaPipe = true
    var fixtureLoader: ((String) -> ByteArray)? = null
    override fun onCreate() = true
    override fun getType(uri: Uri): String? = when (uri.lastPathSegment) {
        "metadata-fails" -> throw UnsupportedOperationException("private document metadata")
        "misdeclared" -> "image/heic"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "heif" -> "image/heif"
        else -> "image/jpeg"
    }
    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor {
        if (uri.lastPathSegment == "revoked") throw SecurityException("private path and permission token")
        if (uri.lastPathSegment == "missing") throw FileNotFoundException("private document")
        val asset = when (uri.lastPathSegment) { "png" -> "alpha.png"; "webp" -> "noise.webp"; else -> "noise.jpg" }
        val payload = if (uri.lastPathSegment == "heif") byteArrayOf(0,0,0,24,102,116,121,112,104,101,105,99)
            else fixtureLoader?.invoke(asset) ?: context!!.assets.open(asset).use { it.readBytes() }
        if (uri.lastPathSegment == "offset") {
            val file = File(context!!.cacheDir, "gallery-offset-fixture")
            val prefix = ByteArray(37) { 7 }
            file.writeBytes(prefix + payload + byteArrayOf(5,6,7))
            return AssetFileDescriptor(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY), prefix.size.toLong(), payload.size.toLong())
        }
        if (!streamViaPipe) {
            // Robolectric shadows pipes with files and can return early EOF before
            // the writer runs. Device CI uses the real blocking pipe below.
            val file = File(context!!.cacheDir, "gallery-stream-fixture").apply { writeBytes(payload) }
            return AssetFileDescriptor(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY), 0, AssetFileDescriptor.UNKNOWN_LENGTH)
        }
        val pipe = ParcelFileDescriptor.createPipe()
        Thread {
            try { ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { it.write(payload) } }
            catch (_: java.io.IOException) { } // Reader may cancel/reject a test fixture.
        }.start()
        return AssetFileDescriptor(pipe[0], 0, AssetFileDescriptor.UNKNOWN_LENGTH)
    }
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = throw UnsupportedOperationException("No _data path or size metadata")
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
}
