package com.borc.takip.voice

import com.borc.takip.data.model.TransactionType

data class VoiceCommand(
    val personName: String,
    val type: TransactionType,
    val amount: Double,
    val rawText: String,
)

sealed class VoiceParseResult {
    data class Success(val command: VoiceCommand) : VoiceParseResult()
    data class Error(val reason: String) : VoiceParseResult()
}

object VoiceCommandProcessor {

    // ── Güvenlik sınırları ───────────────────────────────────────────────────
    private const val MAX_INPUT_LENGTH = 300      // ses tanıma maks karakter
    private const val MAX_NAME_LENGTH = 100       // kişi adı maks karakter
    private const val MAX_AMOUNT = 10_000_000.0  // 10 milyon TL üst sınır
    private const val MIN_AMOUNT = 0.01          // kuruş alt sınır

    // Türkçe + Latin harf, rakam, boşluk, tire, nokta, tek tırnak
    private val NAME_REGEX = Regex("^[\\p{L}\\p{N}\\s.\\-']+$")

    // ── Çok kelimeli anahtar kelimeler önce aranmalı (daha spesifik) ───────────
    private val CREDIT_KEYWORDS = listOf(
        // Çok kelimeli (öncelikli — daha spesifik)
        "borç ekle", "borc ekle",
        "borç eklendi", "borc eklendi",
        "borç oldu", "borc oldu",
        "borç aldı", "borc aldi",
        "borçlandı", "borclandı", "borclansin", "borçlansin",
        "borçlan", "borclan",
        "geri ver", "geri verdi",
        // Tek kelimeli
        "artı", "arti",
        "artır", "artir",
        "arttır", "arttir",
        "arttı", "artti",
        "artıyor", "artiyor",
        "artar",
        "ekle", "ekliyor", "eklensin", "eklendi",
        "yükle", "yukle",
        "plus",
        "pozitif",
        "aldı", "aldi",
        "alındı", "alindi",
        "verildi"
    )
    private val DEBIT_KEYWORDS = listOf(
        // Çok kelimeli (öncelikli — daha spesifik)
        "borçtan düş", "borctan dus",
        "borçtan düşür", "borctan dusur",
        "borç düş", "borc dus",
        "borç düşür", "borc dusur",
        "borç düştü", "borc dustu",
        "borç sil", "borc sil",
        "borç kapandı", "borc kapandi",
        "geri ödedi", "geri odedi",
        "geri öde", "geri ode",
        "geri ödendi", "geri odendi",
        "geri aldı", "geri aldi",
        "tahsil etti", "tahsil edildi",
        // Tek kelimeli
        "eksi",
        "çıkar", "cikar",
        "çıkart", "cikart",
        "çıktı", "cikti",
        "düş", "dus",
        "düşür", "dusur",
        "düştü", "dustu",
        "öde", "ode",
        "ödedi", "odedi",
        "ödendi", "odendi",
        "ödeme", "odeme",
        "ödeniyor", "odeniyor",
        "sil", "silindi",
        "kapat",
        "kapandı", "kapandi",
        "kapattı", "kapatti",
        "azalt",
        "eksilt",
        "indir",
        "mahsup",
        "tahsil",
        "minus",
        "negatif"
    )

    // Rakam değerleri — parse için ayrı kategoriler
    private val UNITS = mapOf(
        "bir" to 1L, "iki" to 2L, "üç" to 3L, "uc" to 3L,
        "dört" to 4L, "dort" to 4L, "beş" to 5L, "bes" to 5L,
        "altı" to 6L, "alti" to 6L, "yedi" to 7L,
        "sekiz" to 8L, "dokuz" to 9L
    )
    private val TENS = mapOf(
        "on" to 10L, "yirmi" to 20L, "otuz" to 30L,
        "kırk" to 40L, "kirk" to 40L, "elli" to 50L,
        "altmış" to 60L, "altmis" to 60L,
        "yetmiş" to 70L, "yetmis" to 70L,
        "seksen" to 80L, "doksan" to 90L
    )
    private val HUNDREDS  = setOf("yüz", "yuz")
    private val THOUSANDS = setOf("bin")
    private val MILLIONS  = setOf("milyon")

    // Geriye dönük uyumluluk için tek kelime haritası
    private val TURKISH_NUMBERS: Map<String, Double> =
        (UNITS.mapValues { it.value.toDouble() } +
         TENS.mapValues  { it.value.toDouble() } +
         mapOf("sıfır" to 0.0, "sfir" to 0.0,
               "yüz" to 100.0, "yuz" to 100.0,
               "bin" to 1000.0, "milyon" to 1_000_000.0))

