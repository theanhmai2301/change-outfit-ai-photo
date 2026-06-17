package com.exo.styleswap.firstopen.survey

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.exo.styleswap.databinding.ActivitySurveyDupBinding

/** The same survey screen in its "exactly one selected" state (state 1). */
class SurveyDupActivity : BaseSurveyActivity<ActivitySurveyDupBinding>() {

    override fun inflateBinding(inflater: LayoutInflater) = ActivitySurveyDupBinding.inflate(inflater)
    override fun recyclerView(): RecyclerView = binding.surveyGrid
    override fun nextButton(): View = binding.btnContinue
    override fun nextLabel(): TextView = binding.btnContinueText
    override fun messageView(): TextView = binding.tvMessage
    override fun currentState(): Int = 1
}
