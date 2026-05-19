package com.tlib.inappupdate.ui

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
import com.tlib.inappupdate.databinding.IauFragmentUpdateDialogBinding

class UpdateDialogFragment : DialogFragment() {

    var onUpdateClicked: (() -> Unit)? = null
    var onDismissed: (() -> Unit)? = null

    private var _binding: IauFragmentUpdateDialogBinding? = null
    private val binding get() = _binding!!
    private var availableVersion = ""
    private val ARG_AVAILABLE = "arg_available"

    companion object {

        const val TAG = "UpdateDialog"
        fun newInstance(
            availableVersion: String
        ): UpdateDialogFragment {
            return UpdateDialogFragment().apply {
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

        _binding = IauFragmentUpdateDialogBinding.inflate(inflater, container, false)

        binding.availableVersionTxt.text = "v$availableVersion"

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        isCancelable = false

        binding.tvTitle.text =
            HtmlCompat.fromHtml(
                getString(R.string.iau_99),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

        val text = "👉 Tap Update and enjoy the latest version today!"

        val spannable = SpannableString(text)

        spannable.setSpan(
            AbsoluteSizeSpan(15, true),
            2,
            12,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable.setSpan(
            AbsoluteSizeSpan(13, true),
            12,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvHeader.text = spannable

        binding.updateBtn.setOnClickListener {
            onUpdateClicked?.invoke()
            dismiss()
        }

        binding.btnLater.setOnClickListener {
            onDismissed?.invoke()
            dismiss()
        }
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