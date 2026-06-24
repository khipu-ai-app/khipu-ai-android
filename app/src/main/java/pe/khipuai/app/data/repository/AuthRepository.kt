package pe.khipuai.app.data.repository

import kotlinx.coroutines.flow.Flow
import pe.khipuai.app.core.datastore.SessionDataStore
import pe.khipuai.app.data.local.database.AppDatabase
import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.AuthRequest
import pe.khipuai.app.data.remote.dto.UserLoginRequest
import pe.khipuai.app.data.remote.dto.UserRegisterRequest
import pe.khipuai.app.data.remote.dto.UserProfileResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: KhipuApiService,
    private val sessionDataStore: SessionDataStore,
    private val appDatabase: AppDatabase
) {

    val isLoggedIn: Flow<String?> = sessionDataStore.tokenFlow

    suspend fun loginWithGoogle(idToken: String): Result<Unit> {
        return try {
            val response = apiService.googleAuth(AuthRequest(idToken = idToken))
            sessionDataStore.saveToken(response.accessToken)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithEmail(email: String, password: String): Result<Unit> {
        return try {
            val response = apiService.loginTraditional(UserLoginRequest(email, password))
            sessionDataStore.saveToken(response.accessToken)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerWithEmail(email: String, password: String, fullName: String): Result<Unit> {
        return try {
            val response = apiService.registerTraditional(UserRegisterRequest(email, password, fullName))
            sessionDataStore.saveToken(response.accessToken)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchMyProfile(): Result<UserProfileResponse> {
        return try {
            Result.success(apiService.getMyProfile())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchUsage(): Result<pe.khipuai.app.data.remote.dto.UsageResponse> {
        return try {
            Result.success(apiService.getUsage())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchMySubscription(): Result<pe.khipuai.app.data.remote.dto.SubscriptionResponse> {
        return try {
            Result.success(apiService.getMySubscription())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMySubscription(plan: String): Result<pe.khipuai.app.data.remote.dto.SubscriptionResponse> {
        return try {
            Result.success(
                apiService.updateMySubscription(
                    pe.khipuai.app.data.remote.dto.UpdatePlanRequest(plan)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMyProfile(request: pe.khipuai.app.data.remote.dto.UserUpdateRequest): Result<UserProfileResponse> {
        return try {
            Result.success(apiService.updateMyProfile(request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMyAccount(password: String): Result<Unit> {
        return try {
            apiService.deleteMyAccount(pe.khipuai.app.data.remote.dto.UserDeleteRequest(password))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        sessionDataStore.clearToken()
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            appDatabase.clearAllTables()
        }
    }
}