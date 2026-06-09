/**
 * 文件说明：RedBookApiService.kt
 * 作用：声明远端接口访问方法与请求定义。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.data.remote.model.ApiResponseDto
import com.zhengyang.redbook.data.remote.model.PageResponseDto
import com.zhengyang.redbook.data.remote.model.RemoteCommentDto
import com.zhengyang.redbook.data.remote.model.RemoteCreateCommentRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteCreateNoteRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteDraftDto
import com.zhengyang.redbook.data.remote.model.RemoteFeedItemDto
import com.zhengyang.redbook.data.remote.model.RemoteFollowToggleRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteFollowingSeedDto
import com.zhengyang.redbook.data.remote.model.RemoteHomeCategoryDto
import com.zhengyang.redbook.data.remote.model.RemoteMessageOverviewDto
import com.zhengyang.redbook.data.remote.model.RemoteMyInterestPersonDto
import com.zhengyang.redbook.data.remote.model.RemoteMyProfileDto
import com.zhengyang.redbook.data.remote.model.RemoteNoteDetailDto
import com.zhengyang.redbook.data.remote.model.RemoteSaveDraftRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteSearchBootstrapDto
import com.zhengyang.redbook.data.remote.model.RemoteSearchGuessDto
import com.zhengyang.redbook.data.remote.model.RemoteSearchResultDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Part
import retrofit2.http.Query

/**
 * 后端接口服务定义
 *
 * 通过 Retrofit 注解描述各业务模块的 HTTP 契约，
 * 供仓储层直接调用并获取结构化 DTO。
 */
interface RedBookApiService {

    /** 获取首页分类列表。 */
    @GET("api/v1/home/categories")
    suspend fun getHomeCategories(): ApiResponseDto<List<RemoteHomeCategoryDto>>

    /**
     * 获取首页内容流。
     *
     * @param categoryId 分类标识
     * @param offset 偏移量
     * @param limit 单次请求数量
     * @return 分页内容流结果
     */
    @GET("api/v1/home/feed")
    suspend fun getHomeFeed(
        @Query("categoryId") categoryId: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): ApiResponseDto<PageResponseDto<RemoteFeedItemDto>>

    /**
     * 获取关注页首屏种子数据。
     *
     * @param offset 偏移量
     * @param limit 单次请求数量
     * @return 推荐用户与关注流分页数据
     */
    @GET("api/v1/home/following/seed")
    suspend fun getFollowingSeed(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): ApiResponseDto<RemoteFollowingSeedDto>

    /**
     * 关注或取消关注指定用户。
     *
     * @param userId 目标用户 ID
     * @param request 关注状态切换请求体
     * @return 通用响应体
     */
    @POST("api/v1/users/{userId}/follow")
    suspend fun followUser(
        @Path("userId") userId: String,
        @Body request: RemoteFollowToggleRequestDto = RemoteFollowToggleRequestDto(true)
    ): ApiResponseDto<Map<String, Any?>>

    /** 获取消息页概览数据。 */
    @GET("api/v1/messages/overview")
    suspend fun getMessageOverview(): ApiResponseDto<RemoteMessageOverviewDto>

    /** 获取当前用户个人资料。 */
    @GET("api/v1/me/profile")
    suspend fun getMyProfile(): ApiResponseDto<RemoteMyProfileDto>

    @Multipart
    @POST("api/v1/me/avatar")
    suspend fun uploadMyAvatar(
        @Part file: MultipartBody.Part
    ): ApiResponseDto<RemoteMyProfileDto>

    /** 获取当前用户感兴趣的人列表。 */
    @GET("api/v1/me/interests")
    suspend fun getMyInterests(): ApiResponseDto<List<RemoteMyInterestPersonDto>>

    /**
     * 获取笔记详情。
     *
     * @param noteId 笔记 ID
     * @return 笔记详情 DTO
     */
    @GET("api/v1/notes/{noteId}")
    suspend fun getNoteDetail(
        @Path("noteId") noteId: String
    ): ApiResponseDto<RemoteNoteDetailDto>

    /**
     * 获取笔记评论列表。
     *
     * @param noteId 笔记 ID
     * @param sortMode 排序模式
     * @param offset 偏移量
     * @param limit 单次请求数量
     * @return 评论分页结果
     */
    @GET("api/v1/notes/{noteId}/comments")
    suspend fun getNoteComments(
        @Path("noteId") noteId: String,
        @Query("sortMode") sortMode: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): ApiResponseDto<PageResponseDto<RemoteCommentDto>>

    /**
     * 切换笔记点赞状态。
     *
     * @param noteId 笔记 ID
     * @param request 点赞状态请求体
     * @return 通用响应体
     */
    @POST("api/v1/notes/{noteId}/like")
    suspend fun toggleNoteLike(
        @Path("noteId") noteId: String,
        @Body request: RemoteFollowToggleRequestDto
    ): ApiResponseDto<Map<String, Any?>>

    /**
     * 切换笔记收藏状态。
     *
     * @param noteId 笔记 ID
     * @param request 收藏状态请求体
     * @return 通用响应体
     */
    @POST("api/v1/notes/{noteId}/collect")
    suspend fun toggleNoteCollect(
        @Path("noteId") noteId: String,
        @Body request: RemoteFollowToggleRequestDto
    ): ApiResponseDto<Map<String, Any?>>

    /**
     * 创建评论或回复。
     *
     * @param request 评论创建请求体
     * @return 服务端创建后的评论 DTO
     */
    @POST("api/v1/comments")
    suspend fun createComment(
        @Body request: RemoteCreateCommentRequestDto
    ): ApiResponseDto<RemoteCommentDto>

    /**
     * 切换评论点赞状态。
     *
     * @param commentId 评论 ID
     * @param request 点赞状态请求体
     * @return 通用响应体
     */
    @POST("api/v1/comments/{commentId}/like")
    suspend fun toggleCommentLike(
        @Path("commentId") commentId: String,
        @Body request: RemoteFollowToggleRequestDto
    ): ApiResponseDto<Map<String, Any?>>

    /** 获取搜索页启动数据。 */
    @GET("api/v1/search/bootstrap")
    suspend fun getSearchBootstrap(): ApiResponseDto<RemoteSearchBootstrapDto>

    /**
     * 获取搜索联想词。
     *
     * @param query 当前输入内容
     * @return 联想词列表
     */
    @GET("api/v1/search/suggestions")
    suspend fun getSearchSuggestions(
        @Query("query") query: String
    ): ApiResponseDto<List<RemoteSearchGuessDto>>

    /**
     * 获取搜索结果列表。
     *
     * @param query 搜索词
     * @return 搜索结果项列表
     */
    @GET("api/v1/search/results")
    suspend fun getSearchResults(
        @Query("query") query: String
    ): ApiResponseDto<List<RemoteSearchResultDto>>

    /**
     * 创建新笔记。
     *
     * @param request 创建笔记请求体
     * @return 新创建的笔记详情 DTO
     */
    @POST("api/v1/notes")
    suspend fun createNote(
        @Body request: RemoteCreateNoteRequestDto
    ): ApiResponseDto<RemoteNoteDetailDto>

    /** 获取当前用户草稿列表。 */
    @GET("api/v1/drafts")
    suspend fun getDrafts(): ApiResponseDto<List<RemoteDraftDto>>

    /**
     * 保存草稿。
     *
     * @param request 草稿保存请求体
     * @return 保存后的草稿 DTO
     */
    @POST("api/v1/drafts")
    suspend fun saveDraft(
        @Body request: RemoteSaveDraftRequestDto
    ): ApiResponseDto<RemoteDraftDto>
}
