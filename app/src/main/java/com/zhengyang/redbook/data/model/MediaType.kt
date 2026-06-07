/**
 * 文件说明：MediaType.kt
 * 作用：定义当前文件在项目中的核心实现与职责。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.model

/**
 * 媒体类型
 *
 * 用于标记一篇笔记的主要内容形式，
 * 供列表展示、详情页跳转和发布流程统一判断。
 */
enum class MediaType {
    /** 单图或多图内容。 */
    IMAGE,

    /** 视频内容。 */
    VIDEO,

    /** 纯文本内容。 */
    TEXT,

    /** 长文内容。 */
    LONG_FORM
}
