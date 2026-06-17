package com.exo.styleswap.firstopen.onboarding

/**
 * Lets a page fragment drive the pager. The host ([OnboardingActivity]) implements
 * it; the bottom button and any in-page CTA route through the same two actions.
 */
interface OnActionNextOnboarding {
    /** Advance to the next page. */
    fun onNext()

    /** Last page reached — finish onboarding and move on. */
    fun onComplete()
}
