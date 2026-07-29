package com.zhengyang.redbook.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.request.ImageRequest
import com.zhengyang.redbook.databinding.ItemNoteBinding
import com.zhengyang.redbook.utils.AppLogger
import com.zhengyang.redbook.utils.applyPreloadDefaults
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 首页卡片列表适配器。
 *
 * 这个类本质上是“首页双列卡片列表”和 RecyclerView 之间的桥梁。
 *
 * 它主要负责两类工作：
 * 1. 正常的适配器职责：
 *    把 `currentList` 里的每一个 [HomeCardItem] 交给 [HomeViewHolder]，
 *    让 ViewHolder 负责把一条数据真正渲染到卡片视图上。
 * 2. 额外的图片体验优化职责：
 *    在用户看到当前卡片时，顺手把“后面几张卡片的封面图”提前交给 Coil 做缓存预热，
 *    这样用户继续往下滑时，图片更容易直接命中内存或磁盘缓存，减少白屏等待。
 *
 * 为什么这个预热逻辑放在 Adapter 里？
 * 因为 Adapter 最清楚“当前绑定到了第几项”，也最容易知道“用户接下来大概率会看到哪些项”。
 * 这比把预热逻辑分散到 Fragment 或 ViewHolder 里更集中，也更容易控制重复请求。
 */
