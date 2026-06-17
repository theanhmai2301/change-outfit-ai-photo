package com.exo.styleswap.firstopen.survey

import androidx.annotation.StringRes

/**
 * One selectable survey topic.
 *
 * @param key      stable key saved for personalization (locale-independent)
 * @param labelRes translated label
 * @param emoji    icon shown on the card
 */
data class Survey(val key: String, @StringRes val labelRes: Int, val emoji: String)
