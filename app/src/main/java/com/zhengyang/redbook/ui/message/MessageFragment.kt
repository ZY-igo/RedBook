/**
 * 文件说明：MessageFragment.kt
 * 作用：承载 Message Fragment 相关页面区块的视图渲染、状态呈现与交互逻辑。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.ui.message

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zhengyang.redbook.databinding.FragmentMessageBinding
import com.zhengyang.redbook.databinding.LayoutMessagePersonRowBinding
import com.zhengyang.redbook.databinding.LayoutMessageRowBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MessageFragment : Fragment() {

    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MessageViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applySystemBarInsets()
        bindActions()
        setupRefresh()
        collectUiState()
    }

    private fun bindActions() {
        binding.peopleLayout.closePeopleButton.setOnClickListener {
            viewModel.dismissPeopleSuggestions()
        }
    }

    private fun setupRefresh() {
        binding.messageRefreshLayout.setOnRefreshListener {
            viewModel.reload()
            binding.messageRefreshLayout.postDelayed(720L) {
                _binding?.messageRefreshLayout?.setRefreshing(false)
            }
        }
    }

    private fun applySystemBarInsets() {
        val baseTopPadding = binding.topBar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { topBar, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            topBar.setPadding(
                topBar.paddingLeft,
                baseTopPadding + statusBarInsets.top,
                topBar.paddingRight,
                topBar.paddingBottom
            )
            insets
        }
        binding.topBar.doOnAttach { ViewCompat.requestApplyInsets(it) }
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: MessageUiState) {
        if (_binding == null) return
        renderMessageRows(state.messageRows)
        renderPeopleSuggestions(state.peopleSuggestions)
    }

    private fun renderMessageRows(rows: List<MessageRowItem>) {
        val binding = _binding ?: return
        binding.messageRowsLayout.messageRowsContainer.removeAllViews()
        rows.forEach { row ->
            val rowBinding = LayoutMessageRowBinding.inflate(
                layoutInflater,
                binding.messageRowsLayout.messageRowsContainer,
                false
            )
            bindMessageRow(rowBinding, row)
            binding.messageRowsLayout.messageRowsContainer.addView(rowBinding.root)
        }
    }

    private fun bindMessageRow(
        rowBinding: LayoutMessageRowBinding,
        row: MessageRowItem
    ) {
        rowBinding.iconAvatarContainer.setBackgroundResource(row.backgroundRes)
        rowBinding.titleText.text = row.title
        rowBinding.subtitleText.text = row.subtitle
        rowBinding.timeText.text = row.timeText
        rowBinding.verifiedBadge.visibility =
            if (row.showsVerifiedBadge) View.VISIBLE else View.GONE
        rowBinding.redDot.visibility = if (row.showsRedDot) View.VISIBLE else View.GONE

        if (row.avatarText != null) {
            rowBinding.textAvatar.visibility = View.VISIBLE
            rowBinding.textAvatar.text = row.avatarText
            rowBinding.iconAvatarImage.visibility = View.GONE
        } else {
            rowBinding.textAvatar.visibility = View.GONE
            rowBinding.iconAvatarImage.visibility = View.VISIBLE
            rowBinding.iconAvatarImage.setImageResource(checkNotNull(row.iconRes))
            rowBinding.iconAvatarImage.imageTintList =
                ColorStateList.valueOf(requireContext().getColor(com.zhengyang.redbook.R.color.xhs_drawer_text_primary))
            rowBinding.iconAvatarImage.layoutParams = rowBinding.iconAvatarImage.layoutParams.apply {
                width = row.iconSizeDp.dpToPx()
                height = row.iconSizeDp.dpToPx()
            }
        }
    }

    private fun renderPeopleSuggestions(peopleSuggestions: List<PersonSuggestionItem>) {
        val binding = _binding ?: return
        binding.peopleLayout.peopleSection.visibility =
            if (peopleSuggestions.isEmpty()) View.GONE else View.VISIBLE
        binding.peopleLayout.peopleContainer.removeAllViews()

        peopleSuggestions.forEach { person ->
            val rowBinding = LayoutMessagePersonRowBinding.inflate(
                layoutInflater,
                binding.peopleLayout.peopleContainer,
                false
            )
            bindPersonSuggestion(rowBinding, person)
            binding.peopleLayout.peopleContainer.addView(rowBinding.root)
        }
    }

    private fun bindPersonSuggestion(
        rowBinding: LayoutMessagePersonRowBinding,
        person: PersonSuggestionItem
    ) {
        rowBinding.avatarText.text = person.avatarText
        rowBinding.avatarText.setBackgroundResource(person.avatarBackgroundRes)
        rowBinding.nameText.text = person.name
        rowBinding.subtitleText.text = person.subtitle
        rowBinding.followButton.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            Toast.makeText(requireContext(), com.zhengyang.redbook.R.string.message_toast_follow, Toast.LENGTH_SHORT)
                .show()
        }
        rowBinding.dismissButton.setOnClickListener {
            viewModel.dismissPerson(person.id)
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
