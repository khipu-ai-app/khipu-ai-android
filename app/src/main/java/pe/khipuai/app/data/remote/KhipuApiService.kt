package pe.khipuai.app.data.remote

import pe.khipuai.app.data.remote.dto.AuthRequest
import pe.khipuai.app.data.remote.dto.AuthResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface KhipuApiService {

    @POST("v1/auth/google")
    suspend fun googleAuth(
        @Body request: AuthRequest
    ): AuthResponse
}