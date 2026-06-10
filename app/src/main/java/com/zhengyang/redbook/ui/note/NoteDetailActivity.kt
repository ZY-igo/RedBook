/**
 * 文件说明：NoteDetailActivity.kt
 * 作用：承载笔记详情页的界面初始化、媒体播放、评论互动与状态呈现逻辑。
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
import android.os.Build
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
import android.view.WindowManager
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
import com.zhengyang.redbook.data.remote.RemoteApiConfig
import com.zhengyang.redbook.data.remote.RemoteResourceMapper
import com.zhengyang.redbook.data.remote.model.RemoteCommentDto
import com.zhengyang.redbook.data.remote.model.RemoteNoteDetailDto
import com.zhengyang.redbook.data.remote.model.RemoteReplyDto
import com.zhengyang.redbook.data.repository.MyRepository
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
import com.zhengyang.redbook.ui.my.MyProfileHeader
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * 笔记详情页活动类
 *
 * 负责承载图文详情与视频详情两套展示模式，统一处理媒体播放、评论互动、网络状态监听、
 * 横竖屏切换以及详情页各类按钮交互。
 */
@AndroidEntryPoint
class NoteDetailActivity : AppCompatActivity() {

    /** 图文详情布局绑定对象，仅在图文模式下初始化。 */
    private var imageBinding: ActivityNoteDetailImageBinding? = null
    /** 视频详情布局绑定对象，仅在视频模式下初始化。 */
    private var videoBinding: ActivityNoteDetailVideoBinding? = null
    /** 播放器监听器实例。 */
    private var playerListener: Player.Listener? = null
    /** 缓存的 ExoPlayer 实例。 */
    /** 播放进度轮询任务。 */
    private var progressUpdater: Runnable? = null
    /** 手势提示自动隐藏任务。 */
    private var gestureHideRunnable: Runnable? = null
    /** 全屏控制层自动隐藏任务。 */
    private var fullscreenControlsHideRunnable: Runnable? = null
    /** 网络状态回调。 */
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    /** 当前是否处于全屏模式。 */
    private var isFullscreen = false
    /** 全屏控制层是否可见。 */
    private var areFullscreenControlsVisible = true
    /** 当前是否正在拖动进度条。 */
    private var isSeeking = false
    /** 待执行的 seek 位置。 */
    private var pendingSeekPositionMs = C.TIME_UNSET
    /** 最近一次视频宽度。 */
    private var lastVideoWidth = 16
    /** 最近一次视频高度。 */
    private var lastVideoHeight = 9
    /** 是否已展示过移动网络提示。 */
    private var hasShownMeteredNetworkHint = false
    /** 是否已经渲染出首帧画面。 */
    private var hasRenderedFirstFrame = false
    /** 是否存在待恢复的网络播放任务。 */
    private var pendingNetworkRecovery = false
    /** 网络恢复后是否继续自动播放。 */
    private var shouldResumeAfterNetworkRecovery = false
    /** 页面重新回到前台后是否需要继续播放，用于 onStop/onStart 之间的短暂切换恢复。 */
    private var shouldResumeOnStart = false
    /** 当前已经完成 prepare 的视频地址，用于避免同一资源被重复 prepare。 */
    private var preparedVideoUrl: String? = null
    /** 手势快进时的基准播放位置。 */
    private var gestureSeekBasePositionMs = 0L
    /** 手势调节音量时的基准音量。 */
    private var gestureVolumeBase = 0
    /** 手势调节亮度时的基准亮度。 */
    private var gestureBrightnessBase = DEFAULT_GESTURE_BRIGHTNESS

