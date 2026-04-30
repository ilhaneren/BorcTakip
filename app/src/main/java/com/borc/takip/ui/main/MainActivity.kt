package com.borc.takip.ui.main

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.borc.takip.R
import com.borc.takip.data.model.Person
import com.borc.takip.data.model.TransactionType
import com.borc.takip.databinding.ActivityMainBinding
import com.borc.takip.ui.detail.PersonDetailActivity
import com.borc.takip.voice.ModelManager
import com.borc.takip.voice.VoiceCommandProcessor
import com.borc.takip.voice.VoiceParseResult
import com.borc.takip.voice.VoskListener
import com.borc.takip.voice.VoskRecognizer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        /**
         * Model yükleme bildiriminin bir kez gösterilmesi için flag.
         * Karanlık mod gibi Activity yeniden oluşturma durumlarında korunur.
         */
        @Volatile private var modelReadyShown = false
    }

    private lateinit var b: ActivityMainBinding
    private val vm: MainViewModel by viewModels()
    private lateinit var personAdapter: PersonAdapter

    // ── Vosk ────────────────────────────────────────────────────────────────
    private lateinit var voskRecognizer: VoskRecognizer

    /** true → ses girişi arama modunda; false → borç komutu modunda */
    @Volatile private var isVoiceSearchMode = false

    /** Sesli arama sonucunu yazmak için SearchView referansı */
    private var searchView: SearchView? = null

    /** Sesli aramadan sonra detaya gidilip geri gelindiğinde aramayı sıfırla */
    private var clearSearchOnResume = false

    // ── Rate limiting: aynı anda birden fazla komut engeli ───────────────────
    @Volatile private var lastCommandTimeMs = 0L
    private val commandDebounceMs = 2_000L

    private val audioPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening() else showSnack(getString(R.string.mic_permission_denied))
    }

    // ── Yaşam döngüsü ────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        // Kaydedilen tema tercihini uygula (flickerı önlemek için super'den önce)
        val isDark = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO,
        )
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)

        setupList()
        setupFab()
        observe()
        initVosk()
    }

    override fun onResume() {
        super.onResume()
        if (clearSearchOnResume) {
            clearSearchOnResume = false
            searchView?.apply {
                setQuery("", false)
                isIconified = true
            }
            vm.search("")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voskRecognizer.release()
    }

    // ── Kurulum ───────────────────────────────────────────────────────────────

    private fun setupList() {
        personAdapter = PersonAdapter(
            onClick = { person -> openDetail(person) },
            onLongClick = { person -> confirmDeletePerson(person); true }
        )
        b.recyclerView.layoutManager = LinearLayoutManager(this)
        b.recyclerView.adapter = personAdapter
    }

    private fun setupFab() {
        // Borç komutu FAB (sağ-alt, mavi)
        b.fabVoice.setOnClickListener {
            when {
                voskRecognizer.isListening -> { isVoiceSearchMode = false; stopListening() }
                voskRecognizer.isModelLoaded -> { isVoiceSearchMode = false; checkPermAndListen() }
                else -> showSnack(getString(R.string.model_not_ready))
            }
        }
        // Sesli arama FAB (sol-alt, yeşil)
        b.fabVoiceSearch.setOnClickListener {
            when {
                voskRecognizer.isListening -> { isVoiceSearchMode = false; stopListening() }
                voskRecognizer.isModelLoaded -> startVoiceSearch()
                else -> showSnack(getString(R.string.model_not_ready))
            }
        }
    }

    private fun observe() {
        vm.persons.observe(this) { list ->
            personAdapter.submitList(list)
            b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        b.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chipDebtors   -> PersonFilter.DEBTORS
                R.id.chipCreditors -> PersonFilter.CREDITORS
                else               -> PersonFilter.ALL
            }
            vm.setPersonFilter(filter)
        }

        vm.currentFilter.observe(this) { filter ->
            val chipId = when (filter) {
                PersonFilter.DEBTORS   -> R.id.chipDebtors
                PersonFilter.CREDITORS -> R.id.chipCreditors
                PersonFilter.ALL       -> R.id.chipAll
            }
            if (b.chipGroupFilter.checkedChipId != chipId) b.chipGroupFilter.check(chipId)
        }

        vm.commandResult.observe(this) { result ->
            result ?: return@observe
            when (result) {
                is CommandResult.Success -> {
                    val typeStr = if (result.transaction.type == TransactionType.CREDIT) "eklendi" else "silindi"
                    showSnack("${result.person.name}: ${VoiceCommandProcessor.formatAmount(result.transaction.amount)} borç $typeStr")
                }
                is CommandResult.Error -> showSnack("Hata: ${result.message}")
            }
            vm.clearCommandResult()
        }

        vm.personUndoMessage.observe(this) { msg ->
            msg ?: return@observe
            Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.action_undo)) { vm.undoDeletePerson() }
                .show()
            vm.clearPersonUndoMessage()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val sv = menu.findItem(R.id.action_search)?.actionView as? SearchView
        sv?.apply {
            queryHint = getString(R.string.search_hint)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(q: String?) = true
                override fun onQueryTextChange(q: String?): Boolean {
                    vm.search(q ?: "")
                    return true
                }
            })
        }
        searchView = sv
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val isDark = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("dark_mode", false)
        menu.findItem(R.id.action_dark_mode)?.isChecked = isDark
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                ManualEntryBottomSheet().show(supportFragmentManager, ManualEntryBottomSheet.TAG)
                true
            }
            R.id.action_stats -> { showStats(); true }
            R.id.action_export_csv -> { exportCsv(); true }
            R.id.action_dark_mode -> { toggleDarkMode(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ── Vosk başlatma ─────────────────────────────────────────────────────────

    private fun initVosk() {
        voskRecognizer = VoskRecognizer(this, lifecycleScope, object : VoskListener {
            override fun onReady() {
                // Karanlık mod gibi Activity yeniden oluşturmalarında tekrar gösterme
                if (!modelReadyShown) {
                    modelReadyShown = true
                    showSnack(getString(R.string.model_ready))
                }
                b.tvVoiceStatus.visibility = View.GONE
            }
            override fun onListening() {
                if (!isVoiceSearchMode) b.fabVoice.setIconResource(R.drawable.ic_mic_off)
                b.tvVoiceStatus.visibility = View.VISIBLE
                b.tvVoiceStatus.text = if (isVoiceSearchMode)
                    getString(R.string.voice_search_speak)
                else
                    getString(R.string.voice_speak)
                b.tvPartialResult.text = ""
                b.tvPartialResult.visibility = View.VISIBLE
            }
            override fun onPartial(text: String) {
                b.tvPartialResult.text = text
                // Arama modunda: partial metin anlık arar (liste gerçek zamanlı filtreler)
                if (isVoiceSearchMode) vm.search(text)
            }
            override fun onResult(text: String) {
                b.tvVoiceStatus.visibility = View.GONE
                b.tvPartialResult.visibility = View.GONE
                if (isVoiceSearchMode) {
                    isVoiceSearchMode = false
                    clearSearchOnResume = true   // detaydan geri gelince aramayı sıfırla
                    // SearchView'ı aç ve sonucu yaz
                    searchView?.apply {
                        isIconified = false
                        setQuery(text, false)
                    }
                    vm.search(text)
                } else {
                    b.fabVoice.setIconResource(R.drawable.ic_mic)
                    processText(text)
                }
            }
            override fun onError(message: String) {
                b.fabVoice.setIconResource(R.drawable.ic_mic)
                b.tvVoiceStatus.visibility = View.GONE
                b.tvPartialResult.visibility = View.GONE
                if (isVoiceSearchMode) {
                    isVoiceSearchMode = false
                    showSnack(message)
                } else {
                    showRetryDialog(message)
                }
            }
        })

        if (ModelManager.isModelReady(this)) {
            // Model zaten indirilmiş → yükle
            loadModel()
        } else {
            // İlk açılış → indirme teklifi
            showModelDownloadDialog()
        }
    }

    private fun loadModel() {
        voskRecognizer.loadModel()
    }

    // ── Model indirme diyaloğu ────────────────────────────────────────────────

    private fun showModelDownloadDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.model_needed_title))
            .setMessage(getString(R.string.model_needed_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.action_download)) { _, _ -> downloadModel() }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun downloadModel() {
        // İlerleme diyaloğunu kur
        val dialogView = layoutInflater.inflate(R.layout.dialog_download_model, null)
        val tvStatus = dialogView.findViewById<TextView>(R.id.tvDownloadStatus)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val tvProgress = dialogView.findViewById<TextView>(R.id.tvProgressText)

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.model_needed_title))
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.show()

        lifecycleScope.launch {
            try {
                ModelManager.downloadAndExtract(this@MainActivity) { downloaded, total ->
                    if (total > 0) {
                        val pct = (downloaded * 100 / total).toInt()
                        progressBar.progress = pct
                        tvProgress.text = getString(
                            R.string.model_download_progress,
                            pct,
                            downloaded / 1_048_576,
                            total / 1_048_576
                        )
                    } else {
                        // Boyut bilinmiyorsa belirsiz ilerleme
                        progressBar.isIndeterminate = true
                        tvProgress.text = getString(R.string.model_download_progress_simple, downloaded / 1_048_576)
                    }
                    // İndirme bitti, şimdi açılıyor
                    tvStatus.text = getString(R.string.model_extracting)
                }
                dialog.dismiss()
                loadModel()
            } catch (e: Exception) {
                android.util.Log.e("ModelDownload", "İndirme hatası", e)
                dialog.dismiss()
                // Kullanıcıya genel mesaj; detay Logcat'te
                showSnack(getString(R.string.model_download_error_generic))
            }
        }
    }

    // ── Ses tanıma ────────────────────────────────────────────────────────────

    private fun checkPermAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) startListening()
        else audioPerm.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startListening() {
        if (!isVoiceSearchMode) {
            b.fabVoice.setIconResource(R.drawable.ic_mic_off)
            b.tvVoiceStatus.text = getString(R.string.voice_listening)
        }
        b.tvVoiceStatus.visibility = View.VISIBLE
        voskRecognizer.startListening()
    }

    private fun startVoiceSearch() {
        isVoiceSearchMode = true
        checkPermAndListen()
    }

    private fun stopListening() {
        b.fabVoice.setIconResource(R.drawable.ic_mic)
        b.tvVoiceStatus.visibility = View.GONE
        b.tvPartialResult.visibility = View.GONE
        isVoiceSearchMode = false
        voskRecognizer.stopListening()
    }

    // ── Komut işleme ─────────────────────────────────────────────────────────

    private fun processText(text: String) {
        // Rate limiting: 2 saniyede bir komut işlenir
        val now = System.currentTimeMillis()
        if (now - lastCommandTimeMs < commandDebounceMs) {
            showSnack("Lütfen biraz bekleyin.")
            return
        }
        lastCommandTimeMs = now

        when (val result = VoiceCommandProcessor.parse(text)) {
            is VoiceParseResult.Success -> {
                val cmd = result.command
                val typeStr = if (cmd.type == TransactionType.CREDIT)
                    getString(R.string.op_add) else getString(R.string.op_remove)

                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.dialog_confirm_title))
                    .setMessage(
                        getString(
                            R.string.dialog_confirm_message,
                            cmd.personName,
                            typeStr,
                            VoiceCommandProcessor.formatAmount(cmd.amount)
                        )
                    )
                    .setPositiveButton(getString(R.string.action_confirm)) { _, _ ->
                        vm.executeVoiceCommand(cmd)
                    }
                    .setNeutralButton(getString(R.string.action_retry)) { _, _ -> checkPermAndListen() }
                    .setNegativeButton(getString(R.string.action_cancel), null)
                    .show()
            }
            is VoiceParseResult.Error -> showRetryDialog(result.reason)
        }
    }

    private fun showRetryDialog(message: String) {
        val example = getString(R.string.dialog_error_example)
        val fullText = "$message\n\n$example"
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf(example)
        val end = start + example.length
        spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(
            ForegroundColorSpan(getColor(R.color.colorPrimary)),
            start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_error_title))
            .setMessage(spannable)
            .setPositiveButton(getString(R.string.action_retry)) { _, _ -> checkPermAndListen() }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    // ── Diğer işlemler ───────────────────────────────────────────────────────

    private fun openDetail(person: Person) {
        startActivity(
            Intent(this, PersonDetailActivity::class.java).apply {
                putExtra(PersonDetailActivity.EXTRA_PERSON_ID, person.id)
            }
        )
    }

    private fun confirmDeletePerson(person: Person) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_delete_person_title))
            .setMessage(getString(R.string.dialog_delete_person_message, person.name))
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                vm.deletePerson(person)
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    // ── İstatistik diyaloğu ───────────────────────────────────────────────────

    private fun showStats() {
        lifecycleScope.launch {
            val stats = vm.getStats()
            val fmt = { d: Double -> VoiceCommandProcessor.formatAmount(d) }
            val sb = StringBuilder()
            sb.appendLine("👥 Toplam kişi: ${stats.totalPersons}")
            sb.appendLine("📌 Aktif bakiyeli: ${stats.activeCount}")
            sb.appendLine()
            sb.appendLine("💸 Toplam borç: ${fmt(stats.totalDebt)}")
            sb.appendLine("💰 Toplam alacak: ${fmt(stats.totalCredit)}")
            val netLabel = if (stats.netBalance >= 0)
                "+ ${fmt(stats.netBalance)}" else "- ${fmt(-stats.netBalance)}"
            sb.appendLine("🧾 Net: $netLabel")
            stats.topDebtor?.let {
                sb.appendLine()
                sb.appendLine("🏆 En yüksek borçlu:")
                sb.append("    ${it.name}  (${fmt(it.totalBalance)})")
            }

            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(getString(R.string.action_stats))
                .setMessage(sb.toString())
                .setPositiveButton(getString(R.string.action_cancel), null)
                .show()
        }
    }

    // ── CSV dışa aktarım ─────────────────────────────────────────────────────

    private fun exportCsv() {
        lifecycleScope.launch {
            try {
                val csv = vm.exportCsv()
                val dir = File(cacheDir, "exports").also { it.mkdirs() }
                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale("tr", "TR")).format(Date())
                val file = File(dir, "borc_takip_$stamp.csv")
                file.writeText(csv, Charsets.UTF_8)

                val uri = FileProvider.getUriForFile(
                    this@MainActivity,
                    "$packageName.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Borç Takip $stamp")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.action_export_csv)))
            } catch (e: Exception) {
                showSnack("Dışa aktarma başarısız: ${e.message}")
            }
        }
    }

    // ── Karanlık mod ──────────────────────────────────────────────────────────

    private fun toggleDarkMode() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val newIsDark = !prefs.getBoolean("dark_mode", false)
        prefs.edit { putBoolean("dark_mode", newIsDark) }
        AppCompatDelegate.setDefaultNightMode(
            if (newIsDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        // AppCompatDelegate otomatik olarak Activity'yi yeniden oluşturur
    }

    private fun showSnack(msg: String) = Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG).show()
}
