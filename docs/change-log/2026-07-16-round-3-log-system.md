# 2026-07-16 第三轮完善：运行期文件日志补齐

## 背景

第二次复查后确认两件事：

1. 图片全屏能力并不是完全缺失，当前代码里已经接入了 `PhotoView`，并且已经具备缩放、双击缩放和局部刷新能力。
2. 真正还没有闭环的是日志系统，现状主要还是 `Logcat + SharedPreferences 崩溃摘要`，缺少运行期日志文件落盘。

这次第三轮改动的主目标仍然是日志系统，不是图片链路重构。

## 修改前的问题

- `AppLogger` 只负责写 `Logcat`，应用退出后日志不可直接保留。
- `NetworkLoggingInterceptor` 虽然有网络日志，但也只停留在 `Logcat`。
- `GlobalExceptionHandler` 只保存了崩溃摘要信息，没有保存完整堆栈文本。
- 缺少日志文件清理机制，后续如果直接加文件写入，容易留下目录无限增长的问题。

## 方案评估

考虑过两种路线：

1. 直接在每次 `AppLogger` 调用时同步写文件。
2. 抽一个独立的文件存储器，统一串行异步写入，并集中处理文件轮转和清理。

## 最终方案

采用方案 2。

原因：

- 统一入口更容易维护。
- 异步串行写文件比每次同步落盘更稳，不容易把 IO 抖动带到主线程。
- 后续要补“导出日志”“上传日志”时，也更容易继续扩展。

## 具体改动

### 1. 新增日志文件存储器

新增文件：

- `app/src/main/java/com/zhengyang/redbook/utils/LogFileStore.kt`

职责：

- 日志写入目录固定为应用私有目录 `files/app_logs`
- 按日期输出文件，命名格式为 `redbook-yyyy-MM-dd.log`
- 使用单线程执行器串行写入
- 最多保留 7 个日志文件，超出后自动删除旧文件

### 2. 扩展 AppLogger

修改文件：

- `app/src/main/java/com/zhengyang/redbook/utils/AppLogger.kt`

补充能力：

- 增加 `initialize(context)` 初始化入口
- 增加日志级别枚举 `LogLevel`
- 增加最小日志级别控制
- 在保留 `Logcat` 输出的同时，把日志异步写入文件
- 日志文本中带时间戳、级别、tag 和堆栈信息

### 3. 扩展崩溃信息持久化

修改文件：

- `app/src/main/java/com/zhengyang/redbook/utils/GlobalExceptionHandler.kt`

补充内容：

- 除了原先的 `message` 和 `thread name` 外，额外保存完整 `stacktrace`
- 应用下次启动恢复时，能读出更完整的崩溃摘要

### 4. 在 Application 中初始化日志系统

修改文件：

- `app/src/main/java/com/zhengyang/redbook/RedBookApplication.kt`

补充内容：

- 在 `onCreate()` 中尽早调用 `AppLogger.initialize(this)`
- 这样应用启动后所有通过 `AppLogger` 输出的日志都会进入文件

## 本轮完成后的效果

- 网络日志会随着 `AppLogger` 一起落到文件
- 崩溃日志除了 `Logcat` 外，也会进入文件和崩溃摘要恢复链路
- 应用运行后的日志可以在设备应用私有目录 `files/app_logs` 下持续保留
- 日志目录有基础清理机制，不会无限堆积

## 图片处理复查校正

本轮完成后，我又重新复查了图片处理链路。为了避免后续误判，这里补一版基于当前代码状态的校正结论。

### 已确认存在的能力

- `ImagePreviewActivity` 已具备单图预览、缩放、点击切换 chrome、拖拽返回。
- `NoteDetailActivity` 的图文详情图片同样已经接入 `PhotoView` 缩放能力。
- `HomeViewHolder` 的首页卡片图片已使用 `ViewSizeResolver` 控制加载尺寸，并且有 `placeholder / error / fallback`。
- `HomeViewHolder` 已实现头像加载失败后的文字头像回退、骨架屏占位和 `recycle()` 资源清理。
- `HomeFollowingSectionRenderer` 现在也已经有完整的头像加载 `listener`、失败回退和日志记录，这一项不应再被归类为“缺少错误监听”。
- `MyFragment` 的头像加载也已经带有错误监听和回退。

### 仍然存在的问题

- 全局 `ImageLoader` 配置仍然偏基础，目前主要只有 `crossfade(true)`，还没有看到显式的内存缓存、磁盘缓存、并发线程、重试或解码质量策略。
- 头像显示仍主要依赖 `clipToOutline` 和圆形背景，没有统一使用 `CircleCrop` 之类的变换策略。
- `NoteDetailActivity` 的视频封面仍使用 `android.R.color.black` 作为 `placeholder` 和 `error`。
- `EditProfileActivity` 的头像预览仍然只有简单 `load + crossfade`，没有补齐失败回退监听。
- 代码里仍未看到 `BlurHash`、低清缩略图占位或明确的图片预加载策略。

### 这轮文档校正的意义

这轮 change-log 的核心交付仍然是日志系统。

图片相关内容在这里的作用只有两个：

1. 明确指出“图片全屏能力完全缺失”这个旧判断已经不成立。
2. 把真正仍待完善的图片问题收敛到配置、回退一致性和体验增强，而不是重复描述已经存在的能力。

## 仍然没有做的内容

- 还没有做远程崩溃上报
- 还没有做 UI 内置的“导出日志”入口
- 还没有做按大小切分日志文件，只做了按日期和文件数量清理
- 还没有做全局图片加载策略增强
- 还没有统一补齐所有头像加载点的失败回退策略

## 验证

本轮修改后重新执行：

```powershell
.\gradlew.bat assembleDebug
```

构建通过。
