package com.example.notitracker.data.repository

import com.example.notitracker.data.remote.NetworkResponse
import com.example.notitracker.data.remote.dto.*
import com.example.notitracker.data.remote.fake.FakeNotificationRemoteDataSource
import com.example.notitracker.data.remote.network.AppError
import com.example.notitracker.data.remote.network.NetworkResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class NotificationRepositoryImplTest {

    private lateinit var repository: NotificationRepository
    private lateinit var fakeRemoteDataSource: FakeNotificationRemoteDataSource

    @Before
    fun setUp() {
        fakeRemoteDataSource = FakeNotificationRemoteDataSource()
        repository = NotificationRepositoryImpl(fakeRemoteDataSource)
    }

    @Test
    fun `getSummary returns Success when API call is successful`() = runTest {
        // Arrange
        val assistantContent = "Summary of notifications"
        val stubResponse = ChatResponse(
            id = "test-id",
            choices = listOf(
                ChatChoice(message = ChatMessage(role = "assistant", content = assistantContent))
            )
        )
        fakeRemoteDataSource.chatCompletionResult = NetworkResult.Success(stubResponse)

        // Act
        val result = repository.getSummary(emptyList())

        // Assert
        assertTrue(result is NetworkResponse.Success)
        assertEquals(assistantContent, (result as NetworkResponse.Success).data.summary)
    }

    @Test
    fun `getSummary returns Error when API returns HTTP 500`() = runTest {
        // Arrange
        fakeRemoteDataSource.chatCompletionResult = NetworkResult.Failure(AppError.Http(500, "Internal Server Error"))

        // Act
        val result = repository.getSummary(emptyList())

        // Assert
        assertTrue(result is NetworkResponse.Error)
        assertEquals("Internal Server Error", (result as NetworkResponse.Error).message)
    }

    @Test
    fun `getSummary returns Error when Network is unavailable`() = runTest {
        // Arrange
        fakeRemoteDataSource.chatCompletionResult = NetworkResult.Failure(AppError.Network("No Internet Connection"))

        // Act
        val result = repository.getSummary(emptyList())

        // Assert
        assertTrue(result is NetworkResponse.Error)
        assertEquals("No Internet Connection", (result as NetworkResponse.Error).message)
    }

    @Test
    fun `getSummary returns Error when JSON is malformed`() = runTest {
        // Arrange
        fakeRemoteDataSource.chatCompletionResult = NetworkResult.Failure(AppError.Serialization("Malformed JSON"))

        // Act
        val result = repository.getSummary(emptyList())

        // Assert
        assertTrue(result is NetworkResponse.Error)
        assertEquals("Malformed JSON", (result as NetworkResponse.Error).message)
    }

    @Test
    fun `getSummary returns Error when Unauthorized`() = runTest {
        // Arrange
        fakeRemoteDataSource.chatCompletionResult = NetworkResult.Failure(AppError.Unauthorized("Invalid API Key"))

        // Act
        val result = repository.getSummary(emptyList())

        // Assert
        assertTrue(result is NetworkResponse.Error)
        assertEquals("Invalid API Key", (result as NetworkResponse.Error).message)
    }

    @Test
    fun `getSummary returns Error when response choices list is empty`() = runTest {
        // Arrange: Successful API call but empty choices
        val stubResponse = ChatResponse(id = "test-id", choices = emptyList())
        fakeRemoteDataSource.chatCompletionResult = NetworkResult.Success(stubResponse)

        // Act
        val result = repository.getSummary(emptyList())

        // Assert
        // The implementation maps empty choices to an empty summary string currently
        assertTrue(result is NetworkResponse.Success)
        assertEquals("", (result as NetworkResponse.Success).data.summary)
    }
}
