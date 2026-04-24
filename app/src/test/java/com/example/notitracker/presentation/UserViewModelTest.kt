package com.example.notitracker.presentation

import com.example.notitracker.data.remote.network.AppError
import com.example.notitracker.data.remote.network.NetworkResult
import com.example.notitracker.data.repository.UserRepository
import com.example.notitracker.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState reflects successful user load`() = runTest {
        val repo = object : UserRepository {
            override fun getUser(userId: String) = flowOf(
                NetworkResult.Success(User("1", "Ann", "ann@example.com")),
            )
        }
        val vm = UserViewModel("1", repo)

        assertEquals("Ann", vm.uiState.value.user?.name)
        assertEquals(false, vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `uiState reflects failure from repository`() = runTest {
        val repo = object : UserRepository {
            override fun getUser(userId: String) = flowOf(
                NetworkResult.Failure(AppError.Http(404, "not found")),
            )
        }
        val vm = UserViewModel("1", repo)

        assertEquals(false, vm.uiState.value.isLoading)
        assertEquals("not found", vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.user == null)
    }
}
