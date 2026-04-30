package com.borc.takip.ui.main

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.borc.takip.R
import com.borc.takip.data.model.Person
import com.borc.takip.databinding.ItemPersonBinding
import com.borc.takip.voice.VoiceCommandProcessor
import kotlin.math.abs

class PersonAdapter(
    private val onClick: (Person) -> Unit,
    private val onLongClick: (Person) -> Boolean
) : ListAdapter<Person, PersonAdapter.VH>(Diff()) {

    companion object {
        private val AVATAR_COLORS = intArrayOf(
            R.color.avatar_1, R.color.avatar_2, R.color.avatar_3,
            R.color.avatar_4, R.color.avatar_5, R.color.avatar_6
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemPersonBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(p: Person) {
            val ctx = b.root.context
            b.tvPersonName.text = p.name
            b.tvInitial.text = p.name.firstOrNull()?.uppercase() ?: "?"

            // İsim hash'ine göre avatar rengi seç
            val colorRes = AVATAR_COLORS[abs(p.name.hashCode()) % AVATAR_COLORS.size]
            b.tvInitial.backgroundTintList = ColorStateList.valueOf(ctx.getColor(colorRes))
            b.tvBalance.text = VoiceCommandProcessor.formatAmount(abs(p.totalBalance))

            when {
                p.totalBalance > 0 -> {
                    b.tvBalanceLabel.text = ctx.getString(R.string.label_debt)
                    b.tvBalance.setTextColor(ctx.getColor(R.color.color_debt))
                    b.tvBalanceLabel.setTextColor(ctx.getColor(R.color.color_debt))
                    b.cardPerson.setCardBackgroundColor(ctx.getColor(R.color.color_debt_bg))
                }
                p.totalBalance < 0 -> {
                    b.tvBalanceLabel.text = ctx.getString(R.string.label_credit)
                    b.tvBalance.setTextColor(ctx.getColor(R.color.color_credit))
                    b.tvBalanceLabel.setTextColor(ctx.getColor(R.color.color_credit))
                    b.cardPerson.setCardBackgroundColor(ctx.getColor(R.color.color_credit_bg))
                }
                else -> {
                    b.tvBalanceLabel.text = ctx.getString(R.string.label_settled)
                    b.tvBalance.setTextColor(ctx.getColor(R.color.color_neutral))
                    b.tvBalanceLabel.setTextColor(ctx.getColor(R.color.color_neutral))
                    b.cardPerson.setCardBackgroundColor(ctx.getColor(R.color.color_neutral_bg))
                }
            }

            b.root.setOnClickListener { onClick(p) }
            b.root.setOnLongClickListener { onLongClick(p) }
        }
    }

    class Diff : DiffUtil.ItemCallback<Person>() {
        override fun areItemsTheSame(o: Person, n: Person) = o.id == n.id
        override fun areContentsTheSame(o: Person, n: Person) = o == n
    }
}
