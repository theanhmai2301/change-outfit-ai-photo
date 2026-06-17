package com.exo.styleswap.firstopen.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/** A page in the onboarding pager: either a content slide or a native-ad slot. */
sealed class OnboardingPage {
    data class Content(
        @StringRes val titleRes: Int,
        @StringRes val descRes: Int,
        @DrawableRes val iconRes: Int
    ) : OnboardingPage()

    object Ads : OnboardingPage()
}

/**
 * Builds onboarding pages, interleaving content slides with full-screen native-ad
 * slots. Each page is a [Fragment]; the ad fragment loads its ad when shown.
 */
class OnboardingPagerAdapter(
    activity: FragmentActivity,
    private val pages: List<OnboardingPage>
) : FragmentStateAdapter(activity) {

    override fun getItemCount() = pages.size

    override fun createFragment(position: Int): Fragment = when (val page = pages[position]) {
        is OnboardingPage.Content ->
            OnboardingContentFragment.newInstance(page.titleRes, page.descRes, page.iconRes)
        OnboardingPage.Ads -> OnboardingAdsFragment()
    }

    fun isAdPage(position: Int) = pages.getOrNull(position) is OnboardingPage.Ads
}
