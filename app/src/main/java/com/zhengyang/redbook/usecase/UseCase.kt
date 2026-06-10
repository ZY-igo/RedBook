package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.core.common.Resource

/**
 * 用例统一契约。
 *
 * 所有用例都接收一个输入参数，并返回一个输出结果。
 *
 * @param Input 输入参数类型。
 * @param Output 输出结果类型。
 */
interface UseCase<in Input, out Output> {

    /**
     * 执行当前用例。
     *
     * @param input 输入参数。
     * @return 用例执行结果。
     */
    suspend operator fun invoke(input: Input): Output
}

/**
 * 无参数用例的占位输入对象。
 */
data object NoParams

/**
 * 返回普通结果的挂起用例基类。
 *
 * 子类只需要实现 [execute]，
 * 外部统一通过 `invoke` 触发用例执行。
 *
 * @param Input 输入参数类型。
 * @param Output 输出结果类型。
 */
abstract class SuspendUseCase<in Input, Output> : UseCase<Input, Output> {

    /**
     * 执行用例入口。
     */
    final override suspend fun invoke(input: Input): Output = execute(input)

    /**
     * 由子类实现的核心业务逻辑。
     *
     * @param input 输入参数。
     * @return 执行结果。
     */
    protected abstract suspend fun execute(input: Input): Output
}

/**
 * 返回 [Resource] 包装结果的用例基类。
 *
 * 该基类会统一捕获异常，
 * 并把成功或失败结果包装成 [Resource.Success] 或 [Resource.Error]。
 *
 * @param Input 输入参数类型。
 * @param Output 成功结果类型。
 */
abstract class ResourceUseCase<in Input, Output> : UseCase<Input, Resource<Output>> {

    /**
     * 执行用例并自动包装结果。
     *
     * @param input 输入参数。
     * @return 资源包装结果。
     */
    final override suspend fun invoke(input: Input): Resource<Output> {
        return try {
            Resource.Success(execute(input))
        } catch (throwable: Throwable) {
            Resource.Error(throwable = throwable)
        }
    }

    /**
     * 由子类实现的核心业务逻辑。
     *
     * @param input 输入参数。
     * @return 成功结果。
     */
    protected abstract suspend fun execute(input: Input): Output
}
