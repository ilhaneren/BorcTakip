package com.borc.takip.ui.detail

import android.app.Application
import androidx.lifecycle.*
import com.borc.takip.data.db.AppDatabase
import com.borc.takip.data.model.Person
import com.borc.takip.data.model.Transaction
import com.borc.takip.data.model.TransactionType
import com.borc.takip.data.repository.DebtRepository
import com.borc.takip.voice.VoiceCommandProcessor.formatAmount
import kotlinx.coroutines.launch

class PersonDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DebtRepository(AppDatabase.getInstance(application))

    private val _personId = MutableLiveData<Long>()

    val person: LiveData<Person?> = _personId.switchMap { id ->
        repository.getPersonByIdLive(id)
    }

    val transactions: LiveData<List<Transaction>> = _personId.switchMap { id ->
        repository.getTransactionsByPerson(id)
    }

    fun loadPerson(personId: Long) { _personId.value = personId }

    // ── İşlem sil + undo ────────────────────────────────────────────────────

    private var lastDeletedTransaction: Transaction? = null

    private val _snackMessage = MutableLiveData<String?>()
    val snackMessage: LiveData<String?> = _snackMessage

    fun deleteTransaction(transaction: Transaction) {
        lastDeletedTransaction = transaction
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _snackMessage.value = "İşlem silindi"
        }
    }

    fun undoDeleteTransaction() {
        val tx = lastDeletedTransaction ?: return
        lastDeletedTransaction = null
        viewModelScope.launch { repository.undoDeleteTransaction(tx) }
    }

    fun clearSnackMessage() { _snackMessage.value = null }

    // ── Hızlı işlem (FAB butonları) ─────────────────────────────────────────

    fun quickTransaction(type: TransactionType, amount: Double) {
        val current = person.value ?: return
        viewModelScope.launch {
            repository.addTransaction(name = current.name, type = type, amount = amount).fold(
                onSuccess = {
                    val sign = if (type == TransactionType.CREDIT) "+" else "-"
                    _snackMessage.value = "${current.name}: $sign${formatAmount(amount)}"
                },
            ) { _snackMessage.value = "Hata: ${it.message}" }
        }
    }

    // ── Yeniden adlandır ────────────────────────────────────────────────────

    private val _renameResult = MutableLiveData<RenameResult?>()
    val renameResult: LiveData<RenameResult?> = _renameResult

    fun renamePerson(newName: String) {
        val current = person.value ?: return
        viewModelScope.launch {
            repository.renamePerson(current, newName).fold(
                onSuccess = { _renameResult.value = RenameResult.Success(it.name) },
            ) { _renameResult.value = RenameResult.Error(it.message ?: "Hata") }
        }
    }

    fun clearRenameResult() { _renameResult.value = null }
}

sealed class RenameResult {
    data class Success(val newName: String) : RenameResult()
    data class Error(val message: String) : RenameResult()
}
