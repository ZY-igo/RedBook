/**
 * 文件说明：MyFragment.kt
 * 作用：承载 My Fragment 相关页面区块的视图渲染、状态呈现与交互逻辑。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.ui.my

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.FragmentMyBinding
import com.zhengyang.redbook.databinding.LayoutMyInterestPersonBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyFragment : Fragment() {

    private var _binding: FragmentMyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSystemBarInsets()
        setupTopBarScrollEffects()

        binding.buttonEditHome.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }
        binding.buttonPreview.setOnClickListener {
            Toast.makeText(requireContext(), R.string.me_toast_preview, Toast.LENGTH_SHORT).show()
        }
        binding.buttonDismissInterest.setOnClickListener {
            viewModel.dismissInterestSection()
        }
        binding.emptyAction.setOnClickListener {
            Toast.makeText(requireContext(), R.string.toast_add_placeholder, Toast.LENGTH_SHORT).show()
        }

        collectUiState()
        setupTab(binding.tabNote, R.string.me_empty_note)
        setupTab(binding.tabCollect, R.string.me_empty_collect)
        setupTab(binding.tabLiked, R.string.me_empty_liked)
        selectTab(binding.tabNote, R.string.me_empty_note)
    }

    private fun setupSystemBarInsets() {
        val baseTopPadding = binding.topActionBar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.topActionBar) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(
                view.paddingLeft,
                baseTopPadding + statusBarInsets.top,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }
        binding.topActionBar.doOnAttach { ViewCompat.requestApplyInsets(it) }
    }

    private fun setupTopBarScrollEffects() {
        val quickFadeDistance = 36.dpToPx()
        val toneShiftDistance = 140.dpToPx()
        val startColor = Color.TRANSPARENT
        val midColor = ContextCompat.getColor(requireContext(), R.color.xhs_profile_scrim_mid)
        val endColor = ContextCompat.getColor(requireContext(), R.color.xhs_profile_scrim_end)

        binding.scrollContainer.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val quickProgress = (scrollY / quickFadeDistance.toFloat()).coerceIn(0f, 1f)
            val toneProgress = (scrollY / toneShiftDistance.toFloat()).coerceIn(0f, 1f)
            val fastAlpha = 1f - (1f - quickProgress) * (1f - quickProgress)
            val backgroundColor = ColorUtils.blendARGB(
                ColorUtils.blendARGB(startColor, midColor, fastAlpha),
                endColor,
                toneProgress
            )

            binding.topActionBarScrim.setBackgroundColor(backgroundColor)
            binding.topActionBarScrim.alpha = fastAlpha
            binding.topBarAvatar.alpha = ((scrollY - 18.dpToPx()) / 42.dpToPx().toFloat())
                .coerceIn(0f, 1f)
            binding.buttonEditHome.alpha = (1f - quickProgress * 1.15f).coerceIn(0f, 1f)
        }
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: MyUiState) {
        binding.profileNameText.text = state.profile.name
        binding.profileUserIdText.text = "小红书号: ${state.profile.id}"
        binding.profileAvatarText.text = state.profile.avatarText
        binding.topBarAvatar.text = state.profile.avatarText
        binding.profileBioText.text = state.profile.bio?.takeIf { it.isNotBlank() }
            ?: getString(R.string.me_no_bio)
        val avatarBackground = createAvatarBackground(state.profile.avatarColorHex)
        binding.profileAvatarText.background = avatarBackground
        binding.topBarAvatar.background = createAvatarBackground(state.profile.avatarColorHex)
        binding.followingCountText.text = state.stats.followingCount
        binding.fansCountText.text = state.stats.fansCount
        binding.likesCountText.text = state.stats.likesCount

        val showInterestSection =
            state.isInterestSectionVisible && state.interestPeople.isNotEmpty()
        binding.interestPeopleSection.isVisible = showInterestSection
        binding.interestPeopleContainer.removeAllViews()

        if (showInterestSection) {
            state.interestPeople.forEach { person ->
                val personBinding = LayoutMyInterestPersonBinding.inflate(
                    layoutInflater,
                    binding.interestPeopleContainer,
                    false
                )
                personBinding.avatarText.text = person.avatarText
                personBinding.avatarText.setBackgroundResource(person.avatarBackgroundRes)
                personBinding.nameText.text = person.name
                personBinding.fansText.text = person.fansText
                personBinding.followButton.setOnClickListener { showFollowToast() }
                binding.interestPeopleContainer.addView(personBinding.root)
            }
        }
    }

    private fun setupTab(tab: TextView, emptyTextRes: Int) {
        tab.setOnClickListener { selectTab(tab, emptyTextRes) }
    }

    private fun selectTab(selectedTab: TextView, emptyTextRes: Int) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.xhs_text_primary)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.xhs_text_secondary)
        listOf(binding.tabNote, binding.tabCollect, binding.tabLiked).forEach { tab ->
            val selected = tab === selectedTab
            tab.setTextColor(if (selected) activeColor else inactiveColor)
            tab.paint.isFakeBoldText = selected
            tab.background = ContextCompat.getDrawable(
                requireContext(),
                if (selected) R.drawable.bg_xhs_tab_indicator else android.R.color.transparent
            )
        }
        binding.emptyTitle.setText(emptyTextRes)
    }

    private fun showFollowToast() {
        Toast.makeText(requireContext(), R.string.message_toast_follow, Toast.LENGTH_SHORT).show()
    }

    private fun createAvatarBackground(colorHex: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(runCatching { Color.parseColor(colorHex) }.getOrDefault(Color.parseColor("#FF8A9F")))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