class HomeAdapter(
    /**
     * 全局注入的 Coil 图片加载器。
     *
     * 这里不用 `ImageView.load(...)`，而是直接持有 [ImageLoader]，
     * 是因为预加载不是把图显示到某个控件，而是单纯发起一个“进入缓存”的图片请求。
     */
    private val imageLoader: ImageLoader
) : ListAdapter<HomeCardItem, HomeViewHolder>(HomeDiffCallback()) {

    /**
     * 记录“目前已经预热到列表的哪一个下标”。
     *
     * 这个值的意义可以理解成：
     * “`0..lastPreloadedEnd` 这段后续预热任务我已经做过了，不需要因为后面的 onBind 再做一遍。”
     *
     * 为什么需要这个字段？
     * 因为 RecyclerView 在滚动过程中会频繁调用 `onBindViewHolder`。
     * 如果没有这个游标，那么：
     * 1. 绑定 position=0 时，可能预热 1..6
     * 2. 绑定 position=1 时，又会预热 2..7
     * 3. 绑定 position=2 时，又会预热 3..8
     *
     * 这样会造成大量重叠请求，虽然 Coil 自己也有缓存和去重能力，
     * 但调用层仍然会产生很多没有必要的入队动作。
     */
    private var lastPreloadedEnd = RecyclerView.NO_POSITION

    /**
     * 当前列表数据的“轻量签名”。
     *
     * 这个签名不是为了加密，也不是为了唯一标识每一条数据，
     * 而是为了快速判断：
     * “Adapter 现在手上的这一批列表数据，和刚才那一批是不是同一批。”
     *
     * 一旦签名变化，就说明列表很可能整体刷新过了：
     * 比如换了 Tab、接口回来了新数据、下拉刷新完成了。
     * 这时候之前记录的 `lastPreloadedEnd` 就不能再继续沿用了，
     * 否则会出现“明明已经换了一批数据，却错误地认为后面的图片已经预热过”的问题。
     */
    private var preloadSignature: String? = null

    /**
     * 列表项点击回调。
     *
     * Adapter 本身不处理“点击后跳转到哪里”这种业务，
     * 它只负责把点击事件透传出去，让外层 Fragment / Activity 决定下一步行为。
     */
    var onItemClick: ((HomeCardItem) -> Unit)? = null
    var onVideoPreloadRequest: ((List<String>) -> Unit)? = null

    /**
     * 创建一个新的 ViewHolder。
     *
     * 调用时机：
     * RecyclerView 发现当前屏幕需要新的卡片 View，但缓存池里没有可复用的 ViewHolder 时。
     *
     * 这里做的事情很简单：
     * 1. 用 `ItemNoteBinding` inflate 出一张卡片布局
     * 2. 把这张布局和全局 `imageLoader` 一起交给 [HomeViewHolder]
     *
     * 注意：
     * 这里“创建”只是创建空壳视图，不会绑定具体数据。
     * 真正把某条笔记数据显示到卡片上的动作，发生在 `onBindViewHolder`。
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        AppLogger.d(
            TAG,
            "onCreateViewHolder: viewType=$viewType, parent=${parent::class.java.simpleName}"
        )
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HomeViewHolder(binding, imageLoader)
    }

    /**
     * 把某个位置的数据绑定到已经创建好的 ViewHolder 上。
     *
     * 这里是整个 Adapter 最核心的入口之一。
     * 每当 RecyclerView 需要“让第 `position` 项显示出来”时，就会走到这里。
     *
     * 当前方法内部做了两件事：
     * 1. `holder.bind(item, onItemClick)`
     *    让 ViewHolder 负责真正渲染卡片内容，比如封面、头像、标题、点赞数等。
     * 2. `preloadAhead(position, context)`
     *    以当前项为参考，顺手预热后面几张封面图。
     *
     * 可以把它理解成：
     * “显示当前项的同时，开始为下一屏做准备。”
     */
    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val item = getItem(position)
        AppLogger.d(
            TAG,
            "onBindViewHolder: position=$position, id=${item.id}, isSkeleton=${item.isSkeleton}, mediaType=${item.mediaType}"
        )
        holder.bind(item, onItemClick)
        preloadAhead(position, holder.itemView.context)
    }

    /**
     * 某个 ViewHolder 即将被回收到复用池时触发。
     *
     * 这里会调用 `holder.recycle()`，
     * 让 ViewHolder 主动释放自己持有的图片、播放器、点击事件等资源。
     *
     * 为什么要显式回收？
     * 因为首页卡片不只是纯文本，还包含图片、头像，甚至可能有视频相关资源。
     * 如果不主动清理，复用时更容易出现旧图闪现、旧状态残留、资源占用偏高等问题。
     */
    override fun onViewRecycled(holder: HomeViewHolder) {
        AppLogger.d(TAG, "onViewRecycled: holder=${holder.hashCode()}")
        holder.recycle()
        super.onViewRecycled(holder)
    }

    /**
     * 某个 ViewHolder 从屏幕上分离时触发。
     *
     * 这和 `onViewRecycled` 不完全一样：
     * - `onViewDetachedFromWindow` 表示它暂时离开屏幕
     * - `onViewRecycled` 表示它准备被彻底回收到池里复用
     *
     * 当前项目里 `holder.onDetached()` 还是轻量处理，
     * 但这个钩子保留下来很有价值，后续如果要做：
     * - 曝光打点结束
     * - 自动播放暂停
     * - 离屏性能优化
     * 都可以从这里继续扩展。
     */
    override fun onViewDetachedFromWindow(holder: HomeViewHolder) {
        AppLogger.d(TAG, "onViewDetachedFromWindow: holder=${holder.hashCode()}")
        holder.onDetached()
        super.onViewDetachedFromWindow(holder)
    }

    /**
     * 预热当前项后面的几张封面图。
     *
     * 这是这个 Adapter 里最值得读懂的方法。
     *
     * 它的目标不是“把图片显示到界面”，而是：
     * 提前发起图片请求，让 Coil 尽量把图片缓存起来。
     *
     * 这样用户继续向下滑动时，真正绑定下一批卡片的封面图时，
     * 很可能已经不需要重新走完整的网络下载和解码过程，体验会更顺。
     *
     * 参数说明：
     * @param position 当前已经显示出来的卡片位置。
     * @param context 用来构建 Coil 请求和读取屏幕尺寸。
     */
    private fun preloadAhead(position: Int, context: Context) {
        val items = currentList

        // 如果列表本身没有数据，就没有任何预热目标，直接结束。
        if (items.isEmpty()) {
            AppLogger.d(TAG, "preloadAhead skipped: currentList is empty, position=$position")
            return
        }

        // 先为当前列表生成一个轻量签名，用来判断“这是不是同一批数据”。
        val signature = buildPreloadSignature(items)

        // 一旦签名不同，说明列表整体发生了明显变化。
        // 此时必须把 lastPreloadedEnd 清掉，让新列表重新从头计算预热进度。
        if (preloadSignature != signature) {
            AppLogger.d(
                TAG,
                "preload signature changed: old=$preloadSignature, new=$signature, reset lastPreloadedEnd"
            )
            preloadSignature = signature
            lastPreloadedEnd = RecyclerView.NO_POSITION
        }

        // 本次预热从哪里开始？
        // 取下面两个值的较大者：
        // 1. position + 1：当前项的下一项
        // 2. lastPreloadedEnd + 1：上次预热结束位置的下一项
        //
        // 这样做的目的就是避免重复预热已经处理过的区间。
        val preloadStart = max(position + 1, lastPreloadedEnd + 1)

        // 本次预热到哪里结束？
        // 最多只看当前项后面的 PRELOAD_AHEAD_COUNT 项，
        // 同时不能超过列表最后一个下标。
        val preloadEnd = minOf(items.lastIndex, position + PRELOAD_AHEAD_COUNT)
        AppLogger.d(
            TAG,
            "preload range: position=$position, preloadStart=$preloadStart, preloadEnd=$preloadEnd, lastPreloadedEnd=$lastPreloadedEnd"
        )

        // 如果开始位置已经比结束位置还大，说明这一次没有任何新的项目需要预热。
        // 常见场景：
        // 1. 这一段刚刚已经预热过了
        // 2. 已经滑到列表尾部附近，没有更多可预热项
        if (preloadStart > preloadEnd) {
            AppLogger.d(TAG, "preloadAhead skipped: preloadStart > preloadEnd")
            return
        }

        val displayMetrics = context.resources.displayMetrics

        // 预热尺寸并不是原图尺寸，而是“这个列表里大概会用到的显示尺寸”。
        //
        // 当前页面是双列瀑布流，所以这里用“半个屏幕宽度”作为封面大致宽度。
        // 这样做的好处是：
        // 1. 不需要把原图完整解码到超大尺寸
        // 2. 更接近真正显示时的目标尺寸
        // 3. 命中缓存后，更可能直接复用，不需要再次缩放
        val preloadWidth = (displayMetrics.widthPixels / 2).coerceAtLeast(1)

        // 高度用一个经验比例估算，目的是让预热结果更贴近卡片封面的常见纵横比。
        // 即使不是绝对精确，也比完全不给 size 更有利于缓存命中和解码成本控制。
        val preloadHeight = max(preloadWidth, (preloadWidth * 1.35f).roundToInt())
        AppLogger.d(
            TAG,
            "preload target size: screenWidth=${displayMetrics.widthPixels}, preloadWidth=$preloadWidth, preloadHeight=$preloadHeight"
        )
        val videoPreloadUrls = mutableListOf<String>()

        // 遍历本次需要预热的区间，一项一项判断是否值得发请求。
        for (index in preloadStart..preloadEnd) {
            val item = items[index]

            // 骨架屏只是占位假数据，没有真实图片地址，不应该进入预热流程。
            if (item.isSkeleton) {
                AppLogger.d(TAG, "preload skip: index=$index, id=${item.id}, reason=skeleton")
                continue
            }

            // 取出这张卡片最主要的封面地址。
            // `primaryCoverUrl()` 的含义通常是：
            // “不管它是图文卡还是视频卡，返回最适合当封面的那个 URL”。
            val coverUrl = item.primaryCoverUrl()

            // URL 为空就没有预热价值，直接跳过。
            if (coverUrl.isNullOrBlank()) {
                AppLogger.d(TAG, "preload skip: index=$index, id=${item.id}, reason=empty_cover_url")
                continue
            }

            AppLogger.d(
                TAG,
                "preload enqueue: index=$index, id=${item.id}, coverUrl=$coverUrl"
            )

            // 这里是真正发起预热请求的地方。
            //
            // 注意几个关键点：
            // 1. `.data(coverUrl)`
            //    告诉 Coil 要加载哪张图片。
            // 2. `.size(preloadWidth, preloadHeight)`
            //    告诉 Coil 预期目标尺寸，避免按原图超大尺寸处理。
            // 3. `.applyPreloadDefaults()`
            //    打开内存 / 磁盘 / 网络缓存策略，明确这是一个缓存预热请求。
            // 4. 没有 `.target(...)`
            //    说明这次请求不打算把图显示到界面，只为了把资源“提前准备好”。
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(coverUrl)
                    .size(preloadWidth, preloadHeight)
                    .applyPreloadDefaults()
                    .build()
            )

            if (item.mediaType == HomeCardItem.MediaType.VIDEO) {
                item.videoUrl
                    ?.takeIf { it.isNotBlank() }
                    ?.let(videoPreloadUrls::add)
            }
        }

        if (videoPreloadUrls.isNotEmpty()) {
            val distinctUrls = videoPreloadUrls.distinct()
            AppLogger.d(
                TAG,
                "video preload request: position=$position, count=${distinctUrls.size}, urls=${distinctUrls.joinToString()}"
            )
            onVideoPreloadRequest?.invoke(distinctUrls)
        }

        // 这次区间处理完成后，把游标推进到本次结束位置。
        // 下次再进来时，就会从新的位置往后继续算，避免重复劳动。
        lastPreloadedEnd = preloadEnd
        AppLogger.d(TAG, "preload complete: lastPreloadedEnd=$lastPreloadedEnd")
    }

    /**
     * 为当前列表构建一个“足够便宜、足够实用”的签名。
     *
     * 这里没有做复杂哈希，而是只拼接了三个信息：
     * 1. 列表总长度
     * 2. 第一项 id
     * 3. 最后一项 id
     *
     * 为什么这样就够用了？
     * 因为这个签名的目的不是 100% 严格判断两份列表完全相等，
     * 而是快速判断“这批数据大概率是不是已经换过了”。
     *
     * 对预加载场景来说，这种轻量判断通常已经足够：
     * - 成本低
     * - 可读性强
     * - 不需要遍历并拼接整份列表的所有 id
     *
     * 当然，这也意味着它不是绝对严谨的：
     * 如果列表中间内容变了，但首尾和长度都没变，签名也可能不变。
     * 不过对“预加载进度是否需要重置”这个问题来说，这个折中是可以接受的。
     */
    private fun buildPreloadSignature(items: List<HomeCardItem>): String {
        val signature = buildString {
            append(items.size)
            append(':')
            append(items.firstOrNull()?.id.orEmpty())
            append(':')
            append(items.lastOrNull()?.id.orEmpty())
        }
        AppLogger.d(
            TAG,
            "buildPreloadSignature: size=${items.size}, first=${items.firstOrNull()?.id.orEmpty()}, last=${items.lastOrNull()?.id.orEmpty()}, signature=$signature"
        )
        return signature
    }

    /**
     * ListAdapter 的 Diff 计算器。
     *
     * RecyclerView 不会因为你 `submitList()` 一次，就傻乎乎地整列表全部重绘。
     * 它会借助 DiffUtil 去判断：
     * 1. 哪些是同一条数据
     * 2. 哪些内容真的发生了变化
     *
     * 这样可以减少不必要的刷新，提升滚动性能和动画表现。
     */
    private class HomeDiffCallback : DiffUtil.ItemCallback<HomeCardItem>() {
        /**
         * 判断两项是不是“同一个对象”。
         *
         * 这里用 `id` 判断，意思是：
         * 只要两条数据 id 相同，就认为它们代表同一张卡片。
         *
         * 即使标题、点赞数、封面等内容变了，
         * 只要 id 不变，它们仍然是“同一项的内容更新”。
         */
        override fun areItemsTheSame(oldItem: HomeCardItem, newItem: HomeCardItem): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * 判断同一项的“内容”是否完全一致。
         *
         * 这里直接用 Kotlin data class 的整体相等比较：
         * `oldItem == newItem`
         *
         * 只要其中任意字段变了，比如：
         * - 标题变了
         * - 点赞数变了
         * - 封面 URL 变了
         * - 是否骨架屏变了
         * 就会返回 false，RecyclerView 才会重新绑定这一项。
         */
        override fun areContentsTheSame(oldItem: HomeCardItem, newItem: HomeCardItem): Boolean {
            return oldItem == newItem
        }
    }

    private companion object {
        /**
         * 这个类统一使用的日志标签。
         *
         * 这样在 logcat 里可以直接搜索 `HomeAdapter`，
         * 只看当前文件的日志，不会和别的图片加载日志混在一起。
         */
        private const val TAG = "HomeAdapter"

        /**
         * 每次最多向后预热多少张卡片。
         *
         * 这个值不能太大，也不能太小：
         * - 太小：用户滑快一点，预热来不及
         * - 太大：浪费流量、内存和解码时间
         *
         * 当前取 6，是一个偏保守但实用的经验值。
         */
        private const val PRELOAD_AHEAD_COUNT = 6
    }
}
