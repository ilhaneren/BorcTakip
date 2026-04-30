package com.borc.takip.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

object ModelManager {

    private const val TAG = "ModelManager"

    // Küçük Türkçe model: ~35 MB (zip), ~75 MB (açık)
    private const val MODEL_URL =
        "https://alphacephei.com/vosk/models/vosk-model-small-tr-0.3.zip"

    private const val MODEL_FOLDER = "vosk-model-tr"
    private const val MARKER_FILE = ".ready"

    // Maksimum ZIP girişi boyutu: 500 MB (zip bomb koruması)
    private const val MAX_ENTRY_SIZE = 500L * 1024 * 1024

    fun getModelPath(context: Context): String =
        File(context.filesDir, MODEL_FOLDER).absolutePath

    fun isModelReady(context: Context): Boolean =
        File(context.filesDir, "$MODEL_FOLDER/$MARKER_FILE").exists()

    /**
     * Modeli indirir, ZIP'ten güvenli şekilde açar ve dahili depoya yazar.
     * ZIP Slip koruması: her dosya yolu canonical path ile doğrulanır.
     */
    suspend fun downloadAndExtract(
        context: Context,
        onProgress: suspend (downloaded: Long, total: Long) -> Unit
    ) = withContext(Dispatchers.IO) {

        val zipFile = File(context.cacheDir, "vosk-tr.zip")
        val modelDir = File(context.filesDir, MODEL_FOLDER)

        try {
            // ── 1. İndir ──────────────────────────────────────────────────────
            val conn = URL(MODEL_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("Connection", "close")
            conn.connect()
            val total = conn.contentLengthLong

            conn.inputStream.use { input ->
                FileOutputStream(zipFile).use { output ->
                    val buf = ByteArray(16_384)
                    var downloaded = 0L
                    var n: Int
                    while (input.read(buf).also { n = it } != -1) {
                        output.write(buf, 0, n)
                        downloaded += n
                        withContext(Dispatchers.Main) { onProgress(downloaded, total) }
                    }
                }
            }
            conn.disconnect()

            // ── 2. ZIP'ten güvenli aç (ZIP Slip koruması) ────────────────────
            modelDir.deleteRecursively()
            modelDir.mkdirs()
            val canonicalModelDir = modelDir.canonicalFile

            ZipInputStream(FileInputStream(zipFile)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    // İlk dizin bileşenini soy (vosk-model-small-tr-0.3/am/... → am/...)
                    val relative = entry.name.substringAfter('/')

                    if (relative.isNotEmpty() && !relative.startsWith("..")) {
                        val dest = File(canonicalModelDir, relative).canonicalFile

                        // [ZIP Slip koruması] Hedef mutlaka modelDir içinde olmalı
                        if (!dest.absolutePath.startsWith(
                                canonicalModelDir.absolutePath + File.separator
                            )
                        ) {
                            Log.w(TAG, "ZIP Slip girişimi engellendi: ${entry.name}")
                            zip.closeEntry()
                            entry = zip.nextEntry
                            continue
                        }

                        if (entry.isDirectory) {
                            dest.mkdirs()
                        } else {
                            dest.parentFile?.mkdirs()
                            // [Zip Bomb koruması] Maksimum boyutu aşan girişleri reddet
                            var written = 0L
                            FileOutputStream(dest).use { out ->
                                val buf = ByteArray(8_192)
                                var n: Int
                                while (zip.read(buf).also { n = it } != -1) {
                                    written += n
                                    if (written > MAX_ENTRY_SIZE) {
                                        throw SecurityException(
                                            "ZIP girişi boyut limitini aştı: ${entry.name}"
                                        )
                                    }
                                    out.write(buf, 0, n)
                                }
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            // ── 3. Tamamlandı işaretçisi ──────────────────────────────────────
            File(canonicalModelDir, MARKER_FILE).createNewFile()

        } catch (e: Exception) {
            Log.e(TAG, "İndirme/açma hatası", e)
            modelDir.deleteRecursively()
            throw e
        } finally {
            zipFile.delete()
        }
    }

    fun deleteModel(context: Context) {
        File(context.filesDir, MODEL_FOLDER).deleteRecursively()
    }
}
