/**
 * 文件说明： NoteDetailActivity.kt
 * 作用： 承载笔记详情展示、播放控制和相关界面行为。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.note

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.zhengyang.redbook.R
import com.zhengyang.redbook.data.remote.RemoteResourceMapper
import com.zhengyang.redbook.data.remote.model.RemoteCommentDto
import com.zhengyang.redbook.data.remote.model.RemoteNoteDetailDto
import com.zhengyang.redbook.data.remote.model.RemoteReplyDto
import com.zhengyang.redbook.data.repository.NoteRepository
import com.zhengyang.redbook.databinding.ActivityNoteDetailImageBinding
import com.zhengyang.redbook.databinding.ActivityNoteDetailVideoBinding
import com.zhengyang.redbook.databinding.ItemNoteCommentBinding
import com.zhengyang.redbook.databinding.ItemNoteCommentReplyBinding
import com.zhengyang.redbook.databinding.ItemNoteDetailImageBinding
import com.zhengyang.redbook.media.MediaItemFactory
import com.zhengyang.redbook.media.MediaPlayerFactory
import com.zhengyang.redbook.service.foreground.NotificationService
import com.zhengyang.redbook.ui.home.HomeCardItem
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NoteDetailActivity : AppCompatActivity() {

    private var imageBinding: ActivityNoteDetailImageBinding? = null
    private var videoBinding: ActivityNoteDetailVideoBinding? = null
    private var playerListener: Player.Listener? = null
    private var cachedPlayer: ExoPlayer? = null
    private var progressUpdater: Runnable? = null
    private var gestureHideRunnable: Runnable? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isFullscreen = false
    private var isSeeking = false
    private var pendingSeekPositionMs = C.TIME_UNSET
    private var lastVideoWidth = 16
    private var lastVideoHeight = 9
    private var hasShownMeteredNetworkHint = false
    private var hasRenderedFirstFrame = false
    private var pendingNetworkRecovery = false
    private var shouldResumeAfterNetworkRecovery = false
    private var gestureSeekBasePositionMs = 0L
    private var gestureVolumeBase = 0
    private var gestureBrightnessBase = DEFAULT_GESTURE_BRIGHTNESS

    private val imagePagerAdapter = NoteImagePagerAdapter()
    private val commentAdapter = NoteCommentAdapter(
        onLikeClick = ::toggleCommentLike,
        onReplyLikeClick = ::toggleReplyLike,
        onReplyClick = { comment, replyTo -> showCommentDialog(parentComment = comment, replyToAuthor = replyTo) },
        onReplyToggleClick = ::toggleCommentReplies
    )
    private val imagePagerSnapHelper = LinearSnapHelper()
    private val playbackSpeeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
    private val commentItems = mutableListOf<NoteCommentUiModel>()
    private var noteLikeCountValue = 0
    private var noteCollectCountValue = 0
    private var isFollowingAuthor = false
    private var isNoteLiked = false
    private var isNoteCollected = false
    private var commentSortMode = CommentSortMode.DEFAULT
    private var activeCommentDialog: AlertDialog? = null
    private var activeCommentDialogState: CommentDialogState? = null
    private var currentNoteId: String? = null
    private var currentAuthorId: String? = null
    private var currentAuthorName: String = ""
    private var currentNoteTitle: String = ""
    private var currentVideoUrl: String = ""
    private var currentCoverUrl: String = ""
    private var currentImageUrls: List<String> = emptyList()
    private val commentImagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            handlePickedCommentImage(uri)
        }

    @Inject
    lateinit var noteRepository: NoteRepository

    @Inject
    lateinit var remoteResourceMapper: RemoteResourceMapper

    private val isVideo: Boolean by lazy {
        intent.getStringExtra(EXTRA_MEDIA_TYPE) == HomeCardItem.MediaType.VIDEO.name
    }

    private val playbackPrefs by lazy {
        getSharedPreferences(PREFS_PLAYBACK, Context.MODE_PRIVATE)
    }

    private val audioManager by lazy {
        getSystemService(AudioManager::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        isFullscreen = savedInstanceState?.getBoolean(STATE_FULLSCREEN)
            ?: resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        currentNoteId = intent.getStringExtra(EXTRA_NOTE_ID)
        currentAuthorName = intent.getStringExtra(EXTRA_AUTHOR).orEmpty()
        currentNoteTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        currentVideoUrl = intent.getStringExtra(EXTRA_VIDEO_URL).orEmpty()
        currentCoverUrl = intent.getStringExtra(EXTRA_COVER_URL).orEmpty()
        currentImageUrls = collectImageUrls(fallbackUrl = currentCoverUrl)

        if (isVideo) {
            setupVideoLayout()
        } else {
            setupImageLayout()
        }
        loadRemoteNote()
    }

    override fun onStart() {
        super.onStart()
        if (isVideo) {
            registerNetworkCallbackIfNeeded()
            setupPlayerIfNeeded()
        }
    }

    override fun onStop() {
        if (isVideo) {
            stopProgressUpdates()
            gestureHideRunnable?.let { runnable ->
                videoBinding?.gestureHintText?.removeCallbacks(runnable)
            }
            gestureHideRunnable = null
            cachedPlayer?.let { player ->
                savePlaybackProgress(
                    videoUrl = currentVideoUrl,
                    positionMs = player.currentPosition,
                    durationMs = player.duration
                )
                shouldResumeAfterNetworkRecovery = false
            }
            videoBinding?.detailVideoView?.player = null
            unregisterNetworkCallback()
        }
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_FULLSCREEN, isFullscreen)
    }

    override fun onBackPressed() {
        if (isFullscreen) {
            if (isVideo) {
                setFullscreen(false)
            } else {
                setImageFullscreen(false)
            }
            return
        }
        super.onBackPressed()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isVideo) return

        videoBinding?.root?.post {
            applyVideoModeUi()
            configureVideoLayout(lastVideoWidth, lastVideoHeight)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = super.dispatchKeyEvent(event)
        if (
            isVideo &&
            event.action == KeyEvent.ACTION_UP &&
            event.keyCode in setOf(
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_MUTE
            )
        ) {
            syncPlayerVolumeWithSystem()
        }
        return handled
    }

    override fun onDestroy() {
        if (isVideo) {
            unregisterNetworkCallback()
            releasePlayerIfNeeded()
        }
        imageBinding = null
        videoBinding = null
        super.onDestroy()
    }

    private fun setupImageLayout() {
        imageBinding = ActivityNoteDetailImageBinding.inflate(layoutInflater)
        setContentView(requireImageBinding().root)
        applyInsets(
            requireImageBinding().topBar,
            ContextCompat.getColor(this, R.color.xhs_card),
            useLightSystemBars = true
        )
        bindImageHeader()
        bindImageContent()
    }

    private fun setupVideoLayout() {
        videoBinding = ActivityNoteDetailVideoBinding.inflate(layoutInflater)
        setContentView(requireVideoBinding().root)
        applyInsets(requireVideoBinding().topBar, Color.BLACK, useLightSystemBars = false)
        bindVideoHeader()
        bindVideoContent()
        bindVideoEvents()
        bindVideoActions()
        applyVideoModeUi()
        updateSpeedButton()
        requireVideoBinding().root.post { refreshVideoActionLabels() }
    }

    private fun applyInsets(topBar: View, backgroundColor: Int, useLightSystemBars: Boolean) {
        val initialTopPadding = topBar.paddingTop
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = useLightSystemBars
            isAppearanceLightNavigationBars = useLightSystemBars
        }
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { insetTarget, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            insetTarget.setPadding(
                insetTarget.paddingLeft,
                initialTopPadding + statusBar.top,
                insetTarget.paddingRight,
                insetTarget.paddingBottom
            )
            insets
        }
    }

    private fun bindImageHeader() {
        val binding = requireImageBinding()
        binding.buttonBack.setOnClickListener {
            if (isFullscreen) setImageFullscreen(false) else finish()
        }
        binding.buttonShare.setOnClickListener {
            Toast.makeText(this, "分享面板待接入，先保留交互入口", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindVideoHeader() {
        val binding = requireVideoBinding()
        binding.buttonBack.setOnClickListener {
            if (isFullscreen) setFullscreen(false) else finish()
        }
        binding.buttonVideoAux.setOnClickListener { }
        binding.buttonAction.setOnClickListener { }
        binding.buttonShare.setOnClickListener { }
    }

    private fun bindImageContent() {
        val binding = requireImageBinding()
        val author = intent.getStringExtra(EXTRA_AUTHOR).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
        val coverUrl = currentCoverUrl

        noteLikeCountValue = intent.getStringExtra(EXTRA_LIKE_COUNT)?.toIntOrNull() ?: 0
        noteCollectCountValue = derivedCollectCount().toIntOrNull() ?: 0
        binding.topAuthorName.text = author
        binding.topAvatarText.text = author.take(1)
        binding.noteTitle.text = title
        binding.noteDescription.text = description
        binding.relatedText.text = title
        binding.publishTimeText.text = derivedPublishTime()
        binding.locationText.text = derivedLocation(author)
        binding.commentInput.text = getString(R.string.note_detail_comment_hint)
        bindImagePager(currentImageUrls.ifEmpty { collectImageUrls(fallbackUrl = coverUrl) })
        bindImageActions(author = author, title = title)
        seedComments(author = author, title = title)
        refreshImageActionState()
        refreshCommentSection()
    }

    private fun bindVideoContent() {
        val binding = requireVideoBinding()
        val author = intent.getStringExtra(EXTRA_AUTHOR).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
        val likeCount = intent.getStringExtra(EXTRA_LIKE_COUNT).orEmpty()
        val coverUrl = currentCoverUrl

        binding.authorName.text = author
        binding.avatarText.text = author.take(1)
        binding.noteTitle.text = title
        binding.noteDescription.text = description
        binding.likeCount.text = likeCount
        binding.collectCount.text = derivedCollectCount()
        binding.commentCount.text = derivedCommentCount()
        binding.relatedText.text = title
        configureVideoLayout(lastVideoWidth, lastVideoHeight)
        binding.videoCover.load(coverUrl) {
            crossfade(false)
            placeholder(android.R.color.black)
            error(android.R.color.black)
        }
        binding.playerStatusText.text = getString(R.string.note_detail_video_status_buffering)
        binding.errorText.text = getString(R.string.note_detail_playback_failed)
        binding.retryButton.text = getString(R.string.note_detail_retry)
        binding.followButton.text = getString(R.string.note_detail_follow)
        binding.commentInput.text = getString(R.string.note_detail_comment_hint)
        refreshVideoActionState()
    }

    private fun bindImageActions(author: String, title: String) {
        val binding = requireImageBinding()
        binding.topFollowButton.setOnClickListener {
            isFollowingAuthor = !isFollowingAuthor
            updateFollowButton()
            Toast.makeText(
                this,
                if (isFollowingAuthor) "已关注 $author" else "已取消关注 $author",
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.relatedBar.setOnClickListener {
            Toast.makeText(this, "相关搜索：$title", Toast.LENGTH_SHORT).show()
        }
        binding.commentComposer.setOnClickListener {
            showCommentDialog()
        }
        binding.commentInput.setOnClickListener {
            showCommentDialog()
        }
        binding.commentIcon.setOnClickListener {
            scrollToCommentSection()
            showCommentDialog()
        }
        binding.likeIcon.setOnClickListener { toggleNoteLike() }
        binding.likeCount.setOnClickListener { toggleNoteLike() }
        binding.collectIcon.setOnClickListener { toggleNoteCollect() }
        binding.collectCount.setOnClickListener { toggleNoteCollect() }
        binding.commentComposers.setOnClickListener {
            showCommentSortPopup(binding.commentSectionTitle)
        }
        binding.commentComposerMic.setOnClickListener {
            Toast.makeText(this, "语音评论入口待接入", Toast.LENGTH_SHORT).show()
        }
        binding.commentComposerCamera.setOnClickListener {
            openCommentImagePicker()
        }
        binding.topFollowButton.setOnClickListener { toggleAuthorFollow(author) }
        binding.commentList.layoutManager = LinearLayoutManager(this)
        binding.commentList.adapter = commentAdapter
    }

    private fun bindVideoActions() {
        val binding = requireVideoBinding()
        binding.followButton.setOnClickListener { toggleAuthorFollow(currentAuthorName) }
        binding.commentInput.setOnClickListener { showCommentDialog() }
        binding.commentIcon.setOnClickListener { showCommentDialog() }
        binding.likeIcon.setOnClickListener { toggleNoteLike() }
        binding.likeCount.setOnClickListener { toggleNoteLike() }
        binding.collectIcon.setOnClickListener { toggleNoteCollect() }
        binding.collectCount.setOnClickListener { toggleNoteCollect() }
    }

    private fun loadRemoteNote() {
        val noteId = currentNoteId ?: return
        lifecycleScope.launch {
            runCatching { noteRepository.getNoteDetail(noteId) }
                .onSuccess { detail ->
                    applyRemoteNote(detail)
                    loadRemoteComments()
                }
                .onFailure { error ->
                    Toast.makeText(
                        this@NoteDetailActivity,
                        error.message ?: "笔记详情加载失败",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun loadRemoteComments() {
        val noteId = currentNoteId ?: return
        lifecycleScope.launch {
            runCatching {
                noteRepository.getComments(
                    noteId = noteId,
                    sortMode = commentSortMode.backendValue()
                )
            }.onSuccess { comments ->
                commentItems.clear()
                commentItems.addAll(comments.map { it.toUiModel() })
                refreshCommentSection()
            }.onFailure { error ->
                Toast.makeText(
                    this@NoteDetailActivity,
                    error.message ?: "评论加载失败",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun applyRemoteNote(detail: RemoteNoteDetailDto) {
        currentNoteId = detail.id
        currentAuthorId = detail.author.id
        currentAuthorName = detail.author.name
        currentNoteTitle = detail.title
        currentVideoUrl = detail.videoUrl.orEmpty()
        currentCoverUrl = detail.coverUrl.orEmpty()
        currentImageUrls = detail.imageUrls.filter { it.isNotBlank() }
        noteLikeCountValue = detail.likeCount
        noteCollectCountValue = detail.collectCount
        isFollowingAuthor = detail.followingAuthor
        isNoteLiked = detail.liked
        isNoteCollected = detail.collected

        if (isVideo) {
            val binding = requireVideoBinding()
            binding.authorName.text = detail.author.name
            binding.avatarText.text = detail.author.avatarText.ifBlank { detail.author.name.take(1) }
            binding.noteTitle.text = detail.title
            binding.noteDescription.text = detail.description
            binding.relatedText.text = detail.title
            binding.videoCover.load(detail.coverUrl) {
                crossfade(false)
                placeholder(android.R.color.black)
                error(android.R.color.black)
            }
            refreshVideoActionState(commentCountOverride = detail.commentCount)
            releasePlayerIfNeeded()
            setupPlayerIfNeeded()
        } else {
            val binding = requireImageBinding()
            binding.topAuthorName.text = detail.author.name
            binding.topAvatarText.text = detail.author.avatarText.ifBlank { detail.author.name.take(1) }
            binding.noteTitle.text = detail.title
            binding.noteDescription.text = detail.description
            binding.relatedText.text = detail.title
            binding.publishTimeText.text = formatPublishTime(detail.createdAt)
            binding.locationText.text = detail.author.location.orEmpty().ifBlank {
                derivedLocation(detail.author.name)
            }
            bindImagePager(currentImageUrls.ifEmpty { collectImageUrls(fallbackUrl = currentCoverUrl) })
            refreshImageActionState()
            binding.commentCount.text = detail.commentCount.toString()
            binding.commentSectionTitle.text = "共 ${detail.commentCount} 条评论"
        }
    }

    private fun toggleAuthorFollow(authorName: String) {
        val authorId = currentAuthorId ?: return
        val targetValue = !isFollowingAuthor
        isFollowingAuthor = targetValue
        refreshActionState()
        lifecycleScope.launch {
            runCatching { noteRepository.followAuthor(authorId, targetValue) }
                .onSuccess {
                    Toast.makeText(
                        this@NoteDetailActivity,
                        if (targetValue) "已关注 $authorName" else "已取消关注 $authorName",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .onFailure { error ->
                    isFollowingAuthor = !targetValue
                    refreshActionState()
                    Toast.makeText(
                        this@NoteDetailActivity,
                        error.message ?: "关注操作失败",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun seedComments(author: String, title: String) {
        commentItems.clear()
        val now = System.currentTimeMillis()
        commentItems += NoteCommentUiModel(
            id = "comment_1",
            author = "小红薯68DDEA9B",
            content = "这组图氛围感很足，尤其是 ${title.take(6)} 这一段看着很舒服。",
            city = "上海",
            timestamp = now - 31L * 60_000L,
            likeCount = 12,
            avatarResId = R.drawable.bg_xhs_avatar_pink,
            replies = mutableListOf(
                NoteReplyUiModel(
                    id = "reply_1",
                    author = author,
                    content = "谢谢喜欢，我当时特意等了这个光线。",
                    city = derivedLocation(author),
                    timestamp = now - 26L * 60_000L,
                    likeCount = 5,
                    isAuthor = true,
                    avatarResId = R.drawable.bg_xhs_avatar_blue_light
                ),
                NoteReplyUiModel(
                    author = "慢慢看海",
                    content = "同感，色调很干净。",
                    city = "广东",
                    timestamp = now - 22L * 60_000L,
                    likeCount = 1,
                    isAuthor = false,
                    avatarResId = R.drawable.bg_xhs_avatar_orange
                )
            )
        )
        commentItems += NoteCommentUiModel(
            id = "comment_2",
            author = "等到天蓝再看海",
            content = "想问下这个机位怎么找的，构图比例很好看。",
            city = "浙江",
            timestamp = now - 3L * 60L * 60_000L,
            likeCount = 8,
            avatarResId = R.drawable.bg_xhs_avatar_teal,
            replies = mutableListOf(
                NoteReplyUiModel(
                    author = author,
                    content = "站位比栏杆再低一点，手机开 2x 会更稳。",
                    city = derivedLocation(author),
                    timestamp = now - 2L * 60L * 60_000L,
                    likeCount = 2,
                    isAuthor = true,
                    avatarResId = R.drawable.bg_xhs_avatar_blue_light
                )
            )
        )
        commentItems += NoteCommentUiModel(
            id = "comment_3",
            author = "夏日气泡水",
            content = "评论区要是能直接回复作者就更方便了，现在这样就顺手多了。",
            city = "四川",
            timestamp = now - 39L * 60_000L,
            likeCount = 3,
            avatarResId = R.drawable.bg_xhs_avatar_green
        )
        commentItems += NoteCommentUiModel(
            id = "comment_4",
            author = author,
            content = "补充一下：原图是傍晚拍的，后期只轻微提亮过。",
            city = derivedLocation(author),
            timestamp = now - 7L * 60_000L,
            likeCount = 1,
            isAuthor = true,
            avatarResId = R.drawable.bg_xhs_avatar_blue_light
        )
    }

    private fun refreshImageActionState() {
        val binding = requireImageBinding()
        binding.likeCount.text = noteLikeCountValue.toString()
        binding.collectCount.text = noteCollectCountValue.toString()
        val accentColor = ContextCompat.getColor(this, R.color.xhs_accent)
        val defaultTextColor = ContextCompat.getColor(this, R.color.xhs_text_primary)
        binding.likeIcon.setColorFilter(if (isNoteLiked) accentColor else defaultTextColor)
        binding.collectIcon.setColorFilter(if (isNoteCollected) accentColor else defaultTextColor)
        binding.likeCount.setTextColor(if (isNoteLiked) accentColor else defaultTextColor)
        binding.collectCount.setTextColor(if (isNoteCollected) accentColor else defaultTextColor)
        updateFollowButton()
    }

    private fun refreshVideoActionState(commentCountOverride: Int? = null) {
        val binding = videoBinding ?: return
        val accentColor = ContextCompat.getColor(this, R.color.xhs_accent)
        val defaultTextColor = ContextCompat.getColor(this, R.color.xhs_text_primary)
        binding.likeCount.text = noteLikeCountValue.toString()
        binding.collectCount.text = noteCollectCountValue.toString()
        commentCountOverride?.let { binding.commentCount.text = it.toString() }
        binding.likeIcon.setColorFilter(if (isNoteLiked) accentColor else defaultTextColor)
        binding.collectIcon.setColorFilter(if (isNoteCollected) accentColor else defaultTextColor)
        binding.likeCount.setTextColor(if (isNoteLiked) accentColor else defaultTextColor)
        binding.collectCount.setTextColor(if (isNoteCollected) accentColor else defaultTextColor)
        updateVideoFollowButton()
    }

    private fun refreshActionState() {
        if (isVideo) refreshVideoActionState() else refreshImageActionState()
    }

    private fun updateFollowButton() {
        val binding = requireImageBinding()
        binding.topFollowButton.text = if (isFollowingAuthor) "已关注" else "关注"
        binding.topFollowButton.background = AppCompatResources.getDrawable(
            this,
            if (isFollowingAuthor) R.drawable.bg_xhs_follow_button_solid
            else R.drawable.bg_xhs_follow_button_outline
        )
        binding.topFollowButton.setTextColor(
            ContextCompat.getColor(
                this,
                if (isFollowingAuthor) android.R.color.white else R.color.xhs_accent
            )
        )
    }

    private fun updateVideoFollowButton() {
        val binding = videoBinding ?: return
        binding.followButton.text = if (isFollowingAuthor) "已关注" else getString(R.string.note_detail_follow)
        binding.followButton.background = AppCompatResources.getDrawable(
            this,
            if (isFollowingAuthor) R.drawable.bg_xhs_follow_button_solid
            else R.drawable.bg_xhs_follow_button_outline
        )
        binding.followButton.setTextColor(
            ContextCompat.getColor(
                this,
                if (isFollowingAuthor) android.R.color.white else R.color.xhs_accent
            )
        )
    }

    private fun toggleNoteLike() {
        val noteId = currentNoteId ?: return
        val targetValue = !isNoteLiked
        isNoteLiked = targetValue
        noteLikeCountValue = (noteLikeCountValue + if (targetValue) 1 else -1).coerceAtLeast(0)
        refreshActionState()
        lifecycleScope.launch {
            runCatching { noteRepository.toggleLike(noteId, targetValue) }
                .onFailure { error ->
                    isNoteLiked = !targetValue
                    noteLikeCountValue = (noteLikeCountValue + if (targetValue) -1 else 1).coerceAtLeast(0)
                    refreshActionState()
                    Toast.makeText(
                        this@NoteDetailActivity,
                        error.message ?: "点赞失败",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun toggleNoteCollect() {
        val noteId = currentNoteId ?: return
        val targetValue = !isNoteCollected
        isNoteCollected = targetValue
        noteCollectCountValue = (noteCollectCountValue + if (targetValue) 1 else -1).coerceAtLeast(0)
        refreshActionState()
        lifecycleScope.launch {
            runCatching { noteRepository.toggleCollect(noteId, targetValue) }
                .onFailure { error ->
                    isNoteCollected = !targetValue
                    noteCollectCountValue = (noteCollectCountValue + if (targetValue) -1 else 1).coerceAtLeast(0)
                    refreshActionState()
                    Toast.makeText(
                        this@NoteDetailActivity,
                        error.message ?: "收藏失败",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun refreshCommentSection() {
        val binding = requireImageBinding()
        val comments = when (commentSortMode) {
            CommentSortMode.DEFAULT -> commentItems.sortedWith(
                compareByDescending<NoteCommentUiModel> { it.likeCount }
                    .thenByDescending { it.timestamp }
            )
            CommentSortMode.LATEST -> commentItems.sortedByDescending { it.timestamp }
            CommentSortMode.MOST_LIKED -> commentItems.sortedByDescending { it.likeCount }
        }
        commentAdapter.submitList(comments)
        binding.commentCount.text = commentItems.size.toString()
        binding.commentSectionTitle.text = "共 ${commentItems.size} 条评论"
    }

    private fun showCommentSortPopup(anchor: View) {
        val contentView = LayoutInflater.from(this).inflate(R.layout.layout_comment_sort_popup, null)
        val popupWindow = PopupWindow(
            contentView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        fun bindCheck(viewId: Int, mode: CommentSortMode) {
            contentView.findViewById<ImageView>(viewId).visibility =
                if (commentSortMode == mode) View.VISIBLE else View.INVISIBLE
        }

        fun bindAction(optionId: Int, mode: CommentSortMode) {
            contentView.findViewById<View>(optionId).setOnClickListener {
                commentSortMode = mode
                popupWindow.dismiss()
                loadRemoteComments()
            }
        }

        bindCheck(R.id.sortOptionDefaultCheck, CommentSortMode.DEFAULT)
        bindCheck(R.id.sortOptionLatestCheck, CommentSortMode.LATEST)
        bindCheck(R.id.sortOptionLikesCheck, CommentSortMode.MOST_LIKED)

        bindAction(R.id.sortOptionDefault, CommentSortMode.DEFAULT)
        bindAction(R.id.sortOptionLatest, CommentSortMode.LATEST)
        bindAction(R.id.sortOptionLikes, CommentSortMode.MOST_LIKED)

        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isOutsideTouchable = true
        popupWindow.elevation = dpToPx(this, 10).toFloat()

        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        popupWindow.showAsDropDown(anchor, 0, dpToPx(this, 6), Gravity.START)
    }

    private fun toggleCommentLike(comment: NoteCommentUiModel) {
        val targetValue = !comment.isLiked
        comment.isLiked = targetValue
        comment.likeCount = (comment.likeCount + if (targetValue) 1 else -1).coerceAtLeast(0)
        refreshCommentSection()
        lifecycleScope.launch {
            runCatching { noteRepository.toggleCommentLike(comment.id, targetValue) }
                .onFailure { error ->
                    comment.isLiked = !targetValue
                    comment.likeCount = (comment.likeCount + if (targetValue) -1 else 1).coerceAtLeast(0)
                    refreshCommentSection()
                    Toast.makeText(
                        this@NoteDetailActivity,
                        error.message ?: "评论点赞失败",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun toggleCommentReplies(comment: NoteCommentUiModel) {
        comment.isReplyExpanded = !comment.isReplyExpanded
        refreshCommentSection()
    }

    private fun toggleReplyLike(reply: NoteReplyUiModel) {
        val targetValue = !reply.isLiked
        reply.isLiked = targetValue
        reply.likeCount = (reply.likeCount + if (targetValue) 1 else -1).coerceAtLeast(0)
        refreshCommentSection()
        lifecycleScope.launch {
            runCatching { noteRepository.toggleCommentLike(reply.id, targetValue) }
                .onFailure { error ->
                    reply.isLiked = !targetValue
                    reply.likeCount = (reply.likeCount + if (targetValue) -1 else 1).coerceAtLeast(0)
                    refreshCommentSection()
                    Toast.makeText(
                        this@NoteDetailActivity,
                        error.message ?: "回复点赞失败",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun showCommentDialog(
        parentComment: NoteCommentUiModel? = null,
        replyToAuthor: String? = null,
        selectedImageUri: String? = null
    ) {
        activeCommentDialog?.dismiss()
        val contentView = LayoutInflater.from(this).inflate(R.layout.layout_comment_input_dialog, null)
        val input = contentView.findViewById<EditText>(R.id.commentDialogInput).apply {
            hint = when {
                replyToAuthor != null -> "回复 @$replyToAuthor"
                parentComment != null -> "回复 ${parentComment.author}"
                else -> "写下你的想法吧"
            }
        }
        val imageButton = contentView.findViewById<TextView>(R.id.commentDialogImageButton)
        val imagePreviewContainer =
            contentView.findViewById<FrameLayout>(R.id.commentDialogImagePreviewContainer)
        val imagePreview = contentView.findViewById<ImageView>(R.id.commentDialogImagePreview)
        val removeImageButton = contentView.findViewById<ImageView>(R.id.commentDialogRemoveImage)

        imageButton.visibility = View.VISIBLE
        imagePreviewContainer.visibility = View.GONE

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (parentComment == null) "发布评论" else "回复评论")
            .setView(contentView)
            .setNegativeButton("取消", null)
            .setPositiveButton("发送", null)
            .create()
        val dialogState = CommentDialogState(
            dialog = dialog,
            input = input,
            imageButton = imageButton,
            imagePreviewContainer = imagePreviewContainer,
            imagePreview = imagePreview,
            removeImageButton = removeImageButton,
            parentComment = parentComment,
            replyToAuthor = replyToAuthor,
            selectedImageUri = selectedImageUri
        )
        activeCommentDialog = dialog
        activeCommentDialogState = dialogState

        imageButton.setOnClickListener {
            openCommentImagePicker()
        }
        removeImageButton.setOnClickListener {
            dialogState.selectedImageUri = null
            renderCommentDialogImageState(dialogState)
        }

        dialog.setOnShowListener {
            renderCommentDialogImageState(dialogState)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val content = input.text?.toString()?.trim().orEmpty()
                if (content.isBlank() && dialogState.selectedImageUri.isNullOrBlank()) {
                    input.error = "请输入文字或添加图片"
                    return@setOnClickListener
                }
                publishComment(
                    content = content,
                    parentComment = parentComment,
                    imageUri = dialogState.selectedImageUri
                )
                dialog.dismiss()
            }
        }
        dialog.setOnDismissListener {
            if (activeCommentDialog === dialog) {
                activeCommentDialog = null
                activeCommentDialogState = null
            }
        }
        dialog.show()
    }

    private fun publishComment(
        content: String,
        parentComment: NoteCommentUiModel?,
        imageUri: String?
    ) {
        val noteId = currentNoteId ?: return
        lifecycleScope.launch {
            runCatching {
                noteRepository.createComment(
                    noteId = noteId,
                    content = content,
                    parentCommentId = parentComment?.id,
                    replyToUserName = activeCommentDialogState?.replyToAuthor,
                    imageUrl = imageUri
                )
            }.onSuccess { remoteComment ->
                if (parentComment == null) {
                    commentItems.add(0, remoteComment.toUiModel())
                    Toast.makeText(this@NoteDetailActivity, "评论已发布", Toast.LENGTH_SHORT).show()
                } else {
                    parentComment.replies.add(0, remoteComment.toReplyUiModel())
                    parentComment.isReplyExpanded = true
                    Toast.makeText(this@NoteDetailActivity, "回复已发布", Toast.LENGTH_SHORT).show()
                }
                refreshCommentSection()
                scrollToCommentSection()
            }.onFailure { error ->
                Toast.makeText(
                    this@NoteDetailActivity,
                    error.message ?: "评论发布失败",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        return
        val now = System.currentTimeMillis()
        if (parentComment == null) {
            commentItems.add(
                0,
                NoteCommentUiModel(
                    id = "comment_$now",
                    author = "我",
                    content = content,
                    city = "当前城市",
                    timestamp = now,
                    likeCount = 0,
                    avatarResId = R.drawable.bg_xhs_avatar_blue_light,
                    imageUri = imageUri
                )
            )
            Toast.makeText(this, "评论已发布", Toast.LENGTH_SHORT).show()
        } else {
            parentComment.replies.add(
                0,
                NoteReplyUiModel(
                    author = "我",
                    content = content,
                    city = "当前城市",
                    timestamp = now,
                    likeCount = 0,
                    isAuthor = false,
                    avatarResId = R.drawable.bg_xhs_avatar_blue_light,
                    imageUri = imageUri
                )
            )
            parentComment.isReplyExpanded = true
            Toast.makeText(this, "回复已发送", Toast.LENGTH_SHORT).show()
        }
        refreshCommentSection()
        scrollToCommentSection()
    }

    private fun openCommentImagePicker() {
        commentImagePicker.launch("image/*")
    }

    private fun handlePickedCommentImage(uri: Uri?) {
        val uriString = uri?.toString() ?: return
        val dialogState = activeCommentDialogState
        if (dialogState == null) {
            showCommentDialog(selectedImageUri = uriString)
            return
        }
        dialogState.selectedImageUri = uriString
        renderCommentDialogImageState(dialogState)
    }

    private fun renderCommentDialogImageState(state: CommentDialogState) {
        val hasImage = !state.selectedImageUri.isNullOrBlank()
        state.imageButton.text = if (hasImage) "更换图片" else "添加图片"
        state.imagePreviewContainer.visibility = if (hasImage) View.VISIBLE else View.GONE
        if (hasImage) {
            state.imagePreview.load(state.selectedImageUri)
        } else {
            state.imagePreview.setImageDrawable(null)
        }
    }

    private fun scrollToCommentSection() {
        val binding = requireImageBinding()
        binding.scrollContainer.post {
            binding.scrollContainer.smoothScrollTo(0, binding.commentSectionTitle.top)
        }
    }

    private fun bindImagePager(imageUrls: List<String>) {
        val binding = requireImageBinding()
        binding.detailImagePager.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.detailImagePager.adapter = imagePagerAdapter
        imagePagerAdapter.onImageTap = { setImageFullscreen(!isFullscreen) }
        imagePagerAdapter.isFullscreen = isFullscreen
        if (binding.detailImagePager.onFlingListener == null) {
            imagePagerSnapHelper.attachToRecyclerView(binding.detailImagePager)
        }
        binding.detailImagePager.clearOnScrollListeners()
        binding.detailImagePager.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updatePagerHeight()
                    updatePagerIndicator(currentImagePosition())
                }
            }
        })
        imagePagerAdapter.submitList(imageUrls)
        binding.detailImagePager.post {
            applyImageModeUi()
            updatePagerIndicator(0)
            updatePagerHeight()
        }
    }

    private fun updatePagerHeight() {
        val binding = requireImageBinding()
        val firstChild = binding.detailImagePager.getChildAt(0) ?: return
        val targetHeight = firstChild.measuredHeight.takeIf { it > 0 } ?: return
        binding.detailImagePager.layoutParams = binding.detailImagePager.layoutParams.apply {
            height = targetHeight
        }
    }

    private fun updatePagerIndicator(selectedIndex: Int) {
        val binding = requireImageBinding()
        val itemCount = imagePagerAdapter.itemCount
        binding.imagePagerIndicator.removeAllViews()
        binding.imagePagerIndicator.visibility = if (itemCount > 1) View.VISIBLE else View.GONE
        if (itemCount <= 1) return

        repeat(itemCount) { index ->
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(6), dpToPx(6)).apply {
                    if (index > 0) marginStart = dpToPx(6)
                }
                background = AppCompatResources.getDrawable(
                    this@NoteDetailActivity,
                    if (index == selectedIndex) R.drawable.bg_xhs_pager_dot_active
                    else R.drawable.bg_xhs_pager_dot_inactive
                )
            }
            binding.imagePagerIndicator.addView(dot)
        }
    }

    private fun currentImagePosition(): Int {
        val binding = requireImageBinding()
        val layoutManager = binding.detailImagePager.layoutManager ?: return 0
        val snapView = imagePagerSnapHelper.findSnapView(layoutManager) ?: return 0
        return layoutManager.getPosition(snapView).coerceAtLeast(0)
    }

    private fun setImageFullscreen(enabled: Boolean) {
        if (isVideo || isFullscreen == enabled) return
        isFullscreen = enabled
        applyImageModeUi()
        imagePagerAdapter.isFullscreen = enabled
        imagePagerAdapter.notifyDataSetChanged()
        requireImageBinding().detailImagePager.post { updatePagerHeight() }
    }

    private fun applyImageModeUi() {
        val binding = imageBinding ?: return
        WindowCompat.setDecorFitsSystemWindows(window, !isFullscreen)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            if (isFullscreen) {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                show(WindowInsetsCompat.Type.systemBars())
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
        val surfaceColor = ContextCompat.getColor(binding.root.context, R.color.xhs_card)
        binding.root.setBackgroundColor(if (isFullscreen) Color.BLACK else surfaceColor)
        binding.mediaContainer.setBackgroundColor(if (isFullscreen) Color.BLACK else surfaceColor)
        binding.topBar.visibility = if (isFullscreen) View.GONE else View.VISIBLE
        binding.contentBottomContainer.visibility = if (isFullscreen) View.GONE else View.VISIBLE
        binding.bottomBar.visibility = if (isFullscreen) View.GONE else View.VISIBLE
        binding.imagePagerIndicator.visibility = when {
            isFullscreen -> View.GONE
            imagePagerAdapter.itemCount > 1 -> View.VISIBLE
            else -> View.GONE
        }
    }

    private fun collectImageUrls(fallbackUrl: String): List<String> {
        val explicitUrls = intent.getStringArrayListExtra(EXTRA_IMAGE_URLS).orEmpty()
            .filter { it.isNotBlank() }
        if (explicitUrls.isNotEmpty()) return explicitUrls

        val singleUrl = intent.getStringExtra(EXTRA_IMAGE_URL).orEmpty()
        return listOfNotNull(
            singleUrl.takeIf { it.isNotBlank() },
            fallbackUrl.takeIf { it.isNotBlank() && it != singleUrl }
        )
    }

    private fun setupPlayerIfNeeded() {
        val videoUrl = currentVideoUrl
        if (videoUrl.isBlank()) return

        val binding = requireVideoBinding()
        val player = cachedPlayer ?: MediaPlayerFactory.create(this).also { createdPlayer ->
            cachedPlayer = createdPlayer
            createdPlayer.repeatMode = Player.REPEAT_MODE_OFF
            createdPlayer.playWhenReady = false
            createdPlayer.volume = if (isMuted()) 0f else 1f
            createdPlayer.playbackParameters = PlaybackParameters(getSavedPlaybackSpeed())
            attachPlayerListener(createdPlayer, videoUrl)
            hasRenderedFirstFrame = false

            MediaPlayerFactory.prepare(
                player = createdPlayer,
                mediaItem = MediaItemFactory.guessVideo(videoUrl),
                playWhenReady = false
            )

            val savedPosition = getSavedPlaybackProgress(videoUrl)
            if (savedPosition > 0L) {
                pendingSeekPositionMs = savedPosition
                createdPlayer.seekTo(savedPosition)
            }
        }

        binding.detailVideoView.player = player
        player.volume = if (isMuted()) 0f else 1f
        player.playbackParameters = PlaybackParameters(getSavedPlaybackSpeed())
        updatePlaybackUi(player)
        updateProgressUi(player)
        binding.root.post { refreshVideoActionLabels() }
        updateSpeedButton()
    }

    private fun releasePlayerIfNeeded() {
        val player = cachedPlayer ?: return
        savePlaybackProgress(
            videoUrl = currentVideoUrl,
            positionMs = player.currentPosition,
            durationMs = player.duration
        )
        stopPlaybackService()
        playerListener?.let(player::removeListener)
        playerListener = null
        videoBinding?.detailVideoView?.player = null
        player.release()
        cachedPlayer = null
        pendingSeekPositionMs = C.TIME_UNSET
        hasRenderedFirstFrame = false
        pendingNetworkRecovery = false
        shouldResumeAfterNetworkRecovery = false
    }

    private fun attachPlayerListener(player: Player, videoUrl: String) {
        playerListener?.let(player::removeListener)
        val binding = requireVideoBinding()
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        binding.playerStatusText.text = getString(R.string.note_detail_video_status_buffering)
                        binding.playerStatusText.visibility = View.VISIBLE
                        binding.loadingIndicator.visibility = View.VISIBLE
                    }

                    Player.STATE_READY -> {
                        binding.loadingIndicator.visibility = View.GONE
                        binding.playerStatusText.visibility = View.GONE
                        binding.errorContainer.visibility = View.GONE
                        pendingNetworkRecovery = false
                        if (pendingSeekPositionMs != C.TIME_UNSET) {
                            player.seekTo(pendingSeekPositionMs)
                            pendingSeekPositionMs = C.TIME_UNSET
                        }
                        updateProgressUi(player)
                    }

                    Player.STATE_ENDED -> {
                        savePlaybackProgress(videoUrl, 0L, player.duration)
                        binding.playerStatusText.text = getString(R.string.note_detail_video_status_completed)
                        binding.playerStatusText.visibility = View.VISIBLE
                        binding.loadingIndicator.visibility = View.GONE
                        showPausedCover()
                        updateProgressUi(player)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startPlaybackService()
                } else {
                    stopPlaybackService()
                }
                updatePlaybackUi(player)
                if (isPlaying) startProgressUpdates() else stopProgressUpdates()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width <= 0 || videoSize.height <= 0) return
                lastVideoWidth = videoSize.width
                lastVideoHeight = videoSize.height
                configureVideoLayout(lastVideoWidth, lastVideoHeight)
            }

            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
                if (player.isPlaying) {
                    binding.videoCover.visibility = View.GONE
                }
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                savePlaybackSpeed(playbackParameters.speed)
                updateSpeedButton()
            }

            override fun onPlayerError(error: PlaybackException) {
                binding.loadingIndicator.visibility = View.GONE
                binding.playerStatusText.visibility = View.GONE
                binding.errorContainer.visibility = View.VISIBLE
                binding.errorText.text = resolvePlaybackErrorMessage(error)
                pendingNetworkRecovery = !hasActiveNetwork()
                showPausedCover()
            }
        }
        playerListener = listener
        player.addListener(listener)
    }

    private fun bindVideoEvents() {
        val binding = requireVideoBinding()
        val gestureDetector = createVideoGestureDetector()

        val toggleClickListener = View.OnClickListener { toggleVideoPlayback() }
        binding.playOverlay.setOnClickListener(toggleClickListener)
        binding.videoCover.setOnClickListener(toggleClickListener)
        binding.fullscreenButton.setOnClickListener { setFullscreen(!isFullscreen) }
        binding.muteButton.setOnClickListener { toggleMute() }
        binding.speedButton.setOnClickListener { cyclePlaybackSpeed() }
        binding.retryButton.setOnClickListener { retryPlayback() }

        binding.detailVideoView.setOnTouchListener { _, event ->
            val handled = gestureDetector.onTouchEvent(event)
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                binding.detailVideoView.parent?.requestDisallowInterceptTouchEvent(false)
            } else {
                binding.detailVideoView.parent?.requestDisallowInterceptTouchEvent(true)
            }
            handled
        }

        binding.playbackSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val player = binding.detailVideoView.player ?: return
                val duration = player.duration.takeIf { it > 0 } ?: return
                isSeeking = true
                val previewPosition = duration * progress / seekBar.max
                binding.currentPositionText.text = formatDuration(previewPosition)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val player = binding.detailVideoView.player ?: return
                val duration = player.duration.takeIf { it > 0 } ?: return
                val seekTarget = duration * seekBar.progress / seekBar.max
                player.seekTo(seekTarget)
                isSeeking = false
                updateProgressUi(player)
            }
        })
    }

    private fun createVideoGestureDetector(): GestureDetectorCompat {
        val binding = requireVideoBinding()
        val touchSlop = dpToPx(10).toFloat()
        var horizontalScrollConsumed = false
        var verticalScrollConsumed = false

        return GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                horizontalScrollConsumed = false
                verticalScrollConsumed = false
                gestureSeekBasePositionMs = cachedPlayer?.currentPosition ?: 0L
                gestureBrightnessBase = currentScreenBrightness()
                gestureVolumeBase = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                toggleVideoPlayback()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                toggleVideoPlayback()
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                val player = binding.detailVideoView.player ?: return false
                val duration = player.duration.takeIf { it > 0 } ?: return false
                val diffX = e2.x - (e1?.x ?: e2.x)
                val diffY = e2.y - (e1?.y ?: e2.y)

                if (!horizontalScrollConsumed && !verticalScrollConsumed) {
                    if (abs(diffX) < touchSlop && abs(diffY) < touchSlop) return false
                    horizontalScrollConsumed = abs(diffX) > abs(diffY)
                    verticalScrollConsumed = !horizontalScrollConsumed
                    if (horizontalScrollConsumed) {
                        gestureSeekBasePositionMs = player.currentPosition
                    }
                }

                return if (horizontalScrollConsumed) {
                    val hostWidth = binding.videoHost.width.takeIf { it > 0 } ?: return false
                    val deltaMs = ((diffX / hostWidth) * duration * 0.9f).roundToInt().toLong()
                    val targetPosition = (gestureSeekBasePositionMs + deltaMs).coerceIn(0L, duration)
                    player.seekTo(targetPosition)
                    updateProgressUi(player)
                    showGestureHint(getString(R.string.note_detail_seek_hint, formatDuration(targetPosition)))
                    true
                } else {
                    if (e1 != null && e1.x < binding.videoHost.width / 2f) {
                        adjustBrightnessByGesture(diffY)
                    } else {
                        adjustVideoVolumeByGesture(diffY)
                    }
                    true
                }
            }
        })
    }

    private fun adjustVideoVolumeByGesture(totalDiffY: Float) {
        val manager = audioManager ?: return
        val maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val hostHeight = requireVideoBinding().videoHost.height.coerceAtLeast(1)
        val deltaSteps = ((-totalDiffY / hostHeight) * maxVolume).roundToInt()
        val nextVolume = (gestureVolumeBase + deltaSteps).coerceIn(0, maxVolume)
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, nextVolume, 0)
        saveMuteState(nextVolume == 0)
        cachedPlayer?.volume = if (nextVolume == 0) 0f else 1f
        refreshVideoActionLabels()
        showGestureHint(getString(R.string.note_detail_volume_hint, nextVolume * 100 / maxVolume))
    }

    private fun adjustBrightnessByGesture(totalDiffY: Float) {
        val hostHeight = requireVideoBinding().videoHost.height.coerceAtLeast(1)
        val delta = (-totalDiffY / hostHeight).coerceIn(-1f, 1f)
        val nextBrightness = (gestureBrightnessBase + delta).coerceIn(0.1f, 1f)
        val layoutParams = window.attributes
        layoutParams.screenBrightness = nextBrightness
        window.attributes = layoutParams
        showGestureHint(getString(R.string.note_detail_brightness_hint, (nextBrightness * 100).roundToInt()))
    }

    private fun retryPlayback(autoPlay: Boolean = true) {
        val binding = requireVideoBinding()
        binding.errorContainer.visibility = View.GONE
        binding.playerStatusText.visibility = View.GONE
        pendingNetworkRecovery = false
        val player = cachedPlayer
        if (player == null) {
            setupPlayerIfNeeded()
            if (autoPlay) {
                requireVideoBinding().detailVideoView.player?.play()
            }
            return
        }
        hasRenderedFirstFrame = false
        player.prepare()
        if (autoPlay) {
            player.play()
        } else {
            updatePlaybackUi(player)
        }
    }

    private fun toggleVideoPlayback() {
        val binding = requireVideoBinding()
        val player = binding.detailVideoView.player ?: return

        if (!hasActiveNetwork()) {
            pendingNetworkRecovery = true
            shouldResumeAfterNetworkRecovery = true
            binding.errorContainer.visibility = View.VISIBLE
            binding.errorText.text = getString(R.string.note_detail_network_lost)
            showPausedCover()
            return
        }

        if (!hasShownMeteredNetworkHint && isOnMeteredNetwork()) {
            hasShownMeteredNetworkHint = true
            Toast.makeText(this, "当前为移动网络，请注意流量消耗", Toast.LENGTH_SHORT).show()
        }

        if (player.isPlaying) {
            shouldResumeAfterNetworkRecovery = false
            player.pause()
            showPausedCover()
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
            }
            binding.errorContainer.visibility = View.GONE
            player.play()
            updatePlaybackUi(player)
        }
    }

    private fun toggleMute() {
        val muted = !isMuted()
        saveMuteState(muted)
        cachedPlayer?.volume = if (muted) 0f else 1f
        refreshVideoActionLabels()
    }

    private fun cyclePlaybackSpeed() {
        val currentSpeed = getSavedPlaybackSpeed()
        val currentIndex = playbackSpeeds.indexOfFirst { abs(it - currentSpeed) < 0.01f }
        val safeIndex = if (currentIndex >= 0) currentIndex else 1
        val nextSpeed = playbackSpeeds[(safeIndex + 1) % playbackSpeeds.size]
        savePlaybackSpeed(nextSpeed)
        requireVideoBinding().detailVideoView.player?.playbackParameters = PlaybackParameters(nextSpeed)
        updateSpeedButton()
        showGestureHint("${nextSpeed}x")
    }

    private fun updatePlaybackUi(player: Player) {
        val binding = requireVideoBinding()
        val isPlaying = player.isPlaying
        val isBuffering = player.playbackState == Player.STATE_BUFFERING
        val shouldShowCover = when {
            player.playbackState == Player.STATE_ENDED -> true
            isPlaying -> !hasRenderedFirstFrame
            else -> true
        }
        binding.playOverlay.visibility = if (isPlaying || isBuffering) View.GONE else View.VISIBLE
        binding.videoCover.visibility = if (shouldShowCover) View.VISIBLE else View.GONE
        binding.loadingIndicator.visibility = if (isBuffering) View.VISIBLE else View.GONE
        if (!isPlaying && !isBuffering) {
            binding.playerStatusText.visibility = View.GONE
        }
    }

    private fun showPausedCover() {
        val binding = requireVideoBinding()
        binding.playOverlay.visibility = View.VISIBLE
        binding.videoCover.visibility = View.VISIBLE
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        val binding = videoBinding ?: return
        val runnable = object : Runnable {
            override fun run() {
                val player = binding.detailVideoView.player ?: return
                updateProgressUi(player)
                binding.playbackSeekBar.postDelayed(this, 500L)
            }
        }
        progressUpdater = runnable
        binding.playbackSeekBar.post(runnable)
    }

    private fun stopProgressUpdates() {
        val binding = videoBinding ?: return
        progressUpdater?.let(binding.playbackSeekBar::removeCallbacks)
        progressUpdater = null
    }

    private fun updateProgressUi(player: Player) {
        val binding = videoBinding ?: return
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        val position = player.currentPosition.coerceIn(0L, duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
        binding.durationText.text = formatDuration(duration)
        if (!isSeeking) {
            binding.currentPositionText.text = formatDuration(position)
            binding.playbackSeekBar.progress =
                if (duration > 0L) ((position * binding.playbackSeekBar.max) / duration).toInt() else 0
        }
    }

    private fun configureVideoLayout(videoWidth: Int, videoHeight: Int) {
        val binding = requireVideoBinding()
        lastVideoWidth = videoWidth
        lastVideoHeight = videoHeight
        val isPortraitVideo = videoHeight > videoWidth
        val videoAspectRatio = videoHeight.toFloat() / videoWidth.toFloat()

        binding.root.doOnLayout {
            val hostWidth = binding.videoHost.width.takeIf { it > 0 } ?: binding.root.width
            val hostHeight = binding.root.height.takeIf { it > 0 } ?: binding.videoHost.height
            if (hostWidth <= 0 || hostHeight <= 0) return@doOnLayout

            val containerAspectRatio = hostHeight.toFloat() / hostWidth.toFloat()
            binding.detailVideoView.resizeMode = resolveVideoResizeMode(
                videoAspectRatio = videoAspectRatio,
                containerAspectRatio = containerAspectRatio
            )

            val layoutParams = binding.videoHost.layoutParams
            layoutParams.height = if (isFullscreen) {
                hostHeight
            } else {
                val desiredHeight = (hostWidth * videoAspectRatio).roundToInt()
                val maxHeight = (hostHeight * if (isPortraitVideo) 0.72f else 0.48f).roundToInt()
                val minHeight = dpToPx(if (isPortraitVideo) 320 else 200)
                val safeMinHeight = minHeight.coerceAtMost(maxHeight)
                desiredHeight.coerceIn(safeMinHeight, maxHeight)
            }
            binding.videoHost.layoutParams = layoutParams
        }

        binding.videoCover.scaleType = if (isFullscreen || isPortraitVideo) {
            ImageView.ScaleType.CENTER_CROP
        } else {
            ImageView.ScaleType.FIT_CENTER
        }
        binding.videoActionRow.visibility = View.VISIBLE
    }

    private fun setFullscreen(enabled: Boolean) {
        if (!isVideo || isFullscreen == enabled) return
        isFullscreen = enabled
        try {
            requestedOrientation = if (enabled) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        } catch (error: RuntimeException) {
            Log.w(TAG, "Failed to change orientation for fullscreen toggle.", error)
        }
        applyVideoModeUi()
        refreshVideoActionLabels()
        videoBinding?.root?.post {
            configureVideoLayout(lastVideoWidth, lastVideoHeight)
        }
    }

    private fun resolveVideoResizeMode(
        videoAspectRatio: Float,
        containerAspectRatio: Float
    ): Int {
        if (!isFullscreen) return AspectRatioFrameLayout.RESIZE_MODE_FIT

        val aspectDelta = abs(videoAspectRatio - containerAspectRatio)
        return if (aspectDelta < 0.08f) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT
        } else {
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
    }

    private fun applyVideoModeUi() {
        val binding = videoBinding ?: return
        WindowCompat.setDecorFitsSystemWindows(window, !isFullscreen)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            if (isFullscreen) {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                show(WindowInsetsCompat.Type.systemBars())
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
        binding.topBar.visibility = if (isFullscreen) View.GONE else View.VISIBLE
        binding.bottomBar.visibility = if (isFullscreen) View.GONE else View.VISIBLE
        binding.contentBottomContainer.visibility = if (isFullscreen) View.GONE else View.VISIBLE
        binding.root.post { refreshVideoActionLabels() }
        binding.fullscreenButton.text = if (isFullscreen) "退出全屏" else "全屏"
    }

    private fun updateMuteButton() {
        videoBinding?.muteButton?.text = if (isMuted()) "取消静音" else "静音"
    }

    private fun updateSpeedButton() {
        videoBinding?.speedButton?.text = "${getSavedPlaybackSpeed()}x"
    }

    private fun refreshVideoActionLabels() {
        val binding = videoBinding ?: return
        binding.fullscreenButton.text = if (isFullscreen) {
            getString(R.string.note_detail_exit_fullscreen)
        } else {
            getString(R.string.note_detail_enter_fullscreen)
        }
        binding.muteButton.text = if (isMuted()) {
            getString(R.string.note_detail_unmute)
        } else {
            getString(R.string.note_detail_mute)
        }
    }

    private fun savePlaybackProgress(videoUrl: String, positionMs: Long, durationMs: Long) {
        if (videoUrl.isBlank()) return
        val safePosition = if (durationMs > 0 && durationMs - positionMs <= 2_000L) 0L else positionMs
        playbackPrefs.edit().putLong(progressKey(videoUrl), safePosition.coerceAtLeast(0L)).apply()
    }

    private fun getSavedPlaybackProgress(videoUrl: String): Long {
        if (videoUrl.isBlank()) return 0L
        return playbackPrefs.getLong(progressKey(videoUrl), 0L)
    }

    private fun savePlaybackSpeed(speed: Float) {
        playbackPrefs.edit().putFloat(KEY_PLAYBACK_SPEED, speed).apply()
    }

    private fun getSavedPlaybackSpeed(): Float {
        return playbackPrefs.getFloat(KEY_PLAYBACK_SPEED, 1f)
    }

    private fun saveMuteState(muted: Boolean) {
        playbackPrefs.edit().putBoolean(KEY_MUTED, muted).apply()
    }

    private fun isMuted(): Boolean {
        return playbackPrefs.getBoolean(KEY_MUTED, false)
    }

    private fun progressKey(videoUrl: String): String = "progress_$videoUrl"

    private fun showGestureHint(message: String) {
        val binding = videoBinding ?: return
        binding.gestureHintText.text = message
        binding.gestureHintText.visibility = View.VISIBLE
        gestureHideRunnable?.let(binding.gestureHintText::removeCallbacks)
        val runnable = Runnable {
            binding.gestureHintText.visibility = View.GONE
            gestureHideRunnable = null
        }
        gestureHideRunnable = runnable
        binding.gestureHintText.postDelayed(runnable, 900L)
    }

    private fun registerNetworkCallbackIfNeeded() {
        if (networkCallback != null) return
        val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                runOnUiThread {
                    if (pendingNetworkRecovery) {
                        retryPlayback(autoPlay = shouldResumeAfterNetworkRecovery)
                    }
                }
            }

            override fun onLost(network: android.net.Network) {
                runOnUiThread {
                    if (!hasActiveNetwork()) {
                        cachedPlayer?.let { player ->
                            shouldResumeAfterNetworkRecovery = player.isPlaying || player.playWhenReady
                            pendingNetworkRecovery = true
                            player.pause()
                            if (videoBinding != null) {
                                requireVideoBinding().errorContainer.visibility = View.VISIBLE
                                requireVideoBinding().errorText.text = getString(R.string.note_detail_network_lost)
                                showPausedCover()
                            }
                        }
                    }
                }
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        networkCallback = callback
    }

    private fun unregisterNetworkCallback() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return
        val callback = networkCallback ?: return
        connectivityManager.unregisterNetworkCallback(callback)
        networkCallback = null
    }

    private fun syncPlayerVolumeWithSystem() {
        val manager = audioManager ?: return
        val currentVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        saveMuteState(currentVolume == 0)
        cachedPlayer?.volume = if (currentVolume == 0) 0f else 1f
        refreshVideoActionLabels()
    }

    private fun startPlaybackService() {
        NotificationService.start(this)
    }

    private fun stopPlaybackService() {
        NotificationService.stop(this)
    }

    private fun currentScreenBrightness(): Float {
        val brightness = window.attributes.screenBrightness
        return if (brightness in 0f..1f) brightness else DEFAULT_GESTURE_BRIGHTNESS
    }

    private fun resolvePlaybackErrorMessage(error: PlaybackException): String {
        return when {
            !hasActiveNetwork() -> getString(R.string.note_detail_network_lost)
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> {
                getString(R.string.note_detail_network_connect_failed)
            }

            error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                getString(R.string.note_detail_video_unavailable)
            }

            else -> getString(R.string.note_detail_playback_failed)
        }
    }
    private fun hasActiveNetwork(): Boolean {
        val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isOnMeteredNetwork(): Boolean {
        val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun derivedCollectCount(): String {
        return ((intent.getStringExtra(EXTRA_LIKE_COUNT)?.toIntOrNull() ?: 0) / 2).toString()
    }

    private fun derivedCommentCount(): String {
        return ((intent.getStringExtra(EXTRA_LIKE_COUNT)?.toIntOrNull() ?: 0) / 6).toString()
    }

    private fun derivedPublishTime(): String {
        return "昨天 23:23"
    }

    private fun derivedLocation(author: String): String {
        val presetLocations = listOf("江苏", "浙江", "北京", "广东", "四川", "上海")
        val index = author.hashCode().mod(presetLocations.size)
        return presetLocations[index]
    }

    private fun formatPublishTime(createdAt: String): String {
        val instant = parseInstantMillis(createdAt) ?: return createdAt
        return publishTimeFormatter.format(Instant.ofEpochMilli(instant).atZone(ZoneId.systemDefault()))
    }

    private fun parseInstantMillis(value: String): Long? {
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
    }

    private fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "00:00"
        val totalSeconds = durationMs / 1000L
        val seconds = totalSeconds % 60L
        val minutes = (totalSeconds / 60L) % 60L
        val hours = totalSeconds / 3600L
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    private fun dpToPx(valueDp: Int): Int {
        return (valueDp * resources.displayMetrics.density).roundToInt()
    }

    private fun requireImageBinding(): ActivityNoteDetailImageBinding {
        return checkNotNull(imageBinding) { "Image binding is not initialized." }
    }

    private fun requireVideoBinding(): ActivityNoteDetailVideoBinding {
        return checkNotNull(videoBinding) { "Video binding is not initialized." }
    }

    private fun RemoteCommentDto.toUiModel(): NoteCommentUiModel {
        return NoteCommentUiModel(
            id = id,
            author = author,
            content = content,
            city = city,
            timestamp = parseInstantMillis(createdAt) ?: System.currentTimeMillis(),
            likeCount = likeCount,
            isLiked = liked,
            isAuthor = authorFlag,
            avatarResId = remoteResourceMapper.avatarBackgroundForColorHex(avatarColorHex),
            imageUri = imageUrl,
            replies = replies.map { it.toUiModel() }.toMutableList()
        )
    }

    private fun RemoteCommentDto.toReplyUiModel(): NoteReplyUiModel {
        return NoteReplyUiModel(
            id = id,
            author = author,
            content = content,
            city = city,
            timestamp = parseInstantMillis(createdAt) ?: System.currentTimeMillis(),
            likeCount = likeCount,
            isLiked = liked,
            isAuthor = authorFlag,
            avatarResId = remoteResourceMapper.avatarBackgroundForColorHex(avatarColorHex),
            imageUri = imageUrl
        )
    }

    private fun RemoteReplyDto.toUiModel(): NoteReplyUiModel {
        return NoteReplyUiModel(
            id = id,
            author = author,
            content = content,
            city = city,
            timestamp = parseInstantMillis(createdAt) ?: System.currentTimeMillis(),
            likeCount = likeCount,
            isLiked = liked,
            isAuthor = authorFlag,
            avatarResId = remoteResourceMapper.avatarBackgroundForColorHex(avatarColorHex),
            imageUri = imageUrl,
            replyToName = replyToName
        )
    }

    companion object {
        private const val TAG = "NoteDetailActivity"
        private const val EXTRA_NOTE_ID = "extra_note_id"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_AUTHOR = "extra_author"
        private const val EXTRA_LIKE_COUNT = "extra_like_count"
        private const val EXTRA_DESCRIPTION = "extra_description"
        private const val EXTRA_MEDIA_TYPE = "extra_media_type"
        private const val EXTRA_IMAGE_URLS = "extra_image_urls"
        private const val EXTRA_IMAGE_URL = "extra_image_url"
        private const val EXTRA_VIDEO_URL = "extra_video_url"
        private const val EXTRA_COVER_URL = "extra_cover_url"

        private const val PREFS_PLAYBACK = "note_video_playback"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
        private const val KEY_MUTED = "muted"
        private const val STATE_FULLSCREEN = "state_fullscreen"
        private const val DEFAULT_GESTURE_BRIGHTNESS = 0.5f
        private val publishTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

        fun createIntent(context: Context, item: HomeCardItem): Intent {
            return Intent(context, NoteDetailActivity::class.java).apply {
                putExtra(EXTRA_NOTE_ID, item.id)
                putExtra(EXTRA_TITLE, item.title)
                putExtra(EXTRA_AUTHOR, item.author)
                putExtra(EXTRA_LIKE_COUNT, item.likeCount)
                putExtra(EXTRA_DESCRIPTION, item.coverLabel)
                putExtra(EXTRA_MEDIA_TYPE, item.mediaType.name)
                putStringArrayListExtra(EXTRA_IMAGE_URLS, ArrayList(item.imageUrls))
                putExtra(EXTRA_IMAGE_URL, item.imageUrl)
                putExtra(EXTRA_VIDEO_URL, item.videoUrl)
                putExtra(EXTRA_COVER_URL, item.videoCoverUrl ?: item.imageUrl)
            }
        }
    }
}

private enum class CommentSortMode {
    DEFAULT,
    LATEST,
    MOST_LIKED
}

private fun CommentSortMode.backendValue(): String {
    return when (this) {
        CommentSortMode.DEFAULT -> "default"
        CommentSortMode.LATEST -> "latest"
        CommentSortMode.MOST_LIKED -> "most_liked"
    }
}

private data class CommentDialogState(
    val dialog: AlertDialog,
    val input: EditText,
    val imageButton: TextView,
    val imagePreviewContainer: FrameLayout,
    val imagePreview: ImageView,
    val removeImageButton: ImageView,
    val parentComment: NoteCommentUiModel?,
    val replyToAuthor: String?,
    var selectedImageUri: String?
)

private data class NoteCommentUiModel(
    val id: String,
    val author: String,
    val content: String,
    val city: String,
    val timestamp: Long,
    var likeCount: Int,
    var isLiked: Boolean = false,
    val isAuthor: Boolean = false,
    val avatarResId: Int,
    val imageUri: String? = null,
    val replies: MutableList<NoteReplyUiModel> = mutableListOf(),
    var isReplyExpanded: Boolean = false
)

private data class NoteReplyUiModel(
    val id: String = "",
    val author: String,
    val content: String,
    val city: String,
    val timestamp: Long,
    var likeCount: Int,
    var isLiked: Boolean = false,
    val isAuthor: Boolean,
    val avatarResId: Int,
    val imageUri: String? = null,
    val replyToName: String? = null
)

private class NoteCommentAdapter(
    private val onLikeClick: (NoteCommentUiModel) -> Unit,
    private val onReplyLikeClick: (NoteReplyUiModel) -> Unit,
    private val onReplyClick: (NoteCommentUiModel, String?) -> Unit,
    private val onReplyToggleClick: (NoteCommentUiModel) -> Unit
) : RecyclerView.Adapter<NoteCommentAdapter.NoteCommentViewHolder>() {

    private val items = mutableListOf<NoteCommentUiModel>()

    fun submitList(comments: List<NoteCommentUiModel>) {
        items.clear()
        items.addAll(comments)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteCommentViewHolder {
        val binding = ItemNoteCommentBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteCommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteCommentViewHolder, position: Int) {
        holder.bind(
            comment = items[position],
            onLikeClick = onLikeClick,
            onReplyLikeClick = onReplyLikeClick,
            onReplyClick = onReplyClick,
            onReplyToggleClick = onReplyToggleClick
        )
    }

    override fun getItemCount(): Int = items.size

    class NoteCommentViewHolder(
        private val binding: ItemNoteCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            comment: NoteCommentUiModel,
            onLikeClick: (NoteCommentUiModel) -> Unit,
            onReplyLikeClick: (NoteReplyUiModel) -> Unit,
            onReplyClick: (NoteCommentUiModel, String?) -> Unit,
            onReplyToggleClick: (NoteCommentUiModel) -> Unit
        ) {
            val context = binding.root.context
            binding.commentAvatar.text = comment.author.take(1)
            binding.commentAvatar.background = AppCompatResources.getDrawable(context, comment.avatarResId)
            binding.commentAuthor.text = comment.author
            binding.commentAuthorBadge.visibility = if (comment.isAuthor) View.VISIBLE else View.GONE
            binding.commentContent.text = comment.content
            binding.commentContent.visibility = if (comment.content.isBlank()) View.GONE else View.VISIBLE
            if (comment.imageUri.isNullOrBlank()) {
                binding.commentImage.visibility = View.GONE
                binding.commentImage.setImageDrawable(null)
            } else {
                binding.commentImage.visibility = View.VISIBLE
                binding.commentImage.load(comment.imageUri)
            }
            binding.commentMeta.text = "${relativeTimeLabel(comment.timestamp)}  ${comment.city}"
            binding.commentLikeCount.text = comment.likeCount.toString()
            val likeColor = if (comment.isLiked) {
                ContextCompat.getColor(context, R.color.xhs_accent)
            } else {
                ContextCompat.getColor(context, R.color.xhs_text_muted)
            }
            binding.commentLikeIcon.setColorFilter(likeColor)
            binding.commentLikeCount.setTextColor(likeColor)
            binding.commentEmotionAction.alpha = 0.72f
            binding.commentLikeAction.setOnClickListener { onLikeClick(comment) }
            binding.commentReplyAction.setOnClickListener { onReplyClick(comment, null) }
            bindReplies(comment, onReplyLikeClick, onReplyClick, onReplyToggleClick)
        }

        private fun bindReplies(
            comment: NoteCommentUiModel,
            onReplyLikeClick: (NoteReplyUiModel) -> Unit,
            onReplyClick: (NoteCommentUiModel, String?) -> Unit,
            onReplyToggleClick: (NoteCommentUiModel) -> Unit
        ) {
            val context = binding.root.context
            binding.commentReplyContainer.removeAllViews()
            if (comment.replies.isEmpty()) {
                binding.commentReplyContainer.visibility = View.GONE
                binding.commentReplyToggle.visibility = View.GONE
                return
            }

            binding.commentReplyContainer.visibility = View.VISIBLE
            val visibleReplies = if (comment.isReplyExpanded) {
                comment.replies
            } else {
                comment.replies.take(1)
            }
            visibleReplies.forEachIndexed { index, reply ->
                val replyBinding = ItemNoteCommentReplyBinding.inflate(
                    LayoutInflater.from(context),
                    binding.commentReplyContainer,
                    false
                )
                replyBinding.replyAvatar.text = reply.author.take(1)
                replyBinding.replyAvatar.background =
                    AppCompatResources.getDrawable(context, reply.avatarResId)
                replyBinding.replyAuthor.text = reply.author
                replyBinding.replyAuthorBadge.visibility = if (reply.isAuthor) View.VISIBLE else View.GONE
                replyBinding.replyContent.text = reply.content
                replyBinding.replyContent.visibility = if (reply.content.isBlank()) View.GONE else View.VISIBLE
                if (reply.imageUri.isNullOrBlank()) {
                    replyBinding.replyImage.visibility = View.GONE
                    replyBinding.replyImage.setImageDrawable(null)
                } else {
                    replyBinding.replyImage.visibility = View.VISIBLE
                    replyBinding.replyImage.load(reply.imageUri)
                }
                replyBinding.replyMeta.text = "${relativeTimeLabel(reply.timestamp)}  ${reply.city}"
                updateReplyLikeState(replyBinding, reply)
                replyBinding.replyAction.setOnClickListener { onReplyClick(comment, reply.author) }
                replyBinding.replyLikeAction.setOnClickListener { onReplyLikeClick(reply) }
                replyBinding.replyEmotionAction.alpha = 0.72f
                if (index > 0) {
                    (replyBinding.root.layoutParams as? LinearLayout.LayoutParams)?.topMargin =
                        dpToPx(context, 8)
                }
                binding.commentReplyContainer.addView(replyBinding.root)
            }

            binding.commentReplyToggle.visibility = if (comment.replies.size > 1) View.VISIBLE else View.GONE
            binding.commentReplyToggle.text = if (comment.isReplyExpanded) {
                "收起回复"
            } else {
                "展开 ${comment.replies.size} 条回复"
            }
            binding.commentReplyToggle.setOnClickListener { onReplyToggleClick(comment) }
        }

        private fun updateReplyLikeState(
            replyBinding: ItemNoteCommentReplyBinding,
            reply: NoteReplyUiModel
        ) {
            val context = replyBinding.root.context
            val likeColor = if (reply.isLiked) {
                ContextCompat.getColor(context, R.color.xhs_accent)
            } else {
                ContextCompat.getColor(context, R.color.xhs_text_muted)
            }
            replyBinding.replyLikeCount.text = reply.likeCount.toString()
            replyBinding.replyLikeIcon.setColorFilter(likeColor)
            replyBinding.replyLikeCount.setTextColor(likeColor)
        }
    }
}

private fun relativeTimeLabel(timestamp: Long): String {
    val diffMinutes = ((System.currentTimeMillis() - timestamp) / 60_000L).coerceAtLeast(0L)
    return when {
        diffMinutes < 1L -> "刚刚"
        diffMinutes < 60L -> "${diffMinutes}分钟前"
        diffMinutes < 24L * 60L -> "${diffMinutes / 60L}小时前"
        else -> "${diffMinutes / (24L * 60L)}天前"
    }
}

private fun dpToPx(context: Context, valueDp: Int): Int {
    return (valueDp * context.resources.displayMetrics.density).roundToInt()
}

private class NoteImagePagerAdapter : RecyclerView.Adapter<NoteImagePagerAdapter.NoteImageViewHolder>() {

    private val items = mutableListOf<String>()
    var onImageTap: (() -> Unit)? = null
    var isFullscreen: Boolean = false

    fun submitList(imageUrls: List<String>) {
        items.clear()
        items.addAll(imageUrls)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteImageViewHolder {
        val binding = ItemNoteDetailImageBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteImageViewHolder, position: Int) {
        holder.bind(
            imageUrl = items[position],
            isFullscreen = isFullscreen,
            onImageTap = onImageTap
        )
    }

    override fun getItemCount(): Int = items.size

    class NoteImageViewHolder(
        private val binding: ItemNoteDetailImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            imageUrl: String,
            isFullscreen: Boolean,
            onImageTap: (() -> Unit)?
        ) {
            binding.root.setBackgroundColor(if (isFullscreen) Color.BLACK else Color.TRANSPARENT)
            binding.detailPagerImage.scaleType = if (isFullscreen) {
                ImageView.ScaleType.FIT_CENTER
            } else {
                ImageView.ScaleType.CENTER_CROP
            }
            binding.detailPagerImage.load(imageUrl) {
                crossfade(true)
            }
            binding.detailPagerImage.setOnClickListener { onImageTap?.invoke() }
        }
    }
}
