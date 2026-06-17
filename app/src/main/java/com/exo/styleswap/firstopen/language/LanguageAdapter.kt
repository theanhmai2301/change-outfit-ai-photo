package com.exo.styleswap.firstopen.language

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.exo.styleswap.R
import com.exo.styleswap.databinding.ItemLanguageBinding

/**
 * Radio-style single-select list of languages. Tracks the currently selected
 * position and reports clicks to [onClick].
 */
class LanguageAdapter(
    private val items: List<Language>,
    var currentSelect: Int,
    private val onClick: (position: Int) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.VH>() {

    inner class VH(val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val lang = items[position]
        val selected = position == currentSelect
        with(holder.binding) {
            tvFlag.text = lang.flag
            tvName.text = lang.name
            ivCheck.visibility = if (selected) android.view.View.VISIBLE else android.view.View.INVISIBLE
            root.setBackgroundResource(
                if (selected) R.drawable.bg_option_selected else R.drawable.bg_option
            )
            root.setOnClickListener { onClick(holder.bindingAdapterPosition) }
        }
    }

    /** Updates the highlighted row, refreshing only the two affected items. */
    fun select(position: Int) {
        if (position == currentSelect) return
        val prev = currentSelect
        currentSelect = position
        if (prev in items.indices) notifyItemChanged(prev)
        notifyItemChanged(position)
    }
}