    /** 图文模式图片分页适配器。 */
    private val imagePagerAdapter = NoteImagePagerAdapter()
    /** 评论列表适配器。 */
    private val commentAdapter = NoteCommentAdapter(
        onLikeClick = ::toggleCommentLike,
        onReplyLikeClick = ::toggleReplyLike,
        onReplyClick = { comment, replyTo -> showCommentDialog(parentComment = comment, replyToAuthor = replyTo) },
        onReplyToggleClick = ::toggleCommentReplies
    )
    /** 图片分页吸附辅助器。 */
    private val imagePagerSnapHelper = LinearSnapHelper()
    /** 可选播放倍速列表。 */
    private val playbackSpeeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
    /** 当前评论列表数据。 */
    private val commentItems = mutableListOf<NoteCommentUiModel>()
    /** 当前笔记点赞数数值。 */
    private var noteLikeCountValue = 0
    /** 当前笔记收藏数数值。 */
    private var noteCollectCountValue = 0
    /** 当前是否已关注作者。 */
    private var isFollowingAuthor = false
    /** 当前是否已点赞笔记。 */
    private var isNoteLiked = false
    /** 当前是否已收藏笔记。 */
    private var isNoteCollected = false
    /** 当前评论排序方式。 */
    private var commentSortMode = CommentSortMode.DEFAULT
    /** 当前显示中的评论输入对话框。 */
    private var activeCommentDialog: AlertDialog? = null
    /** 当前评论输入对话框状态。 */
    private var activeCommentDialogState: CommentDialogState? = null
    /** 当前笔记 ID。 */
    private var currentNoteId: String? = null
    /** 当前作者 ID。 */
    private var currentAuthorId: String? = null
    /** 当前作者名称。 */
    private var currentAuthorName: String = ""
    /** 当前笔记标题。 */
    private var currentNoteTitle: String = ""
    /** 当前视频播放地址。 */
    private var currentVideoUrl: String = ""
    /** 当前封面地址。 */
    private var currentCoverUrl: String = ""
    /** 当前图片地址列表。 */
    private var currentImageUrls: List<String> = emptyList()
    /** 评论图片选择器。 */
    private val commentImagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            handlePickedCommentImage(uri)
        }

    @Inject
    /** 笔记详情数据仓库。 */
    lateinit var noteRepository: NoteRepository

    @Inject
    lateinit var myRepository: MyRepository

    @Inject
    /** 远端资源键映射器。 */
    lateinit var remoteResourceMapper: RemoteResourceMapper

    @Inject
    lateinit var remoteApiConfig: RemoteApiConfig

    @Inject
    lateinit var exoPlayer: ExoPlayer

    /** 当前详情页是否为视频模式。 */
    private val isVideo: Boolean by lazy {
        intent.getStringExtra(EXTRA_MEDIA_TYPE) == HomeCardItem.MediaType.VIDEO.name
    }

    /** 播放设置持久化配置。 */
    private val playbackPrefs by lazy {
        getSharedPreferences(PREFS_PLAYBACK, Context.MODE_PRIVATE)
    }

    /** 系统音频管理器。 */
    private val audioManager by lazy {
        getSystemService(AudioManager::class.java)
    }

    /**
     * 初始化详情页。
     *
     * @param savedInstanceState 系统恢复时传入的页面状态快照。
     */
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
            loadCommentComposerAvatar()
        }
        loadRemoteNote()
    }

    /**
     * 页面进入前台时初始化视频相关能力。
     */
    override fun onStart() {
        super.onStart()
        if (isVideo) {
            registerNetworkCallbackIfNeeded()
            setupPlayerIfNeeded()
        }
    }

    /**
     * 页面离开前台时暂停视频相关能力并保存进度。
     */
    override fun onStop() {
        if (isVideo) {
            stopProgressUpdates()
            gestureHideRunnable?.let { runnable ->
                videoBinding?.gestureHintText?.removeCallbacks(runnable)
            }
            gestureHideRunnable = null
            fullscreenControlsHideRunnable?.let { runnable ->
                videoBinding?.fullscreenControlsOverlay?.removeCallbacks(runnable)
            }
            fullscreenControlsHideRunnable = null
            shouldResumeOnStart = exoPlayer.isPlaying
            if (preparedVideoUrl != null) {
                exoPlayer.pause()
                savePlaybackProgress(
                    videoUrl = currentVideoUrl,
                    positionMs = exoPlayer.currentPosition,
                    durationMs = exoPlayer.duration
                )
                shouldResumeAfterNetworkRecovery = false
            }
            videoBinding?.detailVideoView?.onPause()
            videoBinding?.detailVideoView?.player = null
            unregisterNetworkCallback()
        }
        super.onStop()
    }

    /**
     * 保存当前页面关键状态。
     *
     * @param outState 用于保存状态的 Bundle。
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_FULLSCREEN, isFullscreen)
    }

    /**
     * 处理系统返回键逻辑。
     */
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

    /**
     * 处理配置变更后的界面适配。
     *
     * @param newConfig 最新配置对象。
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isVideo) return

        videoBinding?.root?.post {
            applyVideoModeUi()
            configureVideoLayout(lastVideoWidth, lastVideoHeight)
        }
    }

    /**
     * 分发按键事件并同步系统音量状态。
     *
     * @param event 当前按键事件。
     * @return 是否已消费该事件。
     */
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

    /**
     * 销毁页面并释放长生命周期资源。
     */
    override fun onDestroy() {
        if (isVideo) {
            unregisterNetworkCallback()
            releasePlayerIfNeeded()
        }
        imageBinding = null
        videoBinding = null
        super.onDestroy()
    }

    /**
     * 初始化图文详情布局。
     */
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

    /**
     * 初始化视频详情布局。
     */
    private fun setupVideoLayout() {
        videoBinding = ActivityNoteDetailVideoBinding.inflate(layoutInflater)
        setContentView(requireVideoBinding().root)
        applyInsets(requireVideoBinding().topBar, Color.BLACK, useLightSystemBars = false)
        bindVideoInsets()
        bindVideoHeader()
        bindVideoContent()
        bindVideoEvents()
        bindVideoActions()
        applyVideoModeUi()
        updateSpeedButton()
        requireVideoBinding().root.post { refreshVideoActionLabels() }
    }

    /**
     * 适配顶部栏系统窗口插入并更新系统栏颜色。
     *
     * @param topBar 需要处理 inset 的顶部栏视图。
     * @param backgroundColor 页面背景色。
     * @param useLightSystemBars 是否使用浅色系统栏图标。
     */
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

    private fun bindVideoInsets() {
        val binding = requireVideoBinding()
        val overlay = binding.fullscreenControlsOverlay
        val initialTopPadding = overlay.paddingTop
        val initialBottomPadding = overlay.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(overlay) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(
                view.paddingLeft,
                initialTopPadding + statusBars.top,
                view.paddingRight,
                initialBottomPadding + navigationBars.bottom
            )
            insets
        }
    }

    /**
     * 绑定图文详情头部交互。
     */
    private fun bindImageHeader() {
        val binding = requireImageBinding()
        binding.buttonBack.setOnClickListener {
            if (isFullscreen) setImageFullscreen(false) else finish()
        }
        binding.buttonShare.setOnClickListener {
            Toast.makeText(this, "分享面板待接入，先保留交互入口", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 绑定视频详情头部交互。
     */
    private fun bindVideoHeader() {
        val binding = requireVideoBinding()
        binding.buttonBack.setOnClickListener {
            if (isFullscreen) setFullscreen(false) else finish()
        }
        binding.buttonVideoAux.setOnClickListener { }
        binding.buttonAction.setOnClickListener { }
        binding.buttonShare.setOnClickListener { }
        binding.fullscreenBackButton.setOnClickListener { setFullscreen(false) }
        binding.fullscreenExitButton.setOnClickListener { setFullscreen(false) }
    }

    /**
     * 绑定图文详情主体内容。
     */
    private fun bindImageContent() {
        val binding = requireImageBinding()
        val author = intent.getStringExtra(EXTRA_AUTHOR).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
        val coverUrl = currentCoverUrl

        noteLikeCountValue = intent.getStringExtra(EXTRA_LIKE_COUNT)?.toIntOrNull() ?: 0
        noteCollectCountValue = 0
        binding.topAuthorName.text = author
        binding.topAvatarText.text = author.take(1)
        binding.noteTitle.text = title
        binding.noteDescription.text = description
        binding.relatedText.text = title
        binding.publishTimeText.text = ""
        binding.locationText.text = ""
        binding.commentInput.text = getString(R.string.note_detail_comment_hint)
        bindImagePager(currentImageUrls.ifEmpty { collectImageUrls(fallbackUrl = coverUrl) })
        bindImageActions(author = author, title = title)
        commentItems.clear()
        refreshImageActionState()
        refreshCommentSection()
    }

    /**
     * 绑定视频详情主体内容。
     */
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
        binding.collectCount.text = "0"
        binding.commentCount.text = "0"
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

    /**
     * 绑定图文详情操作区交互。
     *
     * @param author 当前作者名称。
     * @param title 当前笔记标题。
     */
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

    /**
     * 绑定视频详情操作区交互。
     */
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

    /**
     * 加载远端笔记详情数据。
     *
     * 这里先请求详情，再基于详情中的真实笔记 ID、作者信息和媒体信息回填页面。
     * 请求成功后会继续串行拉取评论，保证评论排序参数与当前详情页状态保持一致。
     */
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
                        error.message ?: "绗旇璇︽儏鍔犺浇澶辫触",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    /**
     * 加载远端评论列表。
     */
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
                refreshCommentSectionCompat()
            }.onFailure { error ->
                Toast.makeText(
                    this@NoteDetailActivity,
                    error.message ?: "璇勮鍔犺浇澶辫触",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * 将远端详情数据应用到当前页面。
     *
     * @param detail 远端返回的笔记详情 DTO。
     */
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
            setupPlayerIfNeeded(forcePrepare = true)
        } else {
            val binding = requireImageBinding()
            binding.topAuthorName.text = detail.author.name
            binding.topAvatarText.text = detail.author.avatarText.ifBlank { detail.author.name.take(1) }
            binding.noteTitle.text = detail.title
            binding.noteDescription.text = detail.description
            binding.relatedText.text = detail.title
            binding.publishTimeText.text = formatPublishTime(detail.createdAt)
            binding.locationText.text = detail.author.location.orEmpty()
            bindImagePager(currentImageUrls.ifEmpty { collectImageUrls(fallbackUrl = currentCoverUrl) })
            refreshImageActionState()
            binding.commentCount.text = detail.commentCount.toString()
            binding.commentSectionTitle.text = "共 ${detail.commentCount} 条评论"
        }
    }

    /**
     * 切换作者关注状态。
     *
     * @param authorName 当前作者名称，用于提示文案展示。
     */
    private fun loadCommentComposerAvatar() {
        lifecycleScope.launch {
            runCatching { myRepository.getProfile() }
                .onSuccess(::bindCommentComposerAvatar)
        }
    }

    private fun bindCommentComposerAvatar(profile: MyProfileHeader) {
        val binding = imageBinding ?: return
        val resolvedAvatarUrl = resolveRemoteUrl(profile.avatarUrl)
        if (resolvedAvatarUrl.isNullOrBlank()) {
            binding.avatar.scaleType = ImageView.ScaleType.CENTER_INSIDE
            binding.avatar.setImageResource(R.drawable.ic_xhs_profile)
            binding.avatar.background = AppCompatResources.getDrawable(this, R.drawable.bg_xhs_avatar_dog)
            binding.avatar.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
            return
        }
        binding.avatar.clearColorFilter()
        binding.avatar.background = null
        binding.avatar.scaleType = ImageView.ScaleType.CENTER_CROP
        binding.avatar.load(resolvedAvatarUrl) {
            crossfade(true)
            listener(
                onError = { _, _ ->
                    binding.avatar.scaleType = ImageView.ScaleType.CENTER_INSIDE
                    binding.avatar.setImageResource(R.drawable.ic_xhs_profile)
                    binding.avatar.background = AppCompatResources.getDrawable(
                        this@NoteDetailActivity,
                        R.drawable.bg_xhs_avatar_dog
                    )
                    binding.avatar.setColorFilter(
                        ContextCompat.getColor(this@NoteDetailActivity, android.R.color.white)
                    )
                }
            )
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
                        error.message ?: "鍏虫敞鎿嶄綔澶辫触",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    /**
     * 刷新图文模式下的互动按钮状态。
     */
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

    /**
     * 刷新视频模式下的互动按钮状态。
     *
     * @param commentCountOverride 可选的评论数覆盖值。
     */
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

    /**
     * 根据当前模式刷新互动区状态。
     */
    private fun refreshActionState() {
        if (isVideo) refreshVideoActionState() else refreshImageActionState()
    }

    /**
     * 更新图文模式下的关注按钮文案与样式。
     */
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

    /**
     * 更新视频模式下的关注按钮文案与样式。
     */
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

    /**
     * 切换笔记点赞状态。
     */
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
                        error.message ?: "鐐硅禐澶辫触",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    /**
     * 切换笔记收藏状态。
     */
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
                        error.message ?: "鏀惰棌澶辫触",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    /**
     * 刷新评论区列表与标题状态。
     */
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

    /**
     * 兼容刷新评论区。
     *
     * 图文模式拥有完整评论列表区域，需要刷新标题、数量和列表；
     * 视频模式评论入口只展示互动计数，因此这里只刷新排序后的适配器数据和评论数徽标。
     */
    private fun refreshCommentSectionCompat() {
        if (isVideo) {
            val comments = when (commentSortMode) {
                CommentSortMode.DEFAULT -> commentItems.sortedWith(
                    compareByDescending<NoteCommentUiModel> { it.likeCount }
                        .thenByDescending { it.timestamp }
                )
                CommentSortMode.LATEST -> commentItems.sortedByDescending { it.timestamp }
                CommentSortMode.MOST_LIKED -> commentItems.sortedByDescending { it.likeCount }
            }
            commentAdapter.submitList(comments)
            refreshVideoActionState(commentCountOverride = commentItems.size)
            return
        }
        refreshCommentSection()
    }

    /**
     * 展示评论排序弹窗。
     *
     * 弹窗只负责切换前端当前选择的排序模式，真正的数据刷新仍然通过重新请求评论完成，
     * 这样可以保证前后端排序结果一致，也避免本地排序与服务端分页策略不一致。
     *
     * @param anchor 弹窗锚点视图。
     */
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

    /**
     * 切换评论点赞状态。
     *
     * @param comment 当前评论模型。
     */
    private fun toggleCommentLike(comment: NoteCommentUiModel) {
        val targetValue = !comment.isLiked
        comment.isLiked = targetValue
        comment.likeCount = (comment.likeCount + if (targetValue) 1 else -1).coerceAtLeast(0)
        refreshCommentSectionCompat()
        lifecycleScope.launch {
            runCatching { noteRepository.toggleCommentLike(comment.id, targetValue) }
                .onFailure { error ->
                    comment.isLiked = !targetValue
                    comment.likeCount = (comment.likeCount + if (targetValue) -1 else 1).coerceAtLeast(0)
                    refreshCommentSectionCompat()
                    Toast.makeText(
                        this@NoteDetailActivity,
                        error.message ?: "璇勮鐐硅禐澶辫触",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    /**
     * 展开或收起评论回复列表。
     *
     * @param comment 当前评论模型。
     */
    private fun toggleCommentReplies(comment: NoteCommentUiModel) {
        comment.isReplyExpanded = !comment.isReplyExpanded
        refreshCommentSectionCompat()
    }

    /**
     * 切换评论回复点赞状态。
     *
     * @param reply 当前回复模型。
     */
    private fun toggleReplyLike(reply: NoteReplyUiModel) {
        val targetValue = !reply.isLiked
        reply.isLiked = targetValue
        reply.likeCount = (reply.likeCount + if (targetValue) 1 else -1).coerceAtLeast(0)
        refreshCommentSectionCompat()
        lifecycleScope.launch {
            runCatching { noteRepository.toggleCommentLike(reply.id, targetValue) }
                .onFailure { error ->
                    reply.isLiked = !targetValue
                    reply.likeCount = (reply.likeCount + if (targetValue) -1 else 1).coerceAtLeast(0)
                    refreshCommentSectionCompat()
                    Toast.makeText(
                        this@NoteDetailActivity,
                        error.message ?: "鍥炲鐐硅禐澶辫触",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    /**
     * 展示评论输入对话框。
     *
     * @param parentComment 当前回复所属父评论；为空表示发布一级评论。
     * @param replyToAuthor 当前回复目标作者名称；为空表示普通评论。
     * @param selectedImageUri 当前预选中的图片地址。
     */
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

    /**
     * 提交评论或回复。
     *
     * @param content 评论文本内容。
     * @param parentComment 当前回复所属父评论；为空表示一级评论。
     * @param imageUri 评论附图地址。
     */
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
                refreshCommentSectionCompat()
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
    }

    /**
     * 打开评论图片选择器。
     */
    private fun openCommentImagePicker() {
        commentImagePicker.launch("image/*")
    }

    /**
     * 处理用户选择的评论图片。
     *
     * @param uri 当前选择结果。
     */
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

    /**
     * 渲染评论对话框中的图片选择状态。
     *
     * @param state 当前评论对话框状态。
     */
    private fun renderCommentDialogImageState(state: CommentDialogState) {
        val hasImage = !state.selectedImageUri.isNullOrBlank()
        state.imageButton.text = if (hasImage) "鏇存崲鍥剧墖" else "娣诲姞鍥剧墖"
        state.imagePreviewContainer.visibility = if (hasImage) View.VISIBLE else View.GONE
        if (hasImage) {
            state.imagePreview.load(state.selectedImageUri)
        } else {
            state.imagePreview.setImageDrawable(null)
        }
    }

    /**
     * 将页面滚动到评论区附近。
     */
    private fun scrollToCommentSection() {
        val binding = requireImageBinding()
        binding.scrollContainer.post {
            binding.scrollContainer.smoothScrollTo(0, binding.commentSectionTitle.top)
        }
    }

    /**
     * 绑定图文详情图片分页器。
     *
     * @param imageUrls 当前图片地址列表。
     */
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

    /**
     * 根据当前图片比例更新分页器高度。
     */
    private fun updatePagerHeight() {
        val binding = requireImageBinding()
        val firstChild = binding.detailImagePager.getChildAt(0) ?: return
        val targetHeight = firstChild.measuredHeight.takeIf { it > 0 } ?: return
        binding.detailImagePager.layoutParams = binding.detailImagePager.layoutParams.apply {
            height = targetHeight
        }
    }

    /**
     * 更新图片分页指示器。
     *
     * @param selectedIndex 当前选中页索引。
     */
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

    /**
     * 获取当前图片分页位置。
     *
     * @return 当前可见图片索引。
     */
    private fun currentImagePosition(): Int {
        val binding = requireImageBinding()
        val layoutManager = binding.detailImagePager.layoutManager ?: return 0
        val snapView = imagePagerSnapHelper.findSnapView(layoutManager) ?: return 0
        return layoutManager.getPosition(snapView).coerceAtLeast(0)
    }

    /**
     * 切换图文详情全屏模式。
     *
     * @param enabled 是否启用全屏模式。
     */
    private fun setImageFullscreen(enabled: Boolean) {
        if (isVideo || isFullscreen == enabled) return
        isFullscreen = enabled
        applyImageModeUi()
        imagePagerAdapter.isFullscreen = enabled
        imagePagerAdapter.notifyDataSetChanged()
        requireImageBinding().detailImagePager.post { updatePagerHeight() }
    }

    /**
     * 应用图文模式下的全屏与普通态界面配置。
     */
    private fun applyImageModeUi() {
        val binding = imageBinding ?: return
        applyFullscreenCutoutMode(isFullscreen)
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

    /**
     * 收集图文详情可用的图片地址列表。
     *
     * @param fallbackUrl 缺省图片地址。
     * @return 去重和兜底后的图片地址列表。
     */
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

    /**
     * 按需初始化播放器并绑定到当前页面。
     *
     * 该方法既承担首次进入页面时的播放器准备，也承担远端详情回填后的视频地址切换处理。
     * 当视频地址未变化时，只做 View 重新绑定与 UI 恢复；当地址变化或要求强制重建时，
     * 才重新 prepare 媒体源并恢复本地缓存的播放进度与倍速、静音等偏好。
     *
     * @param forcePrepare 是否忽略当前缓存状态，强制重新 prepare 媒体源。
     */
    private fun setupPlayerIfNeeded(forcePrepare: Boolean = false) {
        val videoUrl = currentVideoUrl
        if (videoUrl.isBlank()) return

        val binding = requireVideoBinding()
        val needsPrepare = forcePrepare || preparedVideoUrl != videoUrl
        if (needsPrepare) {
            exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            exoPlayer.playWhenReady = false
            exoPlayer.volume = if (isMuted()) 0f else 1f
            exoPlayer.playbackParameters = PlaybackParameters(getSavedPlaybackSpeed())
            attachPlayerListener(exoPlayer, videoUrl)
            hasRenderedFirstFrame = false

            MediaPlayerFactory.prepare(
                player = exoPlayer,
                mediaItem = MediaItemFactory.guessVideo(videoUrl),
                playWhenReady = false
            )

            val savedPosition = getSavedPlaybackProgress(videoUrl)
            pendingSeekPositionMs = savedPosition.takeIf { it > 0L } ?: C.TIME_UNSET
            if (savedPosition > 0L) {
                exoPlayer.seekTo(savedPosition)
            }
            preparedVideoUrl = videoUrl
        }

        binding.detailVideoView.player = exoPlayer
        binding.detailVideoView.onResume()
        exoPlayer.volume = if (isMuted()) 0f else 1f
        exoPlayer.playbackParameters = PlaybackParameters(getSavedPlaybackSpeed())
        if (shouldResumeOnStart && hasActiveNetwork()) {
            binding.errorContainer.visibility = View.GONE
            exoPlayer.play()
        }
        shouldResumeOnStart = false
        updatePlaybackUi(exoPlayer)
        updateProgressUi(exoPlayer)
        binding.root.post { refreshVideoActionLabels() }
        updateSpeedButton()
    }

    /**
     * 释放播放器及其关联状态。
     *
     * 释放前会先落盘当前进度，随后解除监听、解绑 PlayerView 并清理所有与本次播放相关的
     * 瞬时状态，避免 Activity 销毁后残留自动恢复、待 seek 或网络恢复等旧状态。
     */
    private fun releasePlayerIfNeeded() {
        if (preparedVideoUrl != null) {
            savePlaybackProgress(
                videoUrl = currentVideoUrl,
                positionMs = exoPlayer.currentPosition,
                durationMs = exoPlayer.duration
            )
        }
        stopPlaybackService()
        playerListener?.let(exoPlayer::removeListener)
        playerListener = null
        videoBinding?.detailVideoView?.player = null
        exoPlayer.release()
        preparedVideoUrl = null
        pendingSeekPositionMs = C.TIME_UNSET
        hasRenderedFirstFrame = false
        pendingNetworkRecovery = false
        shouldResumeAfterNetworkRecovery = false
        shouldResumeOnStart = false
    }

    /**
     * 为当前播放器安装页面级监听器。
     *
     * 监听器负责把 Media3 的底层播放状态翻译成页面可见状态，例如：
     * 1. 缓冲时显示 loading 和状态文案。
     * 2. 就绪时隐藏错误态并执行待恢复的 seek。
     * 3. 播放结束时重置保存进度并恢复封面。
     * 4. 播放异常时根据网络与错误码生成更明确的提示。
     *
     * @param player 当前绑定到详情页的播放器实例。
     * @param videoUrl 当前视频地址，用于在播放结束时写回进度。
     */
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
                        if (isFullscreen) {
                            areFullscreenControlsVisible = true
                            updateFullscreenControlsVisibility()
                        }
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
                if (isFullscreen) {
                    areFullscreenControlsVisible = true
                    updateFullscreenControlsVisibility()
                }
                showPausedCover()
            }
        }
        playerListener = listener
        player.addListener(listener)
    }

    /**
     * 绑定视频页所有显式交互事件。
     *
     * 这里集中处理点击、重试、倍速、静音、全屏与拖动进度条等交互，
     * 并把视频区域和 PlayerView 的触摸都交给统一的手势识别器，避免两层 View 各自消费事件。
     */
    private fun bindVideoEvents() {
        val binding = requireVideoBinding()
        val gestureDetector = createVideoGestureDetector()

        val toggleClickListener = View.OnClickListener { toggleVideoPlayback() }
        binding.playOverlay.setOnClickListener(toggleClickListener)
        binding.videoCover.setOnClickListener {
            if (isFullscreen) {
                toggleFullscreenControls()
            } else {
                toggleVideoPlayback()
            }
        }
        binding.fullscreenButton.setOnClickListener { setFullscreen(!isFullscreen) }
        binding.muteButton.setOnClickListener { toggleMute() }
        binding.speedButton.setOnClickListener { cyclePlaybackSpeed() }
        binding.fullscreenMuteButton.setOnClickListener { toggleMute() }
        binding.fullscreenSpeedButton.setOnClickListener { cyclePlaybackSpeed() }
        binding.retryButton.setOnClickListener { retryPlayback() }

        val videoTouchListener = View.OnTouchListener { _, event ->
            val handled = gestureDetector.onTouchEvent(event)
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                binding.videoHost.parent?.requestDisallowInterceptTouchEvent(false)
            } else {
                binding.videoHost.parent?.requestDisallowInterceptTouchEvent(true)
            }
            handled
        }
        binding.videoHost.setOnTouchListener(videoTouchListener)
        binding.detailVideoView.setOnTouchListener(videoTouchListener)

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

    /**
     * 创建视频手势识别器。
     *
     * 手势策略与常见短视频播放器保持一致：
     * 1. 单击在全屏时切换控制层，在非全屏时切换播放。
     * 2. 双击直接切换播放状态。
     * 3. 横向滑动控制 seek。
     * 4. 左半屏纵向滑动调亮度，右半屏纵向滑动调音量。
     *
     * 方法内部通过 touch slop 和首次位移方向锁定横/纵手势，避免一次滑动同时触发两类调节。
     */
    private fun createVideoGestureDetector(): GestureDetectorCompat {
        val binding = requireVideoBinding()
        val touchSlop = dpToPx(10).toFloat()
        var horizontalScrollConsumed = false
        var verticalScrollConsumed = false

        return GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                horizontalScrollConsumed = false
                verticalScrollConsumed = false
                gestureSeekBasePositionMs = exoPlayer.currentPosition
                gestureBrightnessBase = currentScreenBrightness()
                gestureVolumeBase = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (isFullscreen) {
                    toggleFullscreenControls()
                } else {
                    toggleVideoPlayback()
                }
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
        exoPlayer.volume = if (nextVolume == 0) 0f else 1f
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

    /**
     * 重试当前视频播放。
     *
     * 若当前尚未 prepare，则先走完整初始化流程；否则直接对已有播放器重新 prepare。
     * `autoPlay` 主要用于网络恢复场景，决定恢复后是立即继续播放还是仅恢复到可播放状态。
     */
    private fun retryPlayback(autoPlay: Boolean = true) {
        val binding = requireVideoBinding()
        binding.errorContainer.visibility = View.GONE
        binding.playerStatusText.visibility = View.GONE
        pendingNetworkRecovery = false
        if (preparedVideoUrl == null) {
            setupPlayerIfNeeded()
            if (autoPlay) {
                requireVideoBinding().detailVideoView.player?.play()
            }
            return
        }
        hasRenderedFirstFrame = false
        exoPlayer.prepare()
        if (autoPlay) {
            exoPlayer.play()
        } else {
            updatePlaybackUi(exoPlayer)
        }
    }

    /**
     * 切换视频播放状态。
     *
     * 该方法额外承担两类兜底逻辑：
     * 1. 无网络时直接进入错误态，并记住网络恢复后是否需要续播。
     * 2. 首次在移动网络下播放时给出一次性流量提示。
     */
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
            if (isFullscreen) {
                areFullscreenControlsVisible = true
                updateFullscreenControlsVisibility()
            }
            showPausedCover()
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
            }
            binding.errorContainer.visibility = View.GONE
            player.play()
            if (isFullscreen) {
                areFullscreenControlsVisible = false
                updateFullscreenControlsVisibility()
            }
            updatePlaybackUi(player)
        }
    }

    private fun toggleMute() {
        val muted = !isMuted()
        saveMuteState(muted)
        exoPlayer.volume = if (muted) 0f else 1f
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

    /**
     * 启动进度轮询。
     *
     * ExoPlayer 不会自动把当前时间推送到页面文案，因此这里用轻量轮询驱动
     * 进度条和时间文本刷新；在暂停、离开页面或销毁时必须显式停止，避免 View 泄漏。
     */
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

    /**
     * 根据视频原始尺寸和当前页面模式重新计算视频容器布局。
     *
     * 非全屏模式下会限制视频高度，避免超长竖屏视频把详情信息完全挤出首屏；
     * 全屏模式下则尽量占满容器，并配合 `resolveVideoResizeMode` 在 FIT / ZOOM 之间选择。
     */
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

    /**
     * 切换视频全屏状态。
     *
     * 除了切换系统栏和普通内容区显隐外，这里还会尝试同步调整横竖屏方向，
     * 并确保全屏控制层初始可见，避免用户进入全屏后没有退出入口。
     */
    private fun setFullscreen(enabled: Boolean) {
        if (!isVideo || isFullscreen == enabled) return
        isFullscreen = enabled
        areFullscreenControlsVisible = enabled
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
        binding.videoProgressRow.visibility = if (isFullscreen) View.GONE else View.VISIBLE
        binding.videoActionRow.visibility = if (isFullscreen) View.GONE else View.VISIBLE
        updateFullscreenControlsVisibility()
        binding.root.post { refreshVideoActionLabels() }
        binding.fullscreenButton.text = if (isFullscreen) "退出全屏" else "全屏"
    }

    private fun updateMuteButton() {
        videoBinding?.muteButton?.text = if (isMuted()) "鍙栨秷闈欓煶" else "闈欓煶"
    }

    private fun updateSpeedButton() {
        val speedLabel = "${getSavedPlaybackSpeed()}x"
        videoBinding?.speedButton?.text = speedLabel
        videoBinding?.fullscreenSpeedButton?.text = speedLabel
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
        binding.fullscreenMuteButton.text = binding.muteButton.text
        binding.fullscreenSpeedButton.text = "${getSavedPlaybackSpeed()}x"
        binding.fullscreenExitButton.text = getString(R.string.note_detail_exit_fullscreen)
    }

    private fun toggleFullscreenControls() {
        if (!isFullscreen) return
        areFullscreenControlsVisible = !areFullscreenControlsVisible
        updateFullscreenControlsVisibility()
    }

    /**
     * 更新全屏控制层显隐，并在显示后启动自动隐藏计时。
     *
     * 控制层只有在视频全屏时才有意义；每次重新显示都会重置自动隐藏任务，
     * 防止上一次计时器把本次刚展示出来的控制层立即隐藏。
     */
    private fun updateFullscreenControlsVisibility() {
        val binding = videoBinding ?: return
        val shouldShow = isFullscreen && areFullscreenControlsVisible
        binding.fullscreenControlsOverlay.visibility = if (shouldShow) View.VISIBLE else View.GONE
        fullscreenControlsHideRunnable?.let(binding.fullscreenControlsOverlay::removeCallbacks)
        fullscreenControlsHideRunnable = null
        if (shouldShow) {
            val runnable = Runnable {
                areFullscreenControlsVisible = false
                binding.fullscreenControlsOverlay.visibility = View.GONE
                fullscreenControlsHideRunnable = null
            }
            fullscreenControlsHideRunnable = runnable
            binding.fullscreenControlsOverlay.postDelayed(runnable, FULLSCREEN_CONTROLS_AUTO_HIDE_MS)
        }
    }

    private fun applyFullscreenCutoutMode(enabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val attributes = window.attributes
        attributes.layoutInDisplayCutoutMode = if (enabled) {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        } else {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        }
        window.attributes = attributes
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

    /**
     * 注册默认网络回调。
     *
     * 回调只服务于视频详情页：断网时暂停播放并展示错误态；重新联网后，
     * 若此前记录过待恢复状态，则按用户上一次意图决定是否自动续播。
     */
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
                        if (preparedVideoUrl != null) {
                            shouldResumeAfterNetworkRecovery = exoPlayer.isPlaying || exoPlayer.playWhenReady
                            pendingNetworkRecovery = true
                            exoPlayer.pause()
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
        exoPlayer.volume = if (currentVolume == 0) 0f else 1f
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
            avatarUrl = resolveRemoteUrl(avatarUrl),
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
            avatarUrl = resolveRemoteUrl(avatarUrl),
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
            avatarUrl = resolveRemoteUrl(avatarUrl),
            avatarResId = remoteResourceMapper.avatarBackgroundForColorHex(avatarColorHex),
            imageUri = imageUrl,
            replyToName = replyToName
        )
    }

    private fun resolveRemoteUrl(url: String?): String? {
        val trimmed = url?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        return remoteApiConfig.baseUrl.resolve(trimmed)?.toString() ?: trimmed
    }

    companion object {
        /** 日志标签。 */
        private const val TAG = "NoteDetailActivity"
        /** 笔记 ID 传参键。 */
        private const val EXTRA_NOTE_ID = "extra_note_id"
        /** 标题传参键。 */
        private const val EXTRA_TITLE = "extra_title"
        /** 作者传参键。 */
        private const val EXTRA_AUTHOR = "extra_author"
        /** 点赞数传参键。 */
        private const val EXTRA_LIKE_COUNT = "extra_like_count"
        /** 描述传参键。 */
        private const val EXTRA_DESCRIPTION = "extra_description"
        /** 媒体类型传参键。 */
        private const val EXTRA_MEDIA_TYPE = "extra_media_type"
        /** 图片列表传参键。 */
        private const val EXTRA_IMAGE_URLS = "extra_image_urls"
        /** 单图地址传参键。 */
        private const val EXTRA_IMAGE_URL = "extra_image_url"
        /** 视频地址传参键。 */
        private const val EXTRA_VIDEO_URL = "extra_video_url"
        /** 封面地址传参键。 */
        private const val EXTRA_COVER_URL = "extra_cover_url"

        /** 播放配置存储文件名。 */
        private const val PREFS_PLAYBACK = "note_video_playback"
        /** 播放倍速配置键。 */
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
        /** 静音状态配置键。 */
        private const val KEY_MUTED = "muted"
        /** 全屏状态保存键。 */
        private const val STATE_FULLSCREEN = "state_fullscreen"
        /** 手势亮度默认值。 */
        private const val DEFAULT_GESTURE_BRIGHTNESS = 0.5f
        /** 全屏控制层自动隐藏时长。 */
        private const val FULLSCREEN_CONTROLS_AUTO_HIDE_MS = 2_500L
        /** 发布时间显示格式。 */
        private val publishTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

        /**
         * 创建详情页跳转 Intent。
         *
         * @param context 当前上下文。
         * @param item 需要展示的首页卡片模型。
         * @return 配置完成的详情页 Intent。
         */
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

/**
 * 评论排序模式枚举。
 */
private enum class CommentSortMode {
    /** 默认排序。 */
    DEFAULT,
    /** 按最新时间排序。 */
    LATEST,
    /** 按点赞数排序。 */
    MOST_LIKED
}

/**
 * 将评论排序模式转换为后端请求值。
 *
 * @return 对应后端接口使用的排序字符串。
 */
private fun CommentSortMode.backendValue(): String {
    return when (this) {
        CommentSortMode.DEFAULT -> "default"
        CommentSortMode.LATEST -> "latest"
        CommentSortMode.MOST_LIKED -> "most_liked"
    }
}

    /**
     * 评论输入对话框状态。
     *
     * ActivityResult 回调与对话框生命周期并不是同一个调用栈，因此这里把对话框实例、
     * 输入控件、图片预览控件和“正在回复谁”的上下文一起保存，便于图片选择返回后继续
     * 更新同一个弹窗，而不是丢失用户已输入的内容或回复目标。
     */
private data class CommentDialogState(
    /** 当前对话框实例。 */
    val dialog: AlertDialog,
    /** 评论输入框。 */
    val input: EditText,
    /** 图片选择按钮。 */
    val imageButton: TextView,
    /** 图片预览容器。 */
    val imagePreviewContainer: FrameLayout,
    /** 图片预览视图。 */
    val imagePreview: ImageView,
    /** 移除图片按钮。 */
    val removeImageButton: ImageView,
    /** 当前回复所属父评论。 */
    val parentComment: NoteCommentUiModel?,
    /** 当前回复目标作者。 */
    val replyToAuthor: String?,
    /** 当前选择的图片地址。 */
    var selectedImageUri: String?
)

/**
 * 评论展示模型。
 */
private data class NoteCommentUiModel(
    /** 评论唯一标识。 */
    val id: String,
    /** 评论作者。 */
    val author: String,
    /** 评论内容。 */
    val content: String,
    /** 评论城市信息。 */
    val city: String,
    /** 评论时间戳。 */
    val timestamp: Long,
    /** 当前点赞数。 */
    var likeCount: Int,
    /** 当前是否已点赞。 */
    var isLiked: Boolean = false,
    /** 当前评论是否为作者本人发布。 */
    val isAuthor: Boolean = false,
    /** 评论作者头像地址。 */
    val avatarUrl: String? = null,
    /** 头像背景资源 ID。 */
    val avatarResId: Int,
    /** 评论图片地址。 */
    val imageUri: String? = null,
    /** 回复列表。 */
    val replies: MutableList<NoteReplyUiModel> = mutableListOf(),
    /** 是否已展开全部回复。 */
    var isReplyExpanded: Boolean = false
)

/**
 * 评论回复展示模型。
 */
private data class NoteReplyUiModel(
    /** 回复唯一标识。 */
    val id: String = "",
    /** 回复作者。 */
    val author: String,
    /** 回复内容。 */
    val content: String,
    /** 回复城市信息。 */
    val city: String,
    /** 回复时间戳。 */
    val timestamp: Long,
    /** 当前点赞数。 */
    var likeCount: Int,
    /** 当前是否已点赞。 */
    var isLiked: Boolean = false,
    /** 当前回复是否为作者本人发布。 */
    val isAuthor: Boolean,
    /** 回复作者头像地址。 */
    val avatarUrl: String? = null,
    /** 头像背景资源 ID。 */
    val avatarResId: Int,
    /** 回复图片地址。 */
    val imageUri: String? = null,
    /** 被回复用户名。 */
    val replyToName: String? = null
)

/**
 * 评论列表适配器。
 *
 * 适配器本身尽量保持“纯渲染”职责：
 * 1. 只负责把评论模型映射到 View。
 * 2. 不直接修改仓库或发请求。
 * 3. 所有点赞、回复、展开/收起动作都通过回调回传给 Activity 处理。
 *
 * 这样可以让列表层保持轻量，也方便后续把评论区迁移到 Fragment 或独立组件。
 */
private class NoteCommentAdapter(
    private val onLikeClick: (NoteCommentUiModel) -> Unit,
    private val onReplyLikeClick: (NoteReplyUiModel) -> Unit,
    private val onReplyClick: (NoteCommentUiModel, String?) -> Unit,
    private val onReplyToggleClick: (NoteCommentUiModel) -> Unit
) : RecyclerView.Adapter<NoteCommentAdapter.NoteCommentViewHolder>() {

    /** 当前评论列表数据。 */
    private val items = mutableListOf<NoteCommentUiModel>()

    /**
     * 提交评论列表数据。
     *
     * @param comments 最新评论集合。
     */
    fun submitList(comments: List<NoteCommentUiModel>) {
        items.clear()
        items.addAll(comments)
        notifyDataSetChanged()
    }

    /**
     * 创建评论条目持有者。
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteCommentViewHolder {
        val binding = ItemNoteCommentBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteCommentViewHolder(binding)
    }

    /**
     * 绑定指定位置的评论条目。
     */
    override fun onBindViewHolder(holder: NoteCommentViewHolder, position: Int) {
        holder.bind(
            comment = items[position],
            onLikeClick = onLikeClick,
            onReplyLikeClick = onReplyLikeClick,
            onReplyClick = onReplyClick,
            onReplyToggleClick = onReplyToggleClick
        )
    }

    /** 返回评论条目数量。 */
    override fun getItemCount(): Int = items.size

    /**
     * 评论条目视图持有者。
     */
    class NoteCommentViewHolder(
        private val binding: ItemNoteCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * 绑定评论及其回复内容。
         *
         * 这里先完成一级评论自身的头像、正文、图片和点赞态渲染，
         * 再把回复区域单独交给 `bindReplies` 处理，减少单个方法的分支复杂度。
         */
        fun bind(
            comment: NoteCommentUiModel,
            onLikeClick: (NoteCommentUiModel) -> Unit,
            onReplyLikeClick: (NoteReplyUiModel) -> Unit,
            onReplyClick: (NoteCommentUiModel, String?) -> Unit,
            onReplyToggleClick: (NoteCommentUiModel) -> Unit
        ) {
            val context = binding.root.context
            bindCommentAvatar(
                imageView = binding.commentAvatarImage,
                textView = binding.commentAvatar,
                avatarUrl = comment.avatarUrl,
                fallbackText = comment.author.take(1),
                avatarResId = comment.avatarResId
            )
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

        /**
         * 绑定评论回复区。
         *
         * 当前实现采用“嵌套动态添加子 View”的方式展示回复，而不是第二层 RecyclerView。
         * 这是因为回复数量通常较少，结构相对固定，直接 inflate 能减少嵌套滚动和事件分发成本。
         */
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
                bindCommentAvatar(
                    imageView = replyBinding.replyAvatarImage,
                    textView = replyBinding.replyAvatar,
                    avatarUrl = reply.avatarUrl,
                    fallbackText = reply.author.take(1),
                    avatarResId = reply.avatarResId
                )
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

        /**
         * 更新回复点赞视图状态。
         */
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

        /**
         * 绑定评论头像。
         *
         * 为了兼容异步图片加载复用场景，使用 `tag` 校验当前 ImageView 期望展示的地址，
         * 防止旧请求回调晚到时把已经复用到其他评论上的头像错误覆盖。
         */
        private fun bindCommentAvatar(
            imageView: ImageView,
            textView: TextView,
            avatarUrl: String?,
            fallbackText: String,
            avatarResId: Int
        ) {
            val background = AppCompatResources.getDrawable(textView.context, avatarResId)
            textView.text = fallbackText
            textView.background = background
            imageView.background = background?.constantState?.newDrawable()?.mutate()
            if (avatarUrl.isNullOrBlank()) {
                imageView.tag = null
                imageView.visibility = View.GONE
                imageView.setImageDrawable(null)
                textView.visibility = View.VISIBLE
                return
            }
            imageView.tag = avatarUrl
            imageView.visibility = View.GONE
            imageView.setImageDrawable(null)
            textView.visibility = View.VISIBLE
            imageView.load(avatarUrl) {
                crossfade(true)
                listener(
                    onSuccess = { _, _ ->
                        if (imageView.tag != avatarUrl) return@listener
                        imageView.visibility = View.VISIBLE
                        textView.visibility = View.GONE
                    },
                    onError = { _, _ ->
                        if (imageView.tag != avatarUrl) return@listener
                        imageView.setImageDrawable(null)
                        imageView.visibility = View.GONE
                        textView.visibility = View.VISIBLE
                    }
                )
            }
        }
    }
}

/**
 * 将时间戳转换为相对时间文案。
 *
 * @param timestamp 原始时间戳。
 * @return 面向用户展示的相对时间字符串。
 */
private fun relativeTimeLabel(timestamp: Long): String {
    val diffMinutes = ((System.currentTimeMillis() - timestamp) / 60_000L).coerceAtLeast(0L)
    return when {
        diffMinutes < 1L -> "刚刚"
        diffMinutes < 60L -> "${diffMinutes}分钟前"
        diffMinutes < 24L * 60L -> "${diffMinutes / 60L}小时前"
        else -> "${diffMinutes / (24L * 60L)}天前"
    }
}

/**
 * 将 dp 转换为像素值。
 *
 * @param context 当前上下文。
 * @param valueDp 需要转换的 dp 值。
 * @return 对应像素值。
 */
private fun dpToPx(context: Context, valueDp: Int): Int {
    return (valueDp * context.resources.displayMetrics.density).roundToInt()
}

/**
 * 图文详情图片分页适配器。
 *
 * 适配器只关心图片展示与点击回调，不持有页面级状态；
 * 是否全屏由外部通过 `isFullscreen` 注入，确保分页组件本身保持简单。
 */
private class NoteImagePagerAdapter : RecyclerView.Adapter<NoteImagePagerAdapter.NoteImageViewHolder>() {

    /** 当前图片地址列表。 */
    private val items = mutableListOf<String>()
    /** 图片点击回调。 */
    var onImageTap: (() -> Unit)? = null
    /** 当前是否处于全屏图片模式。 */
    var isFullscreen: Boolean = false

    /**
     * 提交图片列表数据。
     *
     * @param imageUrls 最新图片地址列表。
     */
    fun submitList(imageUrls: List<String>) {
        items.clear()
        items.addAll(imageUrls)
        notifyDataSetChanged()
    }

    /**
     * 创建图片分页条目持有者。
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteImageViewHolder {
        val binding = ItemNoteDetailImageBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteImageViewHolder(binding)
    }

    /**
     * 绑定指定位置的图片分页条目。
     */
    override fun onBindViewHolder(holder: NoteImageViewHolder, position: Int) {
        holder.bind(
            imageUrl = items[position],
            isFullscreen = isFullscreen,
            onImageTap = onImageTap
        )
    }

    /** 返回图片条目数量。 */
    override fun getItemCount(): Int = items.size

    /**
     * 图片分页条目持有者。
     */
    class NoteImageViewHolder(
        private val binding: ItemNoteDetailImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * 绑定单张详情图片。
         *
         * 普通态使用裁剪填充，保证瀑布流式详情页更紧凑；
         * 全屏态切换为完整适配，优先确保用户能看到整张图片。
         */
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


