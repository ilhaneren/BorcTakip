package com.borc.takip.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.borc.takip.R
import com.borc.takip.data.model.TransactionType
import com.borc.takip.databinding.BottomSheetManualEntryBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class ManualEntryBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetManualEntryBinding? = null
    private val b get() = _binding!!

    private val vm: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetManualEntryBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onStart() {
        super.onStart()
        // Klavye açıldığında içeriği yukarı kaydır
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        // Sheet'i tam yükseklikte aç; klavye için kayacak boşluk oluşsun
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToggleColors()

        viewLifecycleOwner.lifecycleScope.launch {
            val names = vm.getPersonNames()
            if (names.isNotEmpty()) {
                b.etName.setAdapter(
                    ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                )
            }
        }

        b.btnSave.setOnClickListener { submit() }
    }

    private fun setupToggleColors() {
        val creditColor = requireContext().getColor(R.color.color_debt)
        val debitColor  = requireContext().getColor(R.color.color_credit)
        val white       = requireContext().getColor(R.color.white)
        val surface     = requireContext().getColor(android.R.color.transparent)

        fun applyColors(creditSelected: Boolean) {
            if (creditSelected) {
                b.btnCredit.setBackgroundColor(creditColor)
                b.btnCredit.setTextColor(white)
                b.btnCredit.iconTint = android.content.res.ColorStateList.valueOf(white)
                b.btnDebit.setBackgroundColor(surface)
                b.btnDebit.setTextColor(debitColor)
                b.btnDebit.iconTint = android.content.res.ColorStateList.valueOf(debitColor)
                b.btnSave.setBackgroundColor(creditColor)
            } else {
                b.btnDebit.setBackgroundColor(debitColor)
                b.btnDebit.setTextColor(white)
                b.btnDebit.iconTint = android.content.res.ColorStateList.valueOf(white)
                b.btnCredit.setBackgroundColor(surface)
                b.btnCredit.setTextColor(creditColor)
                b.btnCredit.iconTint = android.content.res.ColorStateList.valueOf(creditColor)
                b.btnSave.setBackgroundColor(debitColor)
            }
        }

        // Başlangıç: borç ekle seçili
        applyColors(creditSelected = true)
        b.toggleType.check(R.id.btnCredit)

        b.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) applyColors(creditSelected = checkedId == R.id.btnCredit)
        }
    }

    private fun submit() {
        val name = b.etName.text?.toString()?.trim().orEmpty()
        val amountStr = b.etAmount.text?.toString()?.trim().orEmpty().replace(',', '.')

        b.tilName.error = null
        b.tilAmount.error = null

        if (name.isEmpty()) {
            b.tilName.error = getString(R.string.manual_entry_error_name)
            return
        }
        if (name.length > 100) {
            b.tilName.error = getString(R.string.manual_entry_error_name_long)
            return
        }
        if (!NAME_REGEX.matches(name)) {
            b.tilName.error = getString(R.string.manual_entry_error_name_invalid)
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount.isNaN() || amount.isInfinite()) {
            b.tilAmount.error = getString(R.string.manual_entry_error_amount)
            return
        }
        if (amount < MIN_AMOUNT) {
            b.tilAmount.error = getString(R.string.manual_entry_error_amount_min)
            return
        }
        if (amount > MAX_AMOUNT) {
            b.tilAmount.error = getString(R.string.manual_entry_error_amount_max)
            return
        }

        val type = if (b.toggleType.checkedButtonId == R.id.btnCredit)
            TransactionType.CREDIT else TransactionType.DEBIT

        vm.submitManualEntry(name, type, amount, null)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ManualEntryBottomSheet"
        private const val MAX_AMOUNT = 10_000_000.0
        private const val MIN_AMOUNT = 0.01
        private val NAME_REGEX = Regex("^[\\p{L}\\p{N}\\s.\\-']+\$")
    }
}
