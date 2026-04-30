package com.borc.takip.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.borc.takip.R
import com.borc.takip.data.model.Transaction
import com.borc.takip.data.model.TransactionType
import com.borc.takip.databinding.ItemTransactionBinding
import com.borc.takip.voice.VoiceCommandProcessor
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onDelete: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.VH>(Diff()) {

    private val dateFmt = SimpleDateFormat("dd MMM yyyy  HH:mm", Locale("tr", "TR"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemTransactionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(tx: Transaction) {
            val ctx = b.root.context
            b.tvDate.text = dateFmt.format(Date(tx.createdAt))

            val formatted = VoiceCommandProcessor.formatAmount(tx.amount)
            if (tx.type == TransactionType.CREDIT) {
                b.tvAmount.text = "+$formatted"
                b.tvAmount.setTextColor(ctx.getColor(R.color.color_debt))
                b.tvTypeLabel.text = ctx.getString(R.string.tx_credit_label)
                b.ivTypeIcon.setImageResource(R.drawable.ic_add_circle)
                b.ivTypeIcon.setColorFilter(ctx.getColor(R.color.color_debt))
            } else {
                b.tvAmount.text = "-$formatted"
                b.tvAmount.setTextColor(ctx.getColor(R.color.color_credit))
                b.tvTypeLabel.text = ctx.getString(R.string.tx_debit_label)
                b.ivTypeIcon.setImageResource(R.drawable.ic_remove_circle)
                b.ivTypeIcon.setColorFilter(ctx.getColor(R.color.color_credit))
            }

            if (tx.note.isNotBlank()) {
                b.tvNote.text = tx.note
                b.tvNote.visibility = View.VISIBLE
            } else {
                b.tvNote.visibility = View.GONE
            }

            if (tx.voiceText.isNotBlank()) {
                b.tvVoiceText.text = "\"${tx.voiceText}\""
                b.tvVoiceText.visibility = View.VISIBLE
            } else {
                b.tvVoiceText.visibility = View.GONE
            }

            b.btnDelete.setOnClickListener { onDelete(tx) }
        }
    }

    class Diff : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(o: Transaction, n: Transaction) = o.id == n.id
        override fun areContentsTheSame(o: Transaction, n: Transaction) = o == n
    }
}
