package com.exo.styleswap.firstopen.survey

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.exo.styleswap.databinding.ActivitySurveyBinding

/** The survey screen in its "nothing selected" state (state 0). */
class SurveyActivity : BaseSurveyActivity<ActivitySurveyBinding>() {

    override fun inflateBinding(inflater: LayoutInflater) = ActivitySurveyBinding.inflate(inflater)
    override fun recyclerView(): RecyclerView = binding.surveyGrid
    override fun nextButton(): View = binding.btnContinue
    override fun nextLabel(): TextView = binding.btnContinueText
    override fun messageView(): TextView = binding.tvMessage
    override fun currentState(): Int = 0

    companion object {
        /** Entry point into the survey screen (fresh, nothing selected). */
        fun start(context: Context) {
            context.startActivity(Intent(context, SurveyActivity::class.java))
        }

        @Suppress("UNUSED_PARAMETER")
        fun preloadAds(activity: Activity): Boolean = false // TODO: ADS
    }
}
