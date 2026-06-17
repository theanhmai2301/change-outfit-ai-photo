package com.exo.styleswap.firstopen.survey

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.exo.styleswap.databinding.ActivitySurvey3Binding

/** The same survey screen in its "more than one selected" state (state 2). */
class Survey3Activity : BaseSurveyActivity<ActivitySurvey3Binding>() {

    override fun inflateBinding(inflater: LayoutInflater) = ActivitySurvey3Binding.inflate(inflater)
    override fun recyclerView(): RecyclerView = binding.surveyGrid
    override fun nextButton(): View = binding.btnContinue
    override fun nextLabel(): TextView = binding.btnContinueText
    override fun messageView(): TextView = binding.tvMessage
    override fun currentState(): Int = 2
}
