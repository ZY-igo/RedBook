package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.core.common.Resource

interface UseCase<in Input, out Output> {
    suspend operator fun invoke(input: Input): Output
}

data object NoParams

abstract class SuspendUseCase<in Input, Output> : UseCase<Input, Output> {
    final override suspend fun invoke(input: Input): Output = execute(input)

    protected abstract suspend fun execute(input: Input): Output
}

abstract class ResourceUseCase<in Input, Output> : UseCase<Input, Resource<Output>> {
    final override suspend fun invoke(input: Input): Resource<Output> {
        return try {
            Resource.Success(execute(input))
        } catch (throwable: Throwable) {
            Resource.Error(throwable = throwable)
        }
    }

    protected abstract suspend fun execute(input: Input): Output
}