    fun parse(text: String): VoiceParseResult {
        // ── Giriş uzunluk kontrolü ─────────────────────────────────────────
        if (text.isBlank()) {
            return VoiceParseResult.Error("Ses tanınamadı, lütfen tekrar deneyin.")
        }
        if (text.length > MAX_INPUT_LENGTH) {
            return VoiceParseResult.Error("Komut çok uzun. Lütfen kısa tutun.")
        }

        val lower = text.lowercase(java.util.Locale("tr", "TR")).trim()

        // ── Anahtar kelimeyi bul ───────────────────────────────────────────
        val creditMatch = findKeyword(lower, CREDIT_KEYWORDS)
        val debitMatch  = findKeyword(lower, DEBIT_KEYWORDS)

        val (keywordIdx, keywordLen, type) = when {
            (creditMatch != null && debitMatch != null) ->
                if (creditMatch.first <= debitMatch.first)
                    Triple(creditMatch.first, creditMatch.second, TransactionType.CREDIT)
                else
                    Triple(debitMatch.first, debitMatch.second, TransactionType.DEBIT)
            creditMatch != null -> Triple(creditMatch.first, creditMatch.second, TransactionType.CREDIT)
            debitMatch  != null -> Triple(debitMatch.first,  debitMatch.second,  TransactionType.DEBIT)
            else -> return VoiceParseResult.Error(
                "Komut kelimesi bulunamadı.\nÖrnek: \"Ahmet artı yüz\" veya \"Ahmet borç düş elli\""
            )
        }

        val namePart   = text.substring(0, keywordIdx).trim()
        val amountPart = text.substring(keywordIdx + keywordLen).trim()

        // ── İsim doğrulaması ──────────────────────────────────────────────
        if (namePart.isEmpty()) {
            return VoiceParseResult.Error("Kişi adı anlaşılamadı.\nÖrnek: \"Ahmet artı 100\"")
        }
        if (namePart.length > MAX_NAME_LENGTH) {
            return VoiceParseResult.Error("Kişi adı çok uzun (en fazla $MAX_NAME_LENGTH karakter).")
        }
        if (!NAME_REGEX.matches(namePart)) {
            return VoiceParseResult.Error("Kişi adında geçersiz karakter var.")
        }

        // ── Miktar doğrulaması ─────────────────────────────────────────────
        val amount = parseAmount(amountPart)
            ?: return VoiceParseResult.Error(
                "Miktar anlaşılamadı: \"$amountPart\"\nLütfen tekrar deneyin."
            )

        if (amount < MIN_AMOUNT) {
            return VoiceParseResult.Error("Miktar en az ${formatAmount(MIN_AMOUNT)} olmalıdır.")
        }
        if (amount > MAX_AMOUNT) {
            return VoiceParseResult.Error(
                "Miktar çok yüksek (en fazla ${formatAmount(MAX_AMOUNT)})."
            )
        }
        if (amount.isNaN() || amount.isInfinite()) {
            return VoiceParseResult.Error("Geçersiz miktar.")
        }

        val displayName = namePart.split(" ").joinToString(" ") {
            it.replaceFirstChar { c -> c.uppercase() }
        }

        return VoiceParseResult.Success(
            VoiceCommand(
                personName = displayName,
                type = type,
                amount = amount,
                rawText = text
            )
        )
    }

    private fun findKeyword(lower: String, keywords: List<String>): Pair<Int, Int>? {
        var best: Pair<Int, Int>? = null
        for (kw in keywords) {
            val patterns = listOf(" $kw ", " $kw", "$kw ")
            for (pattern in patterns) {
                val idx = lower.indexOf(pattern)
                if (idx >= 0) {
                    val actualStart = if (pattern.startsWith(" ")) idx + 1 else idx
                    if (best == null || actualStart < best.first) {
                        best = Pair(actualStart, kw.length)
                    }
                    break
                }
            }
        }
        return best
    }

    private fun parseAmount(text: String): Double? {
        if (text.isBlank()) return null

        val cleaned = text.trim()
            .replace(",", ".")
            .replace("tl", "", ignoreCase = true)
            .replace("lira", "", ignoreCase = true)
            .replace("₺", "")
            .trim()

        cleaned.toDoubleOrNull()?.let { v -> if (v > 0 && !v.isNaN() && !v.isInfinite()) return v }

        val lowerClean = cleaned.lowercase()
        TURKISH_NUMBERS[lowerClean]?.let { return it }
        return parseCombinedTurkish(lowerClean)
    }

    /**
     * Çok kelimeli Türkçe sayıları parse eder.
     * Örnekler: "yüz elli" → 150, "iki bin beş yüz elli" → 2550,
     *           "yüz elli beş" → 155, "iki yüz" → 200
     *
     * Algoritma: soldan sağa tara, "yüz/bin/milyon" çarpan olarak işle.
     *   current: şu an biriktirilen değer
     *   total: tamamlanan dilimler toplamı
     */
    private fun parseCombinedTurkish(text: String): Double? {
        val words = text.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.size < 2) return null

        var total = 0L
        var current = 0L

        for (word in words) {
            when {
                UNITS[word] != null -> current += UNITS[word]!!
                TENS[word] != null  -> current += TENS[word]!!
                word in HUNDREDS -> {
                    current = if (current == 0L) 100L else current * 100L
                }
                word in THOUSANDS -> {
                    total += if (current == 0L) 1000L else current * 1000L
                    current = 0L
                }
                word in MILLIONS -> {
                    total += if (current == 0L) 1_000_000L else current * 1_000_000L
                    current = 0L
                }
                else -> return null  // Tanınmayan kelime
            }
        }
        total += current
        return if (total > 0) total.toDouble() else null
    }

    fun formatAmount(amount: Double): String =
        if (amount == amount.toLong().toDouble()) "${amount.toLong()} ₺"
        else String.format(java.util.Locale("tr", "TR"), "%.2f ₺", amount)

    fun transactionTypeText(type: TransactionType) =
        if (type == TransactionType.CREDIT) "Borç Eklendi" else "Ödeme / İndirim"
}
