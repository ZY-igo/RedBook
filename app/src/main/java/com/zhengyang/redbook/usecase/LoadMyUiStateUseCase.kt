package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.repository.MyRepository
import com.zhengyang.redbook.ui.my.MyUiState
import javax.inject.Inject

/**
 * 加载“我的”页状态时使用的参数。
 *
 * @property loggedIn 当前是否需要按已登录态加载。
 * @property forceRefresh 是否强制刷新资料数据。
 */
data class LoadMyUiStateParams(
    val loggedIn: Boolean,
    val forceRefresh: Boolean = false
)

/**
 * 加载“我的”页 UI 状态的用例。
 *
 * @property myRepository “我的”模块仓库。
 */
class LoadMyUiStateUseCase @Inject constructor(
    private val myRepository: MyRepository
) : SuspendUseCase<LoadMyUiStateParams, MyUiState>() {

    /**
     * 组装“我的”页状态。
     *
     * 未登录时直接返回未登录态，
     * 已登录时再继续拉取资料、统计和兴趣推荐。
     *
     * @param input 加载参数。
     * @return 完整“我的”页状态。
     */
    override suspend fun execute(input: LoadMyUiStateParams): MyUiState {
        if (!input.loggedIn) return MyUiState(isLoggedIn = false)

        val profile = myRepository.getProfile(forceRefresh = input.forceRefresh)
        return MyUiState(
            isLoggedIn = true,
            profile = profile,
            stats = myRepository.getProfileStats(forceRefresh = false),
            interestPeople = myRepository.getInterestPeople(forceRefresh = input.forceRefresh)
        )
    }
}
