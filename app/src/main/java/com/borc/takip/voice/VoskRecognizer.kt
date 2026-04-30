package com.borc.takip.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService

/** Uygulama katmanına bildirilen olaylar */
interface VoskListener {
    fun onReady()
    fun onListening()
    fun onPartial(text: String)
    fun onResult(text: String)
    fun onError(message: String)
}

/**
 * Vosk offline ses tanıma sarmalayıcısı.
 * Thread-safe: tüm paylaşılan durum [lock] ile korunur.
 * Hata detayları yalnızca Logcat'e yazılır, UI'ye genel mesaj verilir.
 */
class VoskRecognizer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val listener: VoskListener
) {
    companion object {
        private const val TAG = "VoskRecognizer"
    }

    private val lock = Any()
    @Volatile private var model: Model? = null
    @Volatile private var speechService: SpeechService? = null
    /** onResult sonucu iletildiyse onFinalResult'ın hata göstermesini engeller */
    @Volatile private var resultDelivered = false

    val isModelLoaded: Boolean get() = model != null
    val isListening: Boolean get() = speechService != null

    // ── Model yükleme ────────────────────────────────────────────────────────

    fun loadModel() {
        scope.launch {
            try {
                val m = withContext(Dispatchers.IO) {
                    Model(ModelManager.getModelPath(context))
                }
                synchronized(lock) { model = m }
                withContext(Dispatchers.Main) { listener.onReady() }
            } catch (e: Exception) {
                Log.e(TAG, "Model yükleme hatası", e)
                withContext(Dispatchers.Main) {
                    listener.onError("Model yüklenemedi. Lütfen uygulamayı yeniden başlatın.")
                }
            }
        }
    }

    // ── Ses tanıma ───────────────────────────────────────────────────────────

    fun startListening() {
        val m = synchronized(lock) { model } ?: run {
            listener.onError("Model henüz hazır değil, lütfen bekleyin.")
            return
        }

        synchronized(lock) {
            if (speechService != null) return
            try {
                resultDelivered = false
                val recognizer = Recognizer(m, 16_000.0f)
                val svc = SpeechService(recognizer, 16_000.0f)
                svc.startListening(voskListener)
                speechService = svc
            } catch (e: Exception) {
                Log.e(TAG, "Ses servisi başlatma hatası", e)
                listener.onError("Ses servisi başlatılamadı.")
                return
            }
        }
        listener.onListening()
    }

    fun stopListening() {
        synchronized(lock) {
            speechService?.apply { stop(); shutdown() }
            speechService = null
        }
    }

    fun release() {
        synchronized(lock) {
            speechService?.apply { stop(); shutdown() }
            speechService = null
            model?.close()
            model = null
        }
    }

    // ── Vosk iç dinleyici ────────────────────────────────────────────────────

    private val voskListener = object : RecognitionListener {

        override fun onPartialResult(hypothesis: String?) {
            val text = try {
                JSONObject(hypothesis ?: return).optString("partial", "").trim()
            } catch (_: Exception) { "" }
            if (text.isNotBlank()) listener.onPartial(text)
        }

        override fun onResult(hypothesis: String?) {
            val text = extractText(hypothesis)
            if (text.isNotBlank()) {
                resultDelivered = true
                stopListening()
                listener.onResult(text)
            }
        }

        override fun onFinalResult(hypothesis: String?) {
            // onResult zaten sonucu ilettiyse (stopListening bu callback'i tetikler),
            // hata gösterme — komut başarıyla işlendi.
            if (resultDelivered) {
                resultDelivered = false
                return
            }
            val text = extractText(hypothesis)
            stopListening()
            if (text.isNotBlank()) listener.onResult(text)
            else listener.onError("Ses anlaşılamadı, lütfen tekrar deneyin.")
        }

        override fun onError(e: Exception?) {
            Log.e(TAG, "Ses tanıma hatası", e)
            stopListening()
            // e.message UI'ye yansıtılmaz — uygulama iç detayı sızdırmaz
            listener.onError("Ses tanıma hatası oluştu.")
        }

        override fun onTimeout() {
            stopListening()
            listener.onError("Zaman aşımı: lütfen tekrar deneyin.")
        }
    }

    // ── Yardımcı ─────────────────────────────────────────────────────────────

    /** Vosk JSON çıktısından metni çıkarır: {"text": "ahmet artı elli"} → "ahmet artı elli" */
    private fun extractText(hypothesis: String?): String {
        if (hypothesis.isNullOrBlank()) return ""
        return try {
            JSONObject(hypothesis).optString("text", "").trim()
        } catch (_: Exception) {
            // JSON parse hatası — ham metni döndürme, boş dön (güvenli taraf)
            ""
        }
    }
}
