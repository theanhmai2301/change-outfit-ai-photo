package com.exo.styleswap.firstopen.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.exo.styleswap.databinding.FragmentOnboardingContentBinding

/** A single onboarding content slide: icon + title + description. */
class OnboardingContentFragment : Fragment() {

    private var _binding: FragmentOnboardingContentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = requireArguments()
        binding.ivIcon.setImageResource(args.getInt(ARG_ICON))
        binding.tvTitle.setText(args.getInt(ARG_TITLE))
        binding.tvDesc.setText(args.getInt(ARG_DESC))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_DESC = "desc"
        private const val ARG_ICON = "icon"

        fun newInstance(titleRes: Int, descRes: Int, iconRes: Int) =
            OnboardingContentFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TITLE, titleRes)
                    putInt(ARG_DESC, descRes)
                    putInt(ARG_ICON, iconRes)
                }
            }
    }
}
