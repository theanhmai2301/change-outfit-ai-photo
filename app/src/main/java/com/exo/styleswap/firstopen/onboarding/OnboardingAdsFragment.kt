package com.exo.styleswap.firstopen.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.exo.styleswap.databinding.FragmentOnboardingAdsBinding

/**
 * Full-screen native-ad slot page. Until a real ad SDK is wired in, it shows a
 * neutral branded placeholder (so the page is never blank) inside [binding].frAds.
 */
class OnboardingAdsFragment : Fragment() {

    private var _binding: FragmentOnboardingAdsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingAdsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadNativeAd()
    }

    private fun loadNativeAd() {
        // TODO: ADS — inflate a full-screen native ad into binding.frAds.
        // When the ad is ready, hide binding.placeholder and show the ad view.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
