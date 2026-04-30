package com.borc.takip.data.repository

import androidx.room.withTransaction
import com.borc.takip.data.db.AppDatabase
import com.borc.takip.data.model.Person
import com.borc.takip.data.model.Transaction
import com.borc.takip.data.model.TransactionType
import java.util.Locale

class DebtRepository(private val database: AppDatabase) {

    private val personDao      = database.personDao()
    private val transactionDao = database.transactionDao()

    private companion object {
        val TR = Locale("tr", "TR")
        val NAME_REGEX = Regex("^[\\p{L}\\p{N}\\s.\\-']+\$")
        const val MAX_NOTE_LENGTH = 500
        fun String.trLower() = this.lowercase(TR).trim()
    }

    // ── Sorgular ────────────────────────────────────────────────────────────

    fun getAllPersons()   = personDao.getAllPersons()
    fun getDebtors()     = personDao.getDebtors()
    fun getCreditors()   = personDao.getCreditors()

    fun searchPersons(query: String)   = personDao.searchPersons(query.trLower())
    fun searchDebtors(query: String)   = personDao.searchDebtors(query.trLower())
    fun searchCreditors(query: String) = personDao.searchCreditors(query.trLower())

    suspend fun getAllPersonNames() = personDao.getAllPersonNames()

    fun getTransactionsByPerson(personId: Long) =
        transactionDao.getTransactionsByPerson(personId)

    suspend fun getPersonById(id: Long) = personDao.getPersonById(id)

    fun getPersonByIdLive(id: Long) = personDao.getPersonByIdLive(id)

    // ── Kişi oluştur / bul ──────────────────────────────────────────────────

    suspend fun findOrCreatePerson(name: String): Person {
        val normalized = name.trLower()
        personDao.getPersonByExactName(normalized)?.let { return it }
        personDao.findPersonsByName(normalized).firstOrNull()?.let { return it }

        val display = name.trim().split(" ").joinToString(" ") {
            it.replaceFirstChar { c -> c.uppercase() }
        }
        val id = personDao.insertPerson(Person(name = display, normalizedName = normalized))
        return personDao.getPersonById(id)!!
    }

    // ── İşlem ekle (atomic) ─────────────────────────────────────────────────

    suspend fun processVoiceCommand(
        name: String, type: TransactionType, amount: Double, voiceText: String
    ): Result<Pair<Person, Transaction>> =
        addTransaction(name, type, amount, voiceText = voiceText)

    suspend fun processManualEntry(
        name: String, type: TransactionType, amount: Double, note: String?
    ): Result<Pair<Person, Transaction>> =
        addTransaction(name, type, amount, note = note.orEmpty())

    suspend fun addTransaction(
        name: String,
        type: TransactionType,
        amount: Double,
        note: String = "",
        voiceText: String = ""
    ): Result<Pair<Person, Transaction>> = runCatching {
        require(note.length <= MAX_NOTE_LENGTH) { "Not çok uzun (max $MAX_NOTE_LENGTH karakter)" }

        val person = findOrCreatePerson(name)
        val delta  = if (type == TransactionType.CREDIT) amount else -amount
        val tx     = Transaction(
            personId  = person.id,
            amount    = amount,
            type      = type,
            note      = note,
            voiceText = voiceText
        )

        // Atomic: işlem + bakiye aynı DB transaction'ında
        val txId = database.withTransaction {
            val id = transactionDao.insertTransaction(tx)
            personDao.updateBalance(person.id, delta)
            id
        }

        val updated = personDao.getPersonById(person.id)!!
        Pair(updated, tx.copy(id = txId))
    }

    // ── İşlem sil (atomic) + undo ────────────────────────────────────────────

    suspend fun deleteTransaction(transaction: Transaction) {
        val delta = if (transaction.type == TransactionType.CREDIT)
            -transaction.amount else transaction.amount
        database.withTransaction {
            transactionDao.deleteTransaction(transaction)
            personDao.updateBalance(transaction.personId, delta)
        }
    }

