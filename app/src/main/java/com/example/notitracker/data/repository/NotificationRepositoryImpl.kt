package com.example.notitracker.data.repository

import com.example.notitracker.data.remote.NetworkResponse
import com.example.notitracker.data.remote.dto.*
import com.example.notitracker.data.remote.datasource.NotificationRemoteDataSource
import com.example.notitracker.data.remote.network.toNetworkResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

interface NotificationRepository {
    fun getNotifications(): Flow<NetworkResponse<List<NotificationDto>>>

    suspend fun getSummary(notifications: List<NotificationDto>): NetworkResponse<SummaryResponse>
}

class NotificationRepositoryImpl(
    private val remoteDataSource: NotificationRemoteDataSource,
) : NotificationRepository {

    override fun getNotifications(): Flow<NetworkResponse<List<NotificationDto>>> = flow {
        emit(NetworkResponse.Loading)
        val result = remoteDataSource.getNotifications()
        emit(result.toNetworkResponse())
    }.catch { e ->
        emit(NetworkResponse.Error(e.message ?: "Unknown error"))
    }

    override suspend fun getSummary(notifications: List<NotificationDto>): NetworkResponse<SummaryResponse> {
        val structuredNotifications = notifications.joinToString("\n") { 
            "- [${it.appName}][Type: ${it.category}] ${it.sender}: ${it.content}"
        }

        val prompt = """
            Bạn là một trợ lý thông báo thông minh. Hãy tóm tắt danh sách thông báo sau một cách súc tích và đưa ra hành động phù hợp.
            
            DANH SÁCH THÔNG BÁO:
            $structuredNotifications
            
            HƯỚNG DẪN XỬ LÝ THEO LOẠI:
            1. FINANCE: Nếu là chi tiêu quá mức, hãy đưa ra cảnh báo nghiêm túc. Nếu là nhận tiền, hãy chúc mừng ngắn gọn.
            2. CHAT/EMAIL: Tóm tắt ý chính. Nếu là spam từ nhóm, hãy gom lại thành 1 câu duy nhất.
            3. LOGISTICS: Nếu shipper đang đến, gợi ý hành động xuống nhận hàng.
            4. SECURITY/OTP: Giữ nguyên mã số xác thực (nếu có), tuyệt đối không làm mất mã.
            
            YÊU CẦU:
            - Tóm tắt cực ngắn (dưới 20 từ).
            - Đưa ra đúng 3 gợi ý trả lời/hành động SIÊU NGẮN (chỉ 1-2 từ mỗi gợi ý). Ví dụ: "Ok", "Đang đến", "Từ chối".
            
            ĐỊNH DẠNG TRẢ VỀ (BẮT BUỘC):
            Tóm tắt: [Nội dung tóm tắt]
            Gợi ý: [Gợi ý 1] | [Gợi ý 2] | [Gợi ý 3]
        """.trimIndent()

        val messages = listOf(
            ChatMessage(role = "system", content = "Bạn là trợ lý tóm tắt thông báo chuyên nghiệp. Luôn trả về đúng định dạng yêu cầu."),
            ChatMessage(role = "user", content = prompt)
        )

        return try {
            val result = remoteDataSource.postChatCompletion(messages)
            when (val response = result.toNetworkResponse()) {
                is NetworkResponse.Success -> {
                    val content = response.data.choices.firstOrNull()?.message?.content ?: ""
                    
                    // Parse logic để tách Summary và Suggested Replies
                    var summary = content
                    var suggestedReplies = listOf("Ok", "Đã hiểu", "Sẽ trả lời sau")

                    try {
                        val lines = content.lines()
                        val summaryLine = lines.find { it.startsWith("Tóm tắt:", ignoreCase = true) }
                        val repliesLine = lines.find { it.startsWith("Gợi ý:", ignoreCase = true) }

                        if (summaryLine != null) {
                            summary = summaryLine.substringAfter(":").trim()
                        }
                        if (repliesLine != null) {
                            suggestedReplies = repliesLine.substringAfter(":")
                                .split("|")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .take(3)
                        }
                    } catch (e: Exception) {
                        // Fallback nếu AI trả về sai format
                    }

                    NetworkResponse.Success(SummaryResponse(
                        summary = summary,
                        suggestedReplies = suggestedReplies
                    ))
                }
                is NetworkResponse.Error -> NetworkResponse.Error(response.message)
                else -> NetworkResponse.Error("Unknown error")
            }
        } catch (e: Exception) {
            NetworkResponse.Error(e.message ?: "Unknown error")
        }
    }
}
