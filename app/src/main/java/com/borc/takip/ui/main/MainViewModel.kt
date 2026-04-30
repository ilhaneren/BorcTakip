package com.borc.takip.ui.main

import android.app.Application
import androidx.lifecycle.*
import com.borc.takip.data.db.AppDatabase
import com.borc.takip.data.model.Person
import com.borc.takip.data.model.Transaction
import com.borc.takip.data.model.TransactionType
import com.borc.takip.data.repository.DebtRepository
import com.borc.takip.voice.VoiceCommand
import kotlinx.coroutines.launch

sealed class CommandResult {
    data class Success(val person: Person, val transaction: Transaction) : CommandResult()
    data class Error(val message: String) : CommandResult()
}

enum class PersonFilter { ALL, DEBTORS, CREDITORS }

private data class SearchFilter(val query: String = "", val personFilter: PersonFilter = PersonFilter.ALL)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = DebtRepository(AppDatabase.getInstance(application))

    private val searchFilter = MutableLiveData(SearchFilter())

    val persons: LiveData<List<Person>> = searchFilter.switchMap { f ->
        val q = f.query
        when (f.personFilter) {
            PersonFilter.ALL      -> if (q.isBlank()) repository.getAllPersons()   else repository.searchPersons(q)
            PersonFilter.DEBTORS  -> if (q.isBlank()) repository.getDebtors()     else repository.searchDebtors(q)
            PersonFilter.CREDITORS -> if (q.isBlank()) repository.getCreditors()  else repository.searchCreditors(q)
        }
    }

    val currentFilter: LiveData<PersonFilter> = searchFilter.map { it.personFilter }

    private val _commandResult = MutableLiveData<CommandResult?>()
    val commandResult: LiveData<CommandResult?> = _commandResult

    // ── Undo: kişi silme ────────────────────────────────────────────────────
    private var lastDeletedPerson: Person? = null
    private var lastDeletedPersonTxs: List<Transaction> = emptyList()

    private val _personUndoMessage = MutableLiveData<String?>()
    val personUndoMessage: LiveData<String?> = _personUndoMessage

    // ── Arama + filtre ──────────────────────────────────────────────────────

    fun search(query: String) {
        searchFilter.value = (searchFilter.value ?: SearchFilter()).copy(query = query)
    }

    fun setPersonFilter(filter: PersonFilter) {
        searchFilter.value = (searchFilter.value ?: SearchFilter()).copy(personFilter = filter)
    }

    // ── Sesli komut ─────────────────────────────────────────────────────────

    fun executeVoiceCommand(command: VoiceCommand) {
        viewModelScope.launch {
            repository.processVoiceCommand(
                name      = command.personName,
                type      = command.type,
                amount    = command.amount,
                voiceText = command.rawText,
            ).fold(
                onSuccess = { (p, tx) -> _commandResult.value = CommandResult.Success(p, tx) },
            ) { _commandResult.value = CommandResult.Error(it.message ?: "Bilinmeyen hata") }
        }
    }

    fun clearCommandResult() { _commandResult.value = null }

    // ── Manuel giriş ────────────────────────────────────────────────────────

    fun submitManualEntry(name: String, type: TransactionType, amount: Double, note: String?) {
        viewModelScope.launch {
            repository.processManualEntry(name, type, amount, note).fold(
                onSuccess = { (p, tx) -> _commandResult.value = CommandResult.Success(p, tx) },
            ) { _commandResult.value = CommandResult.Error(it.message ?: "Bilinmeyen hata") }
        }
    }

    suspend fun getPersonNames(): List<String> = repository.getAllPersonNames()

    // ── Kişi sil + undo ─────────────────────────────────────────────────────

    fun deletePerson(person: Person) {
        viewModelScope.launch {
            lastDeletedPersonTxs = repository.getTransactionsSnapshot(person.id)
            lastDeletedPerson    = person
            repository.deletePerson(person)
            _personUndoMessage.value = "${person.name} silindi"
        }
    }

    fun undoDeletePerson() {
        val p  = lastDeletedPerson  ?: return
        val txs = lastDeletedPersonTxs
        lastDeletedPerson    = null
        lastDeletedPersonTxs = emptyList()
        viewModelScope.launch { repository.undoDeletePerson(p, txs) }
    }

    fun clearPersonUndoMessage() { _personUndoMessage.value = null }

    // ── İstatistik ───────────────────────────────────────────────────────────

    suspend fun getStats() = repository.getStats()

    // ── CSV dışa aktarım ─────────────────────────────────────────────────────

    suspend fun exportCsv() = repository.exportCsv()
}