    suspend fun undoDeleteTransaction(transaction: Transaction) {
        val delta = if (transaction.type == TransactionType.CREDIT)
            transaction.amount else -transaction.amount
        database.withTransaction {
            transactionDao.insertTransactionReplace(transaction)
            personDao.updateBalance(transaction.personId, delta)
        }
    }

    // ── Kişi sil (atomic) + undo ─────────────────────────────────────────────

    /** Person silinmeden önce çağrılmalı — CASCADE öncesi snapshot */
    suspend fun getTransactionsSnapshot(personId: Long): List<Transaction> =
        transactionDao.getTransactionsByPersonSync(personId)

    suspend fun deletePerson(person: Person) = personDao.deletePerson(person)

    suspend fun undoDeletePerson(person: Person, transactions: List<Transaction>) {
        database.withTransaction {
            personDao.insertPersonReplace(person)
            if (transactions.isNotEmpty()) {
                transactionDao.insertTransactionsReplace(transactions)
            }
        }
    }

    // ── Kişi yeniden adlandır ────────────────────────────────────────────────

    suspend fun renamePerson(person: Person, newName: String): Result<Person> = runCatching {
        val trimmed = newName.trim()
        require(trimmed.isNotEmpty()) { "İsim boş olamaz" }
        require(trimmed.length <= 100) { "İsim çok uzun (en fazla 100 karakter)" }
        require(NAME_REGEX.matches(trimmed)) { "İsimde geçersiz karakter var" }

        val normalized = trimmed.trLower()
        val existing = personDao.getPersonByExactName(normalized)
        require(existing == null || existing.id == person.id) { "Bu isimde bir kişi zaten var" }

        val display = trimmed.split(" ").joinToString(" ") {
            it.replaceFirstChar { c -> c.uppercase() }
        }
        val updated = person.copy(
            name           = display,
            normalizedName = normalized,
            updatedAt      = System.currentTimeMillis()
        )
        personDao.updatePerson(updated)
        updated
    }

    // ── İstatistik ────────────────────────────────────────────────────────────

    suspend fun getStats(): Stats {
        val persons = personDao.getAllPersonsSync()
        val totalDebt    = persons.filter { it.totalBalance > 0 }.sumOf { it.totalBalance }
        val totalCredit  = persons.filter { it.totalBalance < 0 }.sumOf { -it.totalBalance }
        val topDebtor    = persons.filter { it.totalBalance > 0 }
            .maxByOrNull { it.totalBalance }
        val activeCount  = persons.count { it.totalBalance != 0.0 }
        return Stats(totalDebt, totalCredit, totalDebt - totalCredit, topDebtor, activeCount, persons.size)
    }

    data class Stats(
        val totalDebt: Double,
        val totalCredit: Double,
        val netBalance: Double,
        val topDebtor: Person?,
        val activeCount: Int,
        val totalPersons: Int
    )

    // ── CSV dışa aktarım ─────────────────────────────────────────────────────

    suspend fun exportCsv(): String {
        val persons      = personDao.getAllPersonsSync()
        val sb           = StringBuilder()
        sb.appendLine("Kişi Adı,İşlem Türü,Tutar (TL),Tarih,Not,Ses Metni")
        val dateFmt = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))

        for (person in persons) {
            val txList = transactionDao.getTransactionsByPersonSync(person.id)
            for (tx in txList) {
                val type = if (tx.type == TransactionType.CREDIT) "Borç Eklendi" else "Ödeme/İndirim"
                val date = dateFmt.format(java.util.Date(tx.createdAt))
                sb.appendLine(
                    "${csvEscape(person.name)},$type,${tx.amount},$date," +
                    "${csvEscape(tx.note)},${csvEscape(tx.voiceText)}"
                )
            }
        }
        return sb.toString()
    }

    private fun csvEscape(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n'))
            "\"${value.replace("\"", "\"\"")}\""
        else value
}
