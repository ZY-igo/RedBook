# Change Log

## 2026-07-16 图片全屏显示优化

### 背景

本次修改围绕两个图片查看场景展开：

1. `NoteDetailActivity` 的多图全屏浏览。
2. `EditProfileActivity` 的头像查看与替换。

### 修改前的问题

`NoteDetailActivity` 原实现存在这些问题：

- 全屏切换本质上只是 `CENTER_CROP` 和 `FIT_CENTER` 的切换，不支持双指缩放、拖拽查看细节。
- `setImageFullscreen()` 使用 `notifyDataSetChanged()`，会触发整组图片重新绑定，刷新过重。
- 全屏切换缺少过渡，系统栏、背景色、上下区域和图片展示模式变化比较生硬。
- 图片放大查看场景下，没有处理父级横向分页与子视图缩放手势的冲突。

`EditProfileActivity` 原实现存在这些问题：

- 头像只能替换，不能查看大图。
- 用户无法在上传前确认头像细节，只能依赖 74dp 的小圆图预览。

### 考虑过的方案

1. 保持原生 `ImageView`，手写缩放、拖拽、双击放大与边界处理。
2. 接入成熟缩放控件，在现有列表结构上做增强。
3. 彻底重构详情页图片链路，统一改成独立预览组件。

### 最终方案

采用方案 2，并对单图场景补一个独立预览页：

- 详情页图片条目接入 `PhotoView`，直接获得缩放、拖拽能力。
- 保留当前 `RecyclerView + SnapHelper` 分页结构，避免大面积改造。
- 全屏切换改为只刷新当前可见图片条目，降低不必要的重复绑定。
- 补布局过渡和媒体区域淡入动画，让全屏切换更平滑。
- 新增通用 `ImagePreviewActivity`，专门承接头像等单图全屏预览。

### 为什么采用这个方案

- 这是当前代码结构下收益最高、侵入性最低的改造。
- 不需要重写详情页现有的分页、高度同步和指示器逻辑。
- 比手写手势层更稳，也更容易维护。
- 可以顺手把头像预览问题一起解决，复用价值更高。

### 本次具体修改

#### 1. 依赖与仓库

- 在 `settings.gradle.kts` 中增加 `jitpack.io`。
- 在 `gradle/libs.versions.toml` 中新增 `photoView = 2.0.0`。
- 在 `app/build.gradle.kts` 中加入 `implementation(libs.photo.view)`。

选择 `2.0.0` 的原因：

- 该版本已经提供当前项目可直接接入的 `com.github.chrisbanes.photoview.PhotoView` 包路径。
- 对现有 `ViewBinding + XML` 结构兼容成本低。

#### 2. 详情页大图浏览

- `item_note_detail_image.xml`
  - 根节点改成容器。
  - 内部图片控件替换为 `PhotoView`。

- `NoteDetailActivity.kt`
  - 全屏切换增加 `TransitionManager` 过渡。
  - 媒体区域增加淡入动画。
  - 把整组 `notifyDataSetChanged()` 改成当前可见项 `payload` 刷新。
  - 给图片项补缩放比例初始化、缩放状态重置和父容器手势冲突处理。

#### 3. 编辑资料页头像预览

- 新增 `ImagePreviewActivity.kt`。
- 新增 `activity_image_preview.xml`。
- 支持远程 URL 与本地 `Uri` 两种图片来源。
- 点击头像时：
  - 有头像则进入全屏预览。
  - 无头像则继续拉起系统选图。
- 长按头像时，始终进入更换头像流程。
- 保留“更换头像”文字入口，不破坏原先的替换路径。

### 这次没有直接实现的内容

没有直接做“高清图渐进加载”，原因是：

- 当前数据层只提供单一图片 URL，没有缩略图/原图双地址。
- 在没有多尺寸资源的前提下，前端重复请求同一地址意义有限。
- 因此先优先解决缩放能力、刷新粒度和切换体验。

### 风险与后续建议

- 详情页仍沿用当前 RecyclerView 分页结构，后续如果要做共享元素转场，建议把详情页首图来源也统一接入预览组件。
- 如果后端未来提供多尺寸图片地址，可以继续在 Coil 层补渐进式加载。
- 如果后续需要“下滑关闭”或背景跟随缩放的交互，建议优先在 `ImagePreviewActivity` 中验证，再决定是否扩展到多图详情页。

### 本次涉及文件

- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/zhengyang/redbook/ui/note/NoteDetailActivity.kt`
- `app/src/main/java/com/zhengyang/redbook/ui/my/EditProfileActivity.kt`
- `app/src/main/java/com/zhengyang/redbook/ui/common/ImagePreviewActivity.kt`
- `app/src/main/res/layout/item_note_detail_image.xml`
- `app/src/main/res/layout/activity_image_preview.xml`

### 后续记录约定

后续每次功能修改继续追加新日期章节，统一记录：

1. 背景
2. 修改前问题
3. 方案评估
4. 最终方案与原因
5. 具体改动
6. 风险与后续建议

## 2026-07-16 第二轮补充完善

### 背景

第一轮改动已经补上缩放、局部刷新和头像预览，但仍有一些体验细节没有真正闭环。

### 补充前的问题

- 详情页图片列表在远端详情回填后重新绑定时，指示器和当前浏览位置可能回到第 0 张。
- 分页高度联动仍然优先依赖第一个子项的高度，不够稳定。
- 单图预览页虽然已经可以缩放，但默认仍先显示顶部栏，沉浸感不够强。
- 单图预览页还缺少更自然的退出方式。

### 本轮方案

- 详情页重新绑定图片列表时，保留当前分页位置。
- 分页高度改为优先读取当前 `SnapHelper` 对齐页的高度。
- 单图预览页默认直接进入沉浸式展示。
- 单图预览页增加“非缩放状态下下滑关闭”的补充交互。

### 为什么这样做

- 这些问题不属于架构性缺陷，但会直接影响用户对“全屏图片是否顺手”的感受。
- 相比继续堆更多功能，先把已有交互做完整，收益更高。

### 本轮具体改动

- `NoteDetailActivity.kt`
  - `bindImagePager()` 在提交新列表前记录当前页，并在重新绑定后恢复该页。
  - `updatePagerHeight()` 优先依据当前吸附页计算高度，减少不同图片比例切换时的高度误差。

- `ImagePreviewActivity.kt`
  - 默认 `chromeVisible = false`，进入页面后先展示纯图片。
  - 新增拖拽关闭：
    - 仅在图片未放大且单指拖动时启用。
    - 拖拽中同步调整位移、缩放和背景透明度。
    - 超过阈值直接关闭，否则执行回弹。

### 风险与说明

- 下滑关闭当前只加在单图预览页，没有直接加到详情页多图浏览，避免和横向分页手势冲突。
- 详情页的位置恢复依赖当前列表顺序稳定，如果后端未来改变图片顺序，恢复的是页位置而不是具体图片 ID。
