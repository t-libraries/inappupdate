package com.tlib.inappupdate.ui

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.tlib.inappupdate.R
import com.tlib.inappupdate.databinding.IauFragmentInstalledDialogBinding

class InstalledDialogFragment : DialogFragment() {

    private var _binding: IauFragmentInstalledDialogBinding? = null
    var onUpdateClicked: (() -> Unit)? = null
    var onDismissed: (() -> Unit)? = null

    private val binding get() = _binding!!

    private var availableVersion = ""
    private val ARG_AVAILABLE = "arg_available"

    companion object {
        const val TAG = "InstalledDialogFragment"

        fun newInstance(
            availableVersion: String
        ): InstalledDialogFragment {
            return InstalledDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_AVAILABLE, availableVersion)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        availableVersion =
            arguments?.getString(ARG_AVAILABLE).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = IauFragmentInstalledDialogBinding.inflate(inflater, container, false)

        binding.availableVersionTxt.text = "v$availableVersion"

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        isCancelable = false
        setupUI()
        setupButtons()
    }

    private fun setupUI() {
        val text = HtmlCompat.fromHtml(
            getString(R.string.iau_latest_version),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        val spannable = SpannableString(text)

        spannable.setSpan(
            AbsoluteSizeSpan(12, true),
            2,
            5,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable.setSpan(
            AbsoluteSizeSpan(15, true),
            5,
            18,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable.setSpan(
            AbsoluteSizeSpan(12, true),
            19,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvTitle.text = spannable
    }

    private fun setupButtons() {
        binding.restartNowBtn.setOnClickListener {

            onUpdateClicked?.invoke()
            dismiss()

        }

        binding.restartLaterBtn?.setOnClickListener {
            onDismissed?.invoke()
            dismiss()
        }
    }

    private fun restartApp() {
        val intent = requireActivity().packageManager.getLaunchIntentForPackage(requireActivity().packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        requireActivity().finishAffinity()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.BOTTOM)
            attributes.windowAnimations = R.style.iau_BottomDialogAnimation
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}