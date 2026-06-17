package com.exo.styleswap.firstopen.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.exo.styleswap.R
import com.exo.styleswap.databinding.ActivityOnboardingBinding
import com.exo.styleswap.firstopen.LocaleHelper
import com.exo.styleswap.firstopen.PermissionActivity
import com.exo.styleswap.firstopen.isOnboardingDone
import com.exo.styleswap.firstopen.isPermissionDone
import com.exo.styleswap.ui.main.MainActivity

/**
 * Onboarding host. A [ViewPager2] of content slides interleaved with native-ad
 * pages, a dots indicator, and a shared bottom button. Finishing the last page
 * marks onboarding done and routes to Permission (or Main).
 */
class OnboardingActivity : AppCompatActivity(), OnActionNextOnboarding {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var pagerAdapter: OnboardingPagerAdapter
    private val dots = mutableListOf<View>()

    private val pages = listOf(
        OnboardingPage.Content(R.string.ob1_title, R.string.ob1_desc, R.drawable.ic_checkroom),
        OnboardingPage.Content(R.string.ob2_title, R.string.ob2_desc, R.drawable.ic_sparkles),
        OnboardingPage.Ads,
        OnboardingPage.Content(R.string.ob3_title, R.string.ob3_desc, R.drawable.ic_share)
    )

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pagerAdapter = OnboardingPagerAdapter(this, pages)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = pages.size

        buildDots()
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                updateButton(position)
            }
        })

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current >= pages.lastIndex) onComplete() else onNext()
        }

        updateDots(0)
        updateButton(0)
    }

    // region OnActionNextOnboarding
    override fun onNext() {
        val next = (binding.viewPager.currentItem + 1).coerceAtMost(pages.lastIndex)
        binding.viewPager.setCurrentItem(next, true)
    }

    override fun onComplete() {
        applicationContext.isOnboardingDone = true
        val ctx = applicationContext
        val next = if (!ctx.isPermissionDone &&
            !PermissionActivity.isNotificationPermissionGranted(this)
        ) {
            Intent(this, PermissionActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        startActivity(next)
        finish()
    }
    // endregion

    private fun buildDots() {
        binding.dots.removeAllViews()
        dots.clear()
        repeat(pages.size) {
            val dot = View(this)
            val lp = LinearLayout.LayoutParams(dp(7), dp(7))
            lp.setMargins(dp(4), 0, dp(4), 0)
            dot.layoutParams = lp
            binding.dots.addView(dot)
            dots.add(dot)
        }
    }

    private fun updateDots(active: Int) {
        dots.forEachIndexed { i, dot ->
            val lp = dot.layoutParams as LinearLayout.LayoutParams
            lp.width = if (i == active) dp(22) else dp(7)
            dot.layoutParams = lp
            dot.background = GradientDrawable().apply {
                cornerRadius = dp(4).toFloat()
                setColor(
                    if (i == active) ContextCompat.getColor(this@OnboardingActivity, R.color.primary)
                    else Color.argb(0x40, 0x88, 0x88, 0xAA)
                )
            }
        }
    }

    private fun updateButton(position: Int) {
        binding.btnNextText.setText(
            if (position >= pages.lastIndex) R.string.ob_start else R.string.ob_next
        )
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, OnboardingActivity::class.java))
        }

        @Suppress("UNUSED_PARAMETER")
        fun preloadAds(activity: Activity): Boolean = false // TODO: ADS
    }
}
