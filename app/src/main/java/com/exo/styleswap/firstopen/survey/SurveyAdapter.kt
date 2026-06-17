package com.exo.styleswap.firstopen.survey

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.exo.styleswap.R
import com.exo.styleswap.databinding.ItemSurveyBinding

/**
 * Multi-select grid of survey topics. Toggles selection on tap and reports the
 * selected count to [onSelectionChanged] so the host can enable/disable Next.
 */
class SurveyAdapter(
    private val items: List<Survey>,
    private val onSelectionChanged: (count: Int) -> Unit
) : RecyclerView.Adapter<SurveyAdapter.VH>() {

    val selected = linkedSetOf<Int>()

    inner class VH(val binding: ItemSurveyBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSurveyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val isSelected = position in selected
        val ctx = holder.itemView.context
        with(holder.binding) {
            tvEmoji.text = item.emoji
            tvLabel.setText(item.labelRes)
            tvLabel.setTextColor(
                ContextCompat.getColor(ctx, if (isSelected) R.color.primary else R.color.foreground)
            )
            ivCheck.visibility = if (isSelected) View.VISIBLE else View.GONE
            root.setBackgroundResource(
                if (isSelected) R.drawable.bg_option_selected else R.drawable.bg_option
            )
            root.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                if (!selected.add(pos)) selected.remove(pos)
                notifyItemChanged(pos)
                onSelectionChanged(selected.size)
            }
        }
    }

    /** Keys of the currently selected topics, in selection order. */
    fun selectedKeys(): List<String> = selected.map { items[it].key }
}
