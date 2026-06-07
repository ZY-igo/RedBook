package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.data.remote.model.ApiResponseDto
import com.zhengyang.redbook.data.remote.model.RemoteCommentDto
import com.zhengyang.redbook.data.remote.model.RemoteCreateCommentRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteCreateNoteRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteDraftDto
import com.zhengyang.redbook.data.remote.model.PageResponseDto
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
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RedBookApiService {

    @GET("api/v1/home/categories")
    suspend fun getHomeCategories(): ApiResponseDto<List<RemoteHomeCategoryDto>>

    @GET("api/v1/home/feed")
    suspend fun getHomeFeed(
        @Query("categoryId") categoryId: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): ApiResponseDto<PageResponseDto<RemoteFeedItemDto>>

    @GET("api/v1/home/following/seed")
    suspend fun getFollowingSeed(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): ApiResponseDto<RemoteFollowingSeedDto>

    @POST("api/v1/users/{userId}/follow")
    suspend fun followUser(
        @Path("userId") userId: String,
        @Body request: RemoteFollowToggleRequestDto = RemoteFollowToggleRequestDto(true)
    ): ApiResponseDto<Map<String, Any?>>

    @GET("api/v1/messages/overview")
    suspend fun getMessageOverview(): ApiResponseDto<RemoteMessageOverviewDto>

    @GET("api/v1/me/profile")
    suspend fun getMyProfile(): ApiResponseDto<RemoteMyProfileDto>

    @GET("api/v1/me/interests")
    suspend fun getMyInterests(): ApiResponseDto<List<RemoteMyInterestPersonDto>>

    @GET("api/v1/notes/{noteId}")
    suspend fun getNoteDetail(
        @Path("noteId") noteId: String
    ): ApiResponseDto<RemoteNoteDetailDto>

    @GET("api/v1/notes/{noteId}/comments")
    suspend fun getNoteComments(
        @Path("noteId") noteId: String,
        @Query("sortMode") sortMode: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): ApiResponseDto<PageResponseDto<RemoteCommentDto>>

    @POST("api/v1/notes/{noteId}/like")
    suspend fun toggleNoteLike(
        @Path("noteId") noteId: String,
        @Body request: RemoteFollowToggleRequestDto
    ): ApiResponseDto<Map<String, Any?>>

    @POST("api/v1/notes/{noteId}/collect")
    suspend fun toggleNoteCollect(
        @Path("noteId") noteId: String,
        @Body request: RemoteFollowToggleRequestDto
    ): ApiResponseDto<Map<String, Any?>>

    @POST("api/v1/comments")
    suspend fun createComment(
        @Body request: RemoteCreateCommentRequestDto
    ): ApiResponseDto<RemoteCommentDto>

    @POST("api/v1/comments/{commentId}/like")
    suspend fun toggleCommentLike(
        @Path("commentId") commentId: String,
        @Body request: RemoteFollowToggleRequestDto
    ): ApiResponseDto<Map<String, Any?>>

    @GET("api/v1/search/bootstrap")
    suspend fun getSearchBootstrap(): ApiResponseDto<RemoteSearchBootstrapDto>

    @GET("api/v1/search/suggestions")
    suspend fun getSearchSuggestions(
        @Query("query") query: String
    ): ApiResponseDto<List<RemoteSearchGuessDto>>

    @GET("api/v1/search/results")
    suspend fun getSearchResults(
        @Query("query") query: String
    ): ApiResponseDto<List<RemoteSearchResultDto>>

    @POST("api/v1/notes")
    suspend fun createNote(
        @Body request: RemoteCreateNoteRequestDto
    ): ApiResponseDto<RemoteNoteDetailDto>

    @GET("api/v1/drafts")
    suspend fun getDrafts(): ApiResponseDto<List<RemoteDraftDto>>

    @POST("api/v1/drafts")
    suspend fun saveDraft(
        @Body request: RemoteSaveDraftRequestDto
    ): ApiResponseDto<RemoteDraftDto>
}
