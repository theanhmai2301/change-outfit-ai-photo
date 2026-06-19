package com.exo.styleswap.firstopen.survey

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.exo.styleswap.R
import com.exo.styleswap.firstopen.LocaleHelper
import com.exo.styleswap.firstopen.isSurveyDone
import com.exo.styleswap.firstopen.onboarding.OnboardingActivity
import com.exo.styleswap.firstopen.surveyChoice

/**
 * ONE survey screen, realized as three activities that each represent a selection
 * state of that single screen:
 *   state 0 = nothing selected   ([SurveyActivity])
 *   state 1 = exactly one selected ([SurveyDupActivity])
 *   state 2 = more than one selected ([Survey3Activity])
 *
 * All three show the SAME title/subtitle/topics. Crossing a state boundary swaps
 * to the matching activity with NO animation, carrying the current selection — so
 * the user sees a single continuous screen while each swap is a chance to refresh
 * a native ad. Selection only advances the state (forward), never thrashes back.
 */
abstract class BaseSurveyActivity<VB : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: VB
    protected lateinit var adapter: SurveyAdapter

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflateBinding(layoutInflater)
        setContentView(binding.root)

        val initial = intent.getStringArrayListExtra(EXTRA_SELECTED).orEmpty()
        adapter = SurveyAdapter(SURVEY_TOPICS) { count -> onSelectionChanged(count) }
        SURVEY_TOPICS.forEachIndexed { i, t -> if (t.key in initial) adapter.selected.add(i) }

        recyclerView().apply {
            layoutManager = GridLayoutManager(this@BaseSurveyActivity, 2)
            adapter = this@BaseSurveyActivity.adapter
            setHasFixedSize(true)
        }

        nextButton().setOnClickListener {
            if (adapter.selected.size < minSelect()) {
                messageView().visibility = View.VISIBLE
            } else {
                proceed()
            }
        }
        updateNextState(adapter.selected.size)
        loadNativeAd()
    }

    private fun onSelectionChanged(count: Int) {
        messageView().visibility = View.GONE
        val target = when {
            count <= 0 -> 0
            count == 1 -> 1
            else -> 2
        }
        if (target > currentState()) {
            swapToState(target)
        } else {
            updateNextState(count)
        }
    }

    /** Seamlessly hand off to the activity for [target], carrying the selection. */
    private fun swapToState(target: Int) {
        val intent = Intent(this, activityForState(target))
            .putStringArrayListExtra(EXTRA_SELECTED, ArrayList(adapter.selectedKeys()))
        startActivity(intent)
        @Suppress("DEPRECATION") overridePendingTransition(0, 0)
        finish()
        @Suppress("DEPRECATION") overridePendingTransition(0, 0)
    }

    private fun proceed() {
        applicationContext.surveyChoice = "styles=" + adapter.selectedKeys().joinToString(",")
        applicationContext.isSurveyDone = true
        OnboardingActivity.start(this)
        finish()
    }

    private fun activityForState(state: Int): Class<*> = when (state) {
        0 -> SurveyActivity::class.java
        1 -> SurveyDupActivity::class.java
        else -> Survey3Activity::class.java
    }

    private fun updateNextState(count: Int) {
        val enabled = count >= minSelect()
        nextButton().setBackgroundResource(
            if (enabled) R.drawable.bg_gradient_primary else R.drawable.bg_button_disabled
        )
        nextLabel().setTextColor(
            ContextCompat.getColor(this, if (enabled) R.color.white else R.color.disabled_foreground)
        )
    }

    private fun loadNativeAd() {
        // TODO: ADS — refresh a native ad for this state in the frAds slot.
    }

    protected abstract fun inflateBinding(inflater: LayoutInflater): VB
    protected abstract fun recyclerView(): RecyclerView
    protected abstract fun nextButton(): View
    protected abstract fun nextLabel(): TextView
    protected abstract fun messageView(): TextView

    /** 0 = nothing selected, 1 = exactly one, 2 = more than one. */
    protected abstract fun currentState(): Int

    /** Must select at least this many to continue. */
    protected open fun minSelect(): Int = 2

    companion object {
        const val EXTRA_SELECTED = "selected_keys"

        /** The single shared topic set shown in every state of the survey screen. */
        val SURVEY_TOPICS: List<Survey> = listOf(
            Survey("casual", R.string.survey_casual, "👕"),
            Survey("office", R.string.survey_office, "👔"),
            Survey("party", R.string.survey_party, "🎉"),
            Survey("sport", R.string.survey_sport, "🏃"),
            Survey("streetwear", R.string.survey_streetwear, "🧢"),
            Survey("elegant", R.string.survey_elegant, "👗")
        )
    }
}
