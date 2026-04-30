package com.borc.takip.ui.detail

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.borc.takip.R
import com.borc.takip.data.model.TransactionType
import com.borc.takip.databinding.ActivityPersonDetailBinding
import com.borc.takip.voice.VoiceCommandProcessor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.math.abs

class PersonDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PERSON_ID = "person_id"
    }

    private lateinit var b: ActivityPersonDetailBinding
    private val vm: PersonDetailViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPersonDetailBinding.inflate(layoutInflater)
        setContentView(b.root)

        val personId = intent.getLongExtra(EXTRA_PERSON_ID, -1L)

        // Geçersiz ID ile açılmayı engelle (kötü niyetli Intent koruması)
        if (personId <= 0L) { finish(); return }

        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Başlık DB'den gelen person.name ile observe()'de set edilir
        // (Intent extra'dan gelen değere güvenilmez)

        adapter = TransactionAdapter { tx ->
            vm.deleteTransaction(tx)
            // Snackbar observe'de gösterilecek, undo oradan
        }
        b.recyclerView.layoutManager = LinearLayoutManager(this)
        b.recyclerView.adapter = adapter
        setupSwipeToDelete()

        vm.loadPerson(personId)
        observe()
        setupFabs()
    }

    private fun observe() {
        vm.person.observe(this) { person ->
            person ?: return@observe
            supportActionBar?.title = person.name

            val bal = person.totalBalance
            b.tvTotalBalance.text = VoiceCommandProcessor.formatAmount(abs(bal))

            when {
                bal > 0 -> {
                    b.tvBalanceLabel.text = getString(R.string.label_total_debt)
                    b.tvTotalBalance.setTextColor(getColor(R.color.color_debt))
                    b.tvBalanceLabel.setTextColor(getColor(R.color.color_debt))
                    b.balanceCard.setCardBackgroundColor(getColor(R.color.color_debt_bg))
                }
                bal < 0 -> {
                    b.tvBalanceLabel.text = getString(R.string.label_total_credit)
                    b.tvTotalBalance.setTextColor(getColor(R.color.color_credit))
                    b.tvBalanceLabel.setTextColor(getColor(R.color.color_credit))
                    b.balanceCard.setCardBackgroundColor(getColor(R.color.color_credit_bg))
                }
                else -> {
                    b.tvBalanceLabel.text = getString(R.string.label_settled)
                    b.tvTotalBalance.text = "0 ₺"
                    b.tvTotalBalance.setTextColor(getColor(R.color.color_neutral))
                    b.tvBalanceLabel.setTextColor(getColor(R.color.color_neutral))
                    b.balanceCard.setCardBackgroundColor(getColor(R.color.color_neutral_bg))
                }
            }
        }

        vm.transactions.observe(this) { list ->
            adapter.submitList(list)
            b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        vm.renameResult.observe(this) { result ->
            result ?: return@observe
            val msg = when (result) {
                is RenameResult.Success -> getString(R.string.rename_success, result.newName)
                is RenameResult.Error -> result.message
            }
            Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG).show()
            vm.clearRenameResult()
        }

        vm.snackMessage.observe(this) { msg ->
            msg ?: return@observe
            val snack = Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG)
            if (msg == "İşlem silindi") {
                snack.setAction(getString(R.string.action_undo)) { vm.undoDeleteTransaction() }
            }
            snack.show()
            vm.clearSnackMessage()
        }
    }

    private fun setupSwipeToDelete() {
        val swipePaint = Paint()
        val deleteIcon = ContextCompat.getDrawable(this, R.drawable.ic_delete)!!
            .mutate().also { it.setTint(0xFFFFFFFF.toInt()) }

        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                if (pos != RecyclerView.NO_ID.toInt()) {
                    vm.deleteTransaction(adapter.currentList[pos])
                }
            }

            override fun onChildDraw(c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                                     dX: Float, dY: Float, actionState: Int, isActive: Boolean) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    val item = vh.itemView
                    swipePaint.color = getColor(R.color.color_debt)
                    c.drawRect(item.right + dX, item.top.toFloat(),
                        item.right.toFloat(), item.bottom.toFloat(), swipePaint)

                    val margin = (item.height - deleteIcon.intrinsicHeight) / 2
                    deleteIcon.setBounds(
                        item.right - margin - deleteIcon.intrinsicWidth,
                        item.top + margin,
                        item.right - margin,
                        item.bottom - margin
                    )
                    deleteIcon.draw(c)
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(b.recyclerView)
    }

    private fun setupFabs() {
        b.fabAdd.setOnClickListener { showQuickAmountDialog(TransactionType.CREDIT) }
        b.fabDeduct.setOnClickListener { showQuickAmountDialog(TransactionType.DEBIT) }
    }

    private fun showQuickAmountDialog(type: TransactionType) {
        val title = if (type == TransactionType.CREDIT)
            getString(R.string.quick_add_title) else getString(R.string.quick_deduct_title)

        val view = layoutInflater.inflate(R.layout.dialog_quick_amount, null)
        val til = view.findViewById<TextInputLayout>(R.id.tilQuickAmount)
        val et = view.findViewById<TextInputEditText>(R.id.etQuickAmount)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(getString(R.string.action_save), null)
            .setNegativeButton(getString(R.string.action_cancel), null)
            .create()

        dialog.setOnShowListener {
            et.requestFocus()
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val raw = et.text?.toString()?.trim().orEmpty().replace(',', '.')
                val amount = raw.toDoubleOrNull()
                when {
                    (amount == null || amount.isNaN() || amount.isInfinite()) ->
                        til.error = getString(R.string.manual_entry_error_amount)
                    amount < 0.01 ->
                        til.error = getString(R.string.manual_entry_error_amount_min)
                    amount > 10_000_000.0 ->
                        til.error = getString(R.string.manual_entry_error_amount_max)
                    else -> {
                        vm.quickTransaction(type, amount)
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_person_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.action_edit_name -> { showRenameDialog(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showRenameDialog() {
        val current = vm.person.value ?: return
        val view = layoutInflater.inflate(R.layout.dialog_rename_person, null)
        val til = view.findViewById<TextInputLayout>(R.id.tilRename)
        val et = view.findViewById<TextInputEditText>(R.id.etRename)
        et.setText(current.name)
        et.setSelection(current.name.length)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.action_edit_name))
            .setView(view)
            .setPositiveButton(getString(R.string.action_save), null)
            .setNegativeButton(getString(R.string.action_cancel), null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newName = et.text?.toString()?.trim().orEmpty()
                if (newName.isEmpty()) {
                    til.error = getString(R.string.manual_entry_error_name)
                    return@setOnClickListener
                }
                vm.renamePerson(newName)
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}
